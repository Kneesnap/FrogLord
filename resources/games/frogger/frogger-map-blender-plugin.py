#################################
#  FROGLORD MAP BLENDER PLUGIN  #
#################################
# If all you're seeing is this file, you probably want to check out the following link for instructions:
# https://github.com/Kneesnap/FrogLord/blob/master/resources/documentation/games/frogger/blender-plugin.md

# Documentation Base: https://docs.blender.org/api/current/info_quickstart.html
# Important things to remember.
# Properties are used to store data persistently. (In the .blend file / between blender reboots)
# Any container (list, etc.), once updated (added/removed/etc) will invalidate all python object references held within.
# RNA Properties can be assigned to a full data type. So all instances of that data type (eg: "Object", "Scene", etc) will have that property, regardless of if assigned or not.
# ID Properties

# Blender File code reference:
# https://developer.blender.org/diffusion/BA/
# Obj:
# - https://developer.blender.org/diffusion/BA/browse/master/io_scene_obj/import_obj.py
# - https://github.com/Wehrdo/blender-obj-export/blob/master/io_scene_obj_all_normals/export_obj.py

# Mesh Data:
# https://docs.blender.org/api/current/bpy.types.Mesh.html#bpy.types.Mesh
# https://docs.blender.org/api/current/bmesh.types.html

# https://github.com/Fast-64/fast64

# If we upgrade from deprecated vertex_colors:
# - https://blender.stackexchange.com/questions/280716/python-code-to-set-color-attributes-per-vertex-in-blender-3-5

bl_info = {
    "name": "FrogLord Map Utilities",
    "category": "Object",
    "blender": (2, 80, 0),
}

import bpy
import bmesh
import os
import pathlib
import random
import re
import io
from datetime import datetime
from mathutils import Vector
import addon_utils

# ImportHelper is a helper class, defines filename and
# invoke() function which calls the file selector.
from bpy_extras.io_utils import ImportHelper, ExportHelper

LEVEL_MESH_NAME = "LevelMesh"
LEVEL_OBJECT_NAME = "LevelObject"
UNTEXTURED_MATERIAL_NAME = "NoTexture"
CURRENT_FFS_VERSION = 1

UV_LAYER_NAME = "Texture Uvs"
VERTEX_COLOR_LAYER_NAME = "Vertex Colors"
TEXTURE_FLAG_LAYER_NAME = "Texture Flags"
GRID_FLAG_LAYER_NAME = "Grid Flags"

def main(context):
    print("Hello from FrogLord Blender")
    for ob in context.scene.objects:
        print(ob)

def read_text_file(filepath):
    f = open(filepath, "r", encoding="utf-8")
    lines = f.readlines()
    f.close()
    return lines

def int_to_rgbaf_color(int_color):
    blue =  (int_color & 255) / 255.0
    green = ((int_color >> 8) & 255) / 255.0
    red =   ((int_color >> 16) & 255) / 255.0
    return red, green, blue, 1.0

def rgbaf_color_to_int(rgb_color):
    red = (round(rgb_color[0] * 255) & 255)
    green = (round(rgb_color[1] * 255) & 255)
    blue = (round(rgb_color[2] * 255) & 255)
    return (red << 16) | (green << 8) | blue

# Clear all nodes in a material
def clear_material(material):
    if material.node_tree:
        material.node_tree.links.clear()
        material.node_tree.nodes.clear()

# Necessary to allow referencing material nodes by their ID instead of their constantly invalidated object reference.
def create_material_node(nodes, node_type):
    id = len(nodes)
    nodes.new(type=node_type)
    return id

# A note on the different rendering engines.
# Vertex colors are great! It's really nice we finally have a good way of editing them.
# Unfortunately, Blender uses the vertex color of 0, 0, 0 when rendering material previews.
# Because the vertex color is multiplied against the texture color, this always results in a fully black material preview.
# More Information: https://blenderartists.org/t/help-fixing-black-incorrect-material-preview/1592069

# The solution to this really sucks, but it's to have different shaders for different rendering engines.
# By setting the scene to render with the 'CYCLES' engine, the material previews will render with 'EEVEE', while the scene renders with 'CYCLES'.
# By doing that, we can have a different engine used for the material previews than the one used to draw the scene.
# I'm not entirely sure why that works, since you'd think setting the scene to use the 'CYCLES' engine would make it... render with the cycles engine in the main viewport.
# Buuuut, it seems to work so I guess I can't really complain.

# Other Options:
# - Open Shading Language https://docs.blender.org/manual/en/latest/render/shader_nodes/osl.html (Might be able to write a custom shader?).
#  - Perhaps there's some way we can tell if we're rendering for a material preview or not. Worst case scenario we can treat pure black as pure white.

# Create shading material definition.
def create_shaded_material(material, folder, file_name):
    texture_file_path = os.path.join(folder, file_name)
    if not os.path.exists(texture_file_path):
        raise Exception("Could not find the texture file '%s', was the .ffs file moved to a folder without its textures?" % file_name)

    clear_material(material)
    material.preview_render_type = 'FLAT'
    material.use_nodes = True

    # To understand what this function does, check out the 'Shading' tab in Blender.
    nodes = material.node_tree.nodes
    links = material.node_tree.links
    
    # Get or create default nodes.
    if nodes.get('Principled BSDF') is None: # By default, this node exists.
        principled_bsdf = create_material_node(nodes, 'ShaderNodeBsdfPrincipled')
        nodes[principled_bsdf].name = 'Principled BSDF'
    if nodes.get('Material Output') is None: # By default, this node exists.
        material_output = create_material_node(nodes, 'ShaderNodeOutputMaterial')
        nodes[material_output].name = 'Material Output'

    # Create the nodes. (All together to avoid object reference invalidation.)
    gouraud_mixer = create_material_node(nodes, 'ShaderNodeMix')
    texture = create_material_node(nodes, 'ShaderNodeTexImage')
    color_doubler = create_material_node(nodes, 'ShaderNodeMix')
    value_two = create_material_node(nodes, 'ShaderNodeValue')
    vertex_color_input = create_material_node(nodes, 'ShaderNodeVertexColor')
    preview_output = create_material_node(nodes, 'ShaderNodeOutputMaterial')

    # Build the shader graph.
    nodes.get('Material Output').location_absolute = Vector((330.0, 201.0))
    nodes.get('Material Output').target = 'EEVEE' # See above.
    
    nodes[preview_output].name = 'Material Preview Output'
    nodes[preview_output].location_absolute = Vector((330.0, -45.0))
    nodes[preview_output].target = 'CYCLES' # See above

    # The purpose of this node is to allow passing a surface to the output instead of just an RGB color.
    # Or in other words, it allows us to use the texture alpha.
    # Using this node will also enable lighting in the "Rendered" view however, which will make the map look dark.
    # This can be avoided by unticking the "Scene World" checkbox under the lighting options in the render mode selector.
    # Not sure yet how we're going to allow baking lights so we might want to revisit this.
    # Using this also makes generating shaders take a bit/hang the program while we load the level. Not a huge deal.
    principled_bsdf = nodes.get('Principled BSDF')
    principled_bsdf.inputs["Metallic"].default_value = 0.0
    principled_bsdf.inputs["Roughness"].default_value = 1.0
    principled_bsdf.inputs["IOR"].default_value = 1.0
    principled_bsdf.location_absolute = Vector((30.0, 179.0))
    links.new(principled_bsdf.outputs['BSDF'], nodes.get('Material Output').inputs['Surface'])

    nodes[gouraud_mixer].blend_type = 'MULTIPLY' # https://docs.blender.org/api/current/bpy_types_enum_items/ramp_blend_items.html#rna-enum-ramp-blend-items
    nodes[gouraud_mixer].data_type = 'RGBA'
    nodes[gouraud_mixer].inputs["Factor"].default_value = 1.0
    nodes[gouraud_mixer].location_absolute = Vector((-156, 295.5))
    links.new(nodes[gouraud_mixer].outputs['Result'], principled_bsdf.inputs['Base Color'])

    nodes[texture].image = bpy.data.images.load(texture_file_path)
    nodes[texture].extension = 'EXTEND' # Extend repeating edge pixels of the image.
    nodes[texture].interpolation = 'Closest' # Disable interpolation.
    nodes[texture].location_absolute = Vector((-457.75, -79))
    links.new(nodes[texture].outputs['Color'], nodes[gouraud_mixer].inputs['B'])
    links.new(nodes[texture].outputs['Alpha'], principled_bsdf.inputs['Alpha'])
    links.new(nodes[texture].outputs['Color'], nodes[preview_output].inputs['Surface'])

    nodes[color_doubler].blend_type = 'MULTIPLY' # https://docs.blender.org/api/current/bpy_types_enum_items/ramp_blend_items.html#rna-enum-ramp-blend-items
    nodes[color_doubler].data_type = 'RGBA'
    nodes[color_doubler].inputs["Factor"].default_value = 1.0
    nodes[color_doubler].location_absolute = Vector((-360.5, 211.5))
    links.new(nodes[color_doubler].outputs['Result'], nodes[gouraud_mixer].inputs['A'])

    nodes[value_two].outputs["Value"].default_value = 4.0 # I'm not entirely sure why 2.0x doesn't work, but 4.0x does seem to work.
    nodes[value_two].location_absolute = Vector((-600.25, 30.25))
    links.new(nodes[value_two].outputs["Value"], nodes[color_doubler].inputs['B'])

    nodes[vertex_color_input].layer_name = VERTEX_COLOR_LAYER_NAME
    #nodes[vertex_color_input].outputs['Color'].default_value = (1.0, 1.0, 1.0, 1.0) # This doesn't fix material previews like I'd hoped, but I'll keep it here as a reminder how to set default values.
    nodes[vertex_color_input].location_absolute = Vector((-583.5, 159.5))
    links.new(nodes[vertex_color_input].outputs['Color'], nodes[color_doubler].inputs['A'])

# Gets the frogger texture ID for the given material.
FROGGER_NO_TEXTURE_ID = -2
FROGGER_UNKNOWN_TEXTURE_ID = -1
def get_frogger_texture_id(material):
    if material is None:
        return FROGGER_NO_TEXTURE_ID

    material_texture_id = material.frogger_data.texture_id
    if material_texture_id >= 0 or material_texture_id == FROGGER_NO_TEXTURE_ID:
        return material_texture_id

    name_match = re.fullmatch(r'tex([0-9]+)(\.([0-9]+))?', material.name, flags=re.IGNORECASE)
    if name_match is not None:
        return int(name_match.group(1))

    return FROGGER_UNKNOWN_TEXTURE_ID

def swap_blender_order(array):
    # Swaps between <Frogger Polygon Data Order> <-> <Blender Data Order>
    if len(array) == 4:
        temp = array[3]
        array[3] = array[2]
        array[2] = temp

    return array

def to_blender_order(array):
    return swap_blender_order(array)[::-1]

def to_ffs_order(array):
    return swap_blender_order(array[::-1])

def convert_grid_flags(blender_grid_flags):
    # The default blender layer value is zero, and we need to therefore treat zero as "I'm not a grid square.", so that new polygons are seen as not part of the grid.
    # However, the flag value of zero IS a valid grid square flag combination. So, we'll treat a REAL value of 0 as -1, so we can distinguish the "I'm not a grid square" from "I'm a grid square but my flags are zero."
    # This operation is reversible.
    if blender_grid_flags == 0:
        return -1
    elif blender_grid_flags == -1:
        return 0
    else:
        return blender_grid_flags

def select_object(obj):
    if not obj in bpy.context.collection.objects.values():
        bpy.context.collection.objects.link(obj) # Add object to scene.
    if obj in bpy.context.view_layer.objects.values():
        bpy.context.view_layer.objects.active = obj # Set the active object.
    obj.select_set(True) # Select the object.

def set_object_mode(operator, object, mode, log_if_fail):
    if bpy.context.active_object is None and object is not None:
        select_object(object) # To avoid the failure that happens if no objects are selected, select the provided object.

    if bpy.context.active_object is not None:
        old_mode = bpy.context.active_object.mode
        bpy.ops.object.mode_set(mode = mode if mode is not None else 'OBJECT')
        return old_mode
    elif log_if_fail:
        operator.report({"WARNING"}, "You did not have any object actively selected. If an error occurs, or the mesh import/export does not have the expected coloring, please select an object (such as the default cube) in the scene and try again.")
        return None

def load_ffs_file(operator, context, filepath):
    is_cycles_enabled, is_cycles_loaded = addon_utils.check('cycles')
    if not is_cycles_loaded:
        operator.report({"INFO"}, "Attempting to automatically enable 'CYCLES' rendering engine.")
        success = addon_utils.enable('cycles', default_set=True)
        if success:
            operator.report({"INFO"}, "Enabled the 'CYCLES' rendering engine.")
        else:
            operator.report({"ERROR"}, "Failed to enable the 'CYCLES' rendering engine.")
            return {'CANCELLED'}

    # Delete old data. This prevented crashes previously, but I've maybe fixed the crash bug.
    # It will remain here until we're confident the crash bug is fixed.
    #if LEVEL_MESH_NAME in bpy.data.meshes:
    #    bpy.data.meshes.remove(bpy.data.meshes[LEVEL_MESH_NAME], do_unlink=True)
    #if LEVEL_OBJECT_NAME in bpy.data.objects:
    #    bpy.data.objects.remove(bpy.data.objects[LEVEL_OBJECT_NAME], do_unlink=True)
    mesh_already_existed = LEVEL_MESH_NAME in bpy.data.meshes
    object_already_existed = LEVEL_OBJECT_NAME in bpy.data.objects
    mesh = bpy.data.meshes[LEVEL_MESH_NAME] if mesh_already_existed else bpy.data.meshes.new(name=LEVEL_MESH_NAME) # mesh is also accessible from obj.data
    obj = bpy.data.objects[LEVEL_OBJECT_NAME] if object_already_existed else bpy.data.objects.new(LEVEL_OBJECT_NAME, mesh)
    if mesh != obj.data:
        operator.report({"ERROR"}, "The object named '%s' MUST be attached to the mesh named '%s'!" % (LEVEL_OBJECT_NAME, LEVEL_MESH_NAME))
        return {'CANCELLED'}

    # Exit edit mode.
    # Edit mode prevents editing vertex color data.
    # Reference: https://blender.stackexchange.com/questions/122202/changing-vertex-colors-through-python
    old_mode = set_object_mode(operator, obj, 'OBJECT', True)

    bpy.context.scene.render.engine = 'CYCLES' # See above for an explanation of why the scene must use the CYCLES engine.

    # Read FFS file into list of commands.
    folder, file_name = os.path.split(filepath)
    commands = []
    file_lines = read_text_file(filepath)
    line_number = 0
    for file_line in file_lines:
        line_number += 1
        line = file_line.replace("\r", "").replace("\n", "").strip()
        if line == "" or line[0] == '#':
            continue

        split = re.split(r'\s+', line)
        command = split[0].lower()
        commands.append((command, split[1:], file_line, line_number))

    mesh.clear_geometry()
    mesh.materials.clear()

    # This should be done before creating materials, to ensure the vertex color layer can be used.
    if not mesh.uv_layers or not UV_LAYER_NAME in mesh.uv_layers:
        mesh.uv_layers.new(name=UV_LAYER_NAME)
    if not mesh.vertex_colors or not VERTEX_COLOR_LAYER_NAME in mesh.vertex_colors:
        mesh.vertex_colors.new(name=VERTEX_COLOR_LAYER_NAME)

    # Create a material for faces without textures.
    untextured_material = bpy.data.materials[UNTEXTURED_MATERIAL_NAME] if UNTEXTURED_MATERIAL_NAME in bpy.data.materials else bpy.data.materials.new(name=UNTEXTURED_MATERIAL_NAME)
    untextured_material.frogger_data.texture_id = FROGGER_NO_TEXTURE_ID # Mark as different from the default 'no texture ID' value.
    untextured_material_id = len(mesh.materials)
    untextured_material.use_nodes = True
    clear_material(untextured_material)

    untextured_material_nodes = untextured_material.node_tree.nodes
    untextured_material_links = untextured_material.node_tree.links

    # Create the nodes. (All together to avoid object reference invalidation.)
    vertex_color_input = create_material_node(untextured_material_nodes, 'ShaderNodeVertexColor')
    untextured_material_nodes[vertex_color_input].layer_name = VERTEX_COLOR_LAYER_NAME
    untextured_material_nodes[vertex_color_input].location_absolute = Vector((0.0, 0.0))

    if untextured_material_nodes.get('Material Output') is None: # By default, this node exists.
        material_output = create_material_node(untextured_material_nodes, 'ShaderNodeOutputMaterial')
        untextured_material_nodes[material_output].name = 'Material Output'
    untextured_material_nodes.get('Material Output').location_absolute = Vector((175.0, 20.0))

    # Setup.
    untextured_material_links.new(untextured_material_nodes[vertex_color_input].outputs['Color'], untextured_material_nodes.get('Material Output').inputs['Surface'])
    untextured_material.preview_render_type = 'FLAT'
    mesh.materials.append(untextured_material)

    # Process FFS File Commands.
    ffs_version = 0
    pending_vertex_data = []
    pending_face_data = []
    extra_face_data = []
    material_id_remaps = {}
    downsized_face_count = 0
    for command, args, line_text, line_number in commands:
        arg_index = 0

        if command == "version_ffs": # version_ffs version
            ffs_version = int(args[arg_index])
            if ffs_version > CURRENT_FFS_VERSION:
                operator.report({"WARNING"}, "FFS File uses a newer version (%d) than what is supported by the Blender plugin (%d). Issues may result from this!" % (ffs_version, CURRENT_FFS_VERSION))
        elif command == "version_game": # version_game version
            bpy.context.scene.frogger_data.game_version = args[arg_index]
        elif command == "vertex": # vertex x y z (Swap z and y in Blender, then invert Z.)
            pending_vertex_data.append([float(args[arg_index + 0]), float(args[arg_index + 2]), -float(args[arg_index + 1])])
        elif command == "texture": # texture textureId
            texture_id = int(args[arg_index])
            material_name = "Tex%d" % texture_id
            material_id_remaps[texture_id] = len(obj.data.materials)
            material = bpy.data.materials[material_name] if material_name in bpy.data.materials else bpy.data.materials.new(name=material_name)
            material.frogger_data.texture_id = texture_id
            create_shaded_material(material, folder, "%d.png" % texture_id)
            obj.data.materials.append(material)
        elif command == "polygon": # polygon polygon_args...
            polygon_type = args[arg_index].lower()
            if polygon_type == "f3": # f3 show v1 v2 v3 color gridFlags
                num_colors = 1
                has_texture = False
                num_vertices = 3
            elif polygon_type == "f4": # f4 show v1 v2 v3 v4 color gridFlags
                num_colors = 1
                has_texture = False
                num_vertices = 4
            elif polygon_type == "ft3": # ft3 show texture flags v1 v2 v3 uv1 uv2 uv3 color gridFlags
                num_colors = 1
                has_texture = True
                num_vertices = 3
            elif polygon_type == "ft4": # ft4 show texture flags v1 v2 v3 v4 uv1 uv2 uv3 uv4 color gridFlags
                num_colors = 1
                has_texture = True
                num_vertices = 4
            elif polygon_type == "g3": # g3 show v1 v2 v3 color1 color2 color3 gridFlags
                num_colors = 3
                has_texture = False
                num_vertices = 3
            elif polygon_type == "g4": # g4 show v1 v2 v3 v4 color1 color2 color3 color4 gridFlags
                num_colors = 4
                has_texture = False
                num_vertices = 4
            elif polygon_type == "gt3": # gt3 show texture flags v1 v2 v3 uv1 uv2 uv3 color1 color2 color3 gridFlags
                num_colors = 3
                has_texture = True
                num_vertices = 3
            elif polygon_type == "gt4": # gt4 show texture flags v1 v2 v3 v4 uv1 uv2 uv3 uv4 color1 color2 color3 color4 gridFlags
                num_colors = 4
                has_texture = True
                num_vertices = 4
            else:
                operator.report({"WARNING"}, "Unknown polygon type '%s' on line %d." % (polygon_type, line_number))
                continue

            # Read polygon data.
            should_hide = (args[arg_index + 1] == "hide")
            material_index = untextured_material_id
            texture_flags = 0

            # Read textured polygon data, maybe.
            arg_index += 2
            if has_texture:
                material_index = material_id_remaps[int(args[arg_index])]
                texture_flags = int(args[arg_index + 1])
                arg_index += 2

            # Read vertices.
            vertices = []
            for i in range(num_vertices):
                vertices.append(int(args[arg_index]))
                arg_index += 1
            vertices = to_blender_order(vertices)

            # Read texCoords.
            tex_coords = []
            if has_texture:
                for i in range(num_vertices):
                    uv_text = args[arg_index].split(":")
                    tex_coords.append([float(uv_text[0]), 1.0 - float(uv_text[1])])
                    arg_index += 1
            tex_coords = to_blender_order(tex_coords)

            # Read colors.
            colors = []
            for i in range(num_colors):
                colors.append(int_to_rgbaf_color(int(args[arg_index], 16)))
                arg_index += 1
            colors = to_blender_order(colors)

            # Read optional grid flags.
            grid_flags = -1
            if len(args) > arg_index:
                grid_flags = int(args[arg_index])
                arg_index += 1

            # After preparing the polygon data, we have a problem.
            # Some Frogger maps such as DES1.MAP contain quads that use the same vertex more than once, to effectively render as a triangle, while still being a quad.
            # The purpose of this is unknown, and it appears to have been likely an oversight/remnant of how the maps were modelled.
            # Blender does not support faces using the same vertex more than once, so we must remove those vertices to actually become the assumed type.
            seen_vertices = []
            duplicate_vertices = []
            for i in range(len(vertices)):
                vertex = vertices[i]
                if vertex in seen_vertices:
                    duplicate_vertices.append(i)
                else:
                    seen_vertices.append(vertex)

            # Remove the duplicate vertices.
            for i in duplicate_vertices:
                vertices.pop(i)
                if has_texture:
                    tex_coords.pop(i)
                if len(colors) > 1:
                    colors.pop(i)
            if len(duplicate_vertices) > 0:
                downsized_face_count += 1

            # Store the polygon data for later access.
            pending_face_data.append(vertices)
            extra_face_data.append((should_hide, material_index, texture_flags, tex_coords, colors, grid_flags))
        else:
            operator.report({"WARNING"}, "Unknown Command '%s', skipping line %d!" % (command, line_number))

    if downsized_face_count > 0:
        operator.report({"WARNING"}, "Converted down %d face(s) to remove duplicate vertex usages." % downsized_face_count)

    # Apply the mesh data to the mesh.
    mesh.from_pydata(pending_vertex_data, [], pending_face_data)

    # Apply extra polygon data.
    for i in range(len(extra_face_data)):
        polygon = mesh.polygons[i]
        should_hide, material_index, _, tex_coords, colors, _ = extra_face_data[i]

        polygon.hide = should_hide
        polygon.material_index = material_index
        if len(tex_coords) > 0:
            for uv_index in range(polygon.loop_total):
                mesh.uv_layers[UV_LAYER_NAME].data[polygon.loop_start + uv_index].uv = tex_coords[uv_index]

        # Apply vertex colors.
        for color_index in range(polygon.loop_total):
            mesh.vertex_colors[VERTEX_COLOR_LAYER_NAME].data[polygon.loop_start + color_index].color = colors[color_index] if len(colors) > 1 else colors[0]

    # Apply data which blender doesn't support natively.
    bm = bmesh.new()
    bm.from_mesh(mesh)

    # https://docs.blender.org/api/current/bmesh.types.html#bmesh.types.BMLayerAccessFace
    texture_flag_layer = bm.faces.layers.int.get(TEXTURE_FLAG_LAYER_NAME) or bm.faces.layers.int.new(TEXTURE_FLAG_LAYER_NAME)
    grid_flag_layer = bm.faces.layers.int.get(GRID_FLAG_LAYER_NAME) or bm.faces.layers.int.new(GRID_FLAG_LAYER_NAME)
    bm.faces.ensure_lookup_table()
    for i in range(len(extra_face_data)):
        _, _, texture_flags, _, _, grid_flags = extra_face_data[i]

        bm.faces[i][texture_flag_layer] = texture_flags
        bm.faces[i][grid_flag_layer] = convert_grid_flags(grid_flags)

    bm.to_mesh(mesh)
    bm.free()

    # Update mesh.
    if mesh.validate(verbose=True, clean_customdata=True):
        operator.report({"INFO"}, "Invalid geometry was automatically corrected/removed.")
    mesh.update(calc_edges=True)

    # Finish Setup:
    select_object(obj)

    # [AE] Set initial rendering modes, etc.
    for area in context.screen.areas:
        if area.type == 'VIEW_3D':
            for space in area.spaces:
                if space.type == 'VIEW_3D':
                    space.shading.type = 'MATERIAL'

    # Delete unused data.
    if bpy.ops.outliner.orphans_purge is not None:
        bpy.ops.outliner.orphans_purge()

    # Restore the previous mode before we replaced it.
    set_object_mode(operator, obj, old_mode, False)
    operator.report({"INFO"}, "Successfully imported the map!")
    return {'FINISHED'}

def save_ffs_file(operator, context, filepath):
    if not LEVEL_MESH_NAME in bpy.data.meshes:
        operator.report({"ERROR"}, "Could not find a mesh named '%s'!" % LEVEL_MESH_NAME)
        return {'CANCELLED'}
    if not LEVEL_OBJECT_NAME in bpy.data.objects:
        operator.report({"ERROR"}, "Could not find an object named '%s'!" % LEVEL_OBJECT_NAME)
        return {'CANCELLED'}
    if not bpy.context.scene.frogger_data or not bpy.context.scene.frogger_data.game_version:
        operator.report({"ERROR"}, "Could not determine the target version of Frogger from the scene data." % LEVEL_OBJECT_NAME)
        return {'CANCELLED'}

    mesh = bpy.data.meshes[LEVEL_MESH_NAME]
    obj = bpy.data.objects[LEVEL_OBJECT_NAME]
    if mesh != obj.data:
        operator.report({"ERROR"}, "The object named '%s' MUST be attached to the mesh named '%s'!" % (LEVEL_OBJECT_NAME, LEVEL_MESH_NAME))
        return {'CANCELLED'}

    # Exit edit mode.
    # Edit mode prevents accessing vertex color data.
    # Reference: https://blender.stackexchange.com/questions/122202/changing-vertex-colors-through-python
    old_mode = set_object_mode(operator, obj, 'OBJECT', True)

    uv_layer = mesh.uv_layers.get(UV_LAYER_NAME)
    color_layer = mesh.vertex_colors.get(VERTEX_COLOR_LAYER_NAME)
    if uv_layer is None:
        operator.report({"ERROR"}, "Could not find mesh data layer named '%s'." % UV_LAYER_NAME)
        set_object_mode(operator, obj, old_mode, False)
        return {'CANCELLED'}
    if color_layer is None:
        operator.report({"ERROR"}, "Could not find mesh data layer named '%s'." % VERTEX_COLOR_LAYER_NAME)
        set_object_mode(operator, obj, old_mode, False)
        return {'CANCELLED'}

    bm = bmesh.new()
    bm.from_mesh(mesh)
    bm.faces.ensure_lookup_table()

    texture_flag_layer = bm.faces.layers.int.get(TEXTURE_FLAG_LAYER_NAME)
    grid_flag_layer = bm.faces.layers.int.get(GRID_FLAG_LAYER_NAME)
    if texture_flag_layer is None:
        operator.report({"ERROR"}, "Could not find bmesh data layer named '%s'." % TEXTURE_FLAG_LAYER_NAME)
        set_object_mode(operator, obj, old_mode, False)
        bm.free()
        return {'CANCELLED'}
    if grid_flag_layer is None:
        operator.report({"ERROR"}, "Could not find bmesh data layer named '%s'." % GRID_FLAG_LAYER_NAME)
        set_object_mode(operator, obj, old_mode, False)
        bm.free()
        return {'CANCELLED'}

    writer = open(filepath, "w", encoding="utf-8")

    # Write header
    writer.write("# FFS File Export -- By Blender\n")
    writer.write("# File: '%s'\n" % (bpy.path.basename(bpy.context.blend_data.filepath) or 'Untitled'))
    writer.write("# Export Time: %s\n" % (str(datetime.now())))
    writer.write("version_ffs %d\n" % (CURRENT_FFS_VERSION))
    writer.write("version_game %s\n" % (bpy.context.scene.frogger_data.game_version))
    writer.write('\n')

    # Write textures.
    seen_materials = set()
    for material in mesh.materials:
        material_id = get_frogger_texture_id(material)
        if material_id >= 0 and not material_id in seen_materials:
            writer.write("texture %d\n" % (material_id))
            seen_materials.add(material_id)
    if len(seen_materials) > 0:
        writer.write('\n')

    # Write Vertices:
    for vert in bm.verts:
        writer.write("vertex %f %f %f\n" % (vert.co.x, -vert.co.z, vert.co.y))
    writer.write('\n')

    # Setup Data:
    grid_stack_data = {}
    animation_faces = {}

    # Write Faces:
    for polygon_index, polygon in enumerate(mesh.polygons):
        if polygon.loop_total != 4 and polygon.loop_total != 3:
            operator.report({"WARNING"}, "Skipping face %d because it had %d vertices. (Only 3 or 4 vertices are supported)" % (polygon_index, polygon.loop_total))
            continue

        # Resolve material data.
        material = mesh.materials[polygon.material_index] if polygon.material_index >= 0 and len(mesh.materials) > polygon.material_index else None
        material_texture_id = get_frogger_texture_id(material)
        is_textured = material_texture_id >= 0

        # Prepare polygon data for writing.
        vertices = []
        for vertex in polygon.vertices:
            vertices.append(vertex)
        vertices = to_ffs_order(vertices)

        colors = []
        for i in range(polygon.loop_total):
            colors.append(rgbaf_color_to_int(color_layer.data[polygon.loop_start + i].color))
        colors = to_ffs_order(colors)

        uvs = []
        if is_textured:
            for i in range(polygon.loop_total):
                uv = uv_layer.data[polygon.loop_start + i].uv
                if uv[0] < -0.001 or uv[0] > 1.001 or uv[1] < -0.001 or uv[1] > 1.001:
                    operator.report({"WARNING"}, "Face %d has uvs[%d] out of the range Frogger supports! (%f, %f)" % (polygon_index, i, uv[0], uv[1]))

                uvs.append((max(0.0, min(1.0, uv[0])), max(0.0, min(1.0, 1.0 - uv[1]))))
            uvs = to_ffs_order(uvs)
        elif material_texture_id != FROGGER_NO_TEXTURE_ID and material is not None:
            operator.report({"WARNING"}, "Face %d used material %d/'%s', which was not a recognized Frogger texture! It will be exported as an untextured polygon!" % (polygon_index, polygon.material_index, material.name))

        # Determine polygon type.
        # <polygon_type> <show|hide> [texture] [flags] <vertexIds[]...> <textureUvs[]...> <colors[]...> [gridFlags]
        is_quad = polygon.loop_total == 4

        is_gouraud = False
        for i in range(1, len(colors)):
            if colors[i] != colors[0]:
                is_gouraud = True
                break

        # Write polygon command.
        writer.write("polygon ")
        writer.write('g' if is_gouraud else 'f')
        if is_textured:
            writer.write('t')
        writer.write(str(len(polygon.vertices)))

        writer.write(" hide" if polygon.hide else " show")

        # Write optional texture data.
        if is_textured:
            texture_flags = bm.faces[polygon_index][texture_flag_layer]
            writer.write(" %d %d" % (material_texture_id, texture_flags or 0))

        # Write Vertices:
        for vertex_id in vertices:
            writer.write(" %d" % vertex_id)

        # Write TexCoords
        for uv in uvs:
            writer.write(" %f:%f" % uv)

        # Write Colors:
        for i in range(len(colors) if is_gouraud else 1):
            writer.write(" %06X" % colors[i])

        # Write grid flags.
        grid_flags = convert_grid_flags(bm.faces[polygon_index][grid_flag_layer])
        if grid_flags != -1:
            writer.write(" %d" % grid_flags)

        # End of polygon command.
        writer.write('\n')
    writer.write('\n')

    writer.close()
    bm.free()

    set_object_mode(operator, obj, old_mode, False) # Restore previous mode.
    operator.report({"INFO"}, "Successfully exported the map!")
    return {'FINISHED'}

# Reference Blender > Text Editor > Templates > Python > Operator File Import.
class LoadFfsOperator(bpy.types.Operator, ImportHelper):
    """This appears in the tooltip of the operator and in the generated docs"""
    """Loads a .ffs file into the scene"""
    bl_idname = "frog.load_ffs"
    bl_label = "Frogger Map (.ffs)"
    
    # ImportHelper mix-in class uses this.
    filename_ext = ".ffs"
    
    filter_glob: bpy.props.StringProperty(
        default="*.ffs",
        options={'HIDDEN'},
        maxlen=255,  # Max internal buffer length, longer would be clamped.
    )

    def execute(self, context):
        return load_ffs_file(self, context, self.filepath)

class SaveFfsOperator(bpy.types.Operator, ExportHelper):
    """This appears in the tooltip of the operator and in the generated docs"""
    """Saves a .ffs file from the scene"""
    bl_idname = "frog.save_ffs"
    bl_label = "Frogger Map (.ffs)"

    # ExportHelper mix-in class uses this.
    filename_ext = ".ffs"

    filter_glob: bpy.props.StringProperty(
        default="*.ffs",
        options={'HIDDEN'},
        maxlen=255,  # Max internal buffer length, longer would be clamped.
    )

    def execute(self, context):
        return save_ffs_file(self, context, self.filepath)
 

def menu_func(self, context):
    self.layout.separator()
    self.layout.operator(LoadFfsOperator.bl_idname, text=LoadFfsOperator.bl_label)
    self.layout.operator(SaveFfsOperator.bl_idname, text=SaveFfsOperator.bl_label)

# Only needed if you want to add into a dynamic menu.
def menu_func_import(self, context):
    self.layout.operator(LoadFfsOperator.bl_idname, text=LoadFfsOperator.bl_label)

def menu_func_export(self, context):
    self.layout.operator(SaveFfsOperator.bl_idname, text=SaveFfsOperator.bl_label)

class FroggerSceneData(bpy.types.PropertyGroup):
    game_version: bpy.props.StringProperty(name="Frogger Version")

class FroggerMaterialData(bpy.types.PropertyGroup):
    texture_id: bpy.props.IntProperty(name="Texture ID", default=FROGGER_UNKNOWN_TEXTURE_ID)

def register():
    bpy.utils.register_class(FroggerSceneData)
    bpy.types.Scene.frogger_data = bpy.props.PointerProperty(type=FroggerSceneData)

    bpy.utils.register_class(FroggerMaterialData)
    bpy.types.Material.frogger_data = bpy.props.PointerProperty(type=FroggerMaterialData)

    bpy.utils.register_class(SaveFfsOperator)
    bpy.utils.register_class(LoadFfsOperator)
    bpy.types.VIEW3D_MT_object.append(menu_func)
    bpy.types.TOPBAR_MT_file_import.append(menu_func_import)
    bpy.types.TOPBAR_MT_file_export.append(menu_func_export)

# This is only called when treated as an addon/extension, NOT when run directly inside Blender.
# Info: https://blender.stackexchange.com/a/332133/86891
def unregister():
    bpy.utils.unregister_class(FroggerSceneData)
    bpy.utils.unregister_class(FroggerMaterialData)
    bpy.utils.unregister_class(SaveFfsOperator)
    bpy.utils.unregister_class(LoadFfsOperator)
    bpy.types.VIEW3D_MT_object.remove(menu_func)
    bpy.types.TOPBAR_MT_file_import.remove(menu_func_import)
    bpy.types.TOPBAR_MT_file_export.remove(menu_func_export)

#print("HELLO!") # These are only displayed when running Blender from the command-line.
if __name__ == "__main__":
    register()