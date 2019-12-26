#################################
#  FROGLORD MAP BLENDER PLUGIN  #
#################################

# Blender File code:
# https://developer.blender.org/diffusion/BA/
# Obj:
# - https://developer.blender.org/diffusion/BA/browse/master/io_scene_obj/import_obj.py
# - https://developer.blender.org/diffusion/BA/browse/master/io_scene_obj/export_obj.py

# Mesh Data:
# https://docs.blender.org/api/current/bpy.types.Mesh.html#bpy.types.Mesh
# https://docs.blender.org/api/current/bmesh.types.html

# https://bitbucket.org/kurethedead/fast64/src/master/__init__.py

bl_info = {
    "name": "FrogLord Map",
    "category": "Object",
    "blender": (2, 80, 0),
}

import bpy
import bmesh
import os
import pathlib
import random

def main(context):
    print("Hello from FrogLord Blender")
    for ob in context.scene.objects:
        print(ob)

def read_file(file_path):
    f = open(file_path, "r")
    lines = f.readlines()
    f.close()
    return lines

def uv_str(uv):
    return str(uv[0]) + ":" + str(uv[1])

def int_to_rgb_color(int_color):
    blue =  (int_color & 255) / 255.0
    green = ((int_color >> 8) & 255) / 255.0
    red =   ((int_color >> 16) & 255) / 255.0
    return red, green, blue, 1.0

def rgb_color_to_int(rgb_color):
    blue = (round(rgb_color[0] * 255) & 255) << 16
    green = (round(rgb_color[1] * 255) & 255) << 8
    red = (round(rgb_color[2] * 255) & 255)
    return red | green | blue

class LoadFfsOperator(bpy.types.Operator):
    """Loads a .ffs file into the scene"""
    bl_idname = "frog.load_ffs"
    bl_label = "Load .FFS File"

    filepath = bpy.props.StringProperty(subtype="FILE_PATH")
    filter_glob = bpy.props.StringProperty(default="*.ffs", options={'HIDDEN'})

    def invoke(self, context, event):
        context.window_manager.fileselect_add(self)
        return {'RUNNING_MODAL'}

    def execute(self, context):
        file = self.filepath
        print(file)
        folder, file_name = os.path.split(file)
        lines = read_file(file)

        # Delete old data.
        if "LevelMesh" in bpy.data.meshes:
            bpy.data.meshes.remove(bpy.data.meshes["LevelMesh"], do_unlink=True)
        if "LevelObject" in bpy.data.objects:
            bpy.data.objects["LevelObject"].materials.clear()
            bpy.data.objects.remove(bpy.data.objects["LevelObject"], do_unlink=True)

        # Load New Data.
        mesh = bpy.data.meshes.new(name="LevelMesh")
        mesh["SaveFile"] = file
        obj = bpy.data.objects.new("LevelObject", mesh)

        verts = []
        faces = []

        for line in lines:
            line = line.replace("\r", "").replace("\n", "")
            if line == "":
                continue

            split = line.split(" ")
            action = split[0].lower()

            if action == "v":
                verts.append([float(split[1]), float(split[2]), float(split[3])])
            elif action == "f3" or action == "ft3" or action == "g3" or action == "gt3":
                faces.append([int(split[2]), int(split[3]), int(split[4])])
            elif action == "f4" or action == "ft4" or action == "g4" or action == "gt4":
                faces.append([int(split[2]), int(split[3]), int(split[4]), int(split[5])])

        mesh.from_pydata(verts, [], faces)
        mesh = bpy.data.meshes["LevelMesh"]

        # Delete Materials:
        for material in bpy.data.materials:
            material.user_clear()
            bpy.data.materials.remove(material)

        for image in bpy.data.images:
            image.user_clear()
            bpy.data.images.remove(image)

        # Load materials.
        tex_dict = {}

        # Create a material for faces without textures.
        no_material = bpy.data.materials.new(name="NoTexture")
        no_material.diffuse_color = (1.0, 1.0, 1.0, 1.0)
        obj.data.materials.append(no_material)

        mat_count = 1
        for test_file in os.listdir(folder):
            if not test_file.endswith(".png"):
                continue

            id = pathlib.Path(test_file).stem
            material_name = "Tex" + id
            new_material = bpy.data.materials.new(name=material_name)
            new_material.diffuse_color = (1.0, 1.0, 1.0, 1.0)
            new_material.use_nodes = True
            new_material.specular_intensity = 0.0 # This removes the gleam from the materials.

            bsdf = new_material.node_tree.nodes["Principled BSDF"]
            texture = new_material.node_tree.nodes.new("ShaderNodeTexImage")
            texture.image = bpy.data.images.load(folder + os.path.sep + test_file)
            new_material.node_tree.links.new(bsdf.inputs['Base Color'], texture.outputs['Color'])
            obj.data.materials.append(new_material)
            tex_dict[id] = mat_count
            mat_count += 1

        # Load all of the remaining data.
        gridX = 0
        gridZ = 0
        index = 0
        mesh = bpy.data.meshes["LevelMesh"]
        uv_layer = mesh.uv_layers.new() if not mesh.uv_layers else mesh.uv_layers.active
        color_layer = mesh.vertex_colors.new()  if not mesh.vertex_colors else mesh.vertex_colors.active
        for line in lines:
            line = line.replace("\r", "").replace("\n", "")
            if line == "":
                continue

            split = line.split(" ")
            action = split[0].lower()

            if action == "grid-size": # grid-size xSize zSize
                mesh["gridX"] = gridX = int(split[1])
                mesh["gridZ"] = gridZ = int(split[2])
            elif action == "f3": # f3 show v1 v2 v3 color
                poly = mesh.polygons[index]
                poly.hide = (split[1] == "hide")
                color = int_to_rgb_color(int(split[5]))
                for i in range(0, poly.loop_total):
                    color_layer.data[poly.loop_start + i].color = color

                index += 1
            elif action == "f4": # f4 show v1 v2 v3 v4 color
                poly = mesh.polygons[index]
                poly.hide = (split[1] == "hide")
                color = int_to_rgb_color(int(split[6]))
                for i in range(0, poly.loop_total):
                    color_layer.data[poly.loop_start + i].color = color
                index += 1
            elif action == "ft3": # ft3 show v1 v2 v3 flags texture color uv1 uv2 uv3
                poly = mesh.polygons[index]
                poly.material_index = tex_dict[split[6]] # Texture
                poly.hide = (split[1] == "hide")

                # Color:
                color = int_to_rgb_color(int(split[7]))
                for i in range(0, poly.loop_total):
                    color_layer.data[poly.loop_start + i].color = color

                # UVs:
                for i, loop_idx in enumerate(poly.loop_indices):
                    uv_text = split[8 + i].split(":")
                    uv_layer.data[loop_idx].uv = [float(uv_text[0]), float(uv_text[1])]

                index += 1
            elif action == "ft4": # ft4 show v1 v2 v3 v4 flags texture color uv1 uv2 uv3 uv4
                poly = mesh.polygons[index]
                poly.material_index = tex_dict[split[7]] # Texture
                poly.hide = (split[1] == "hide")

                # Color:
                color = int_to_rgb_color(int(split[8]))
                for i in range(0, poly.loop_total):
                    color_layer.data[poly.loop_start + i].color = color

                # UVs:
                for i, loop_idx in enumerate(poly.loop_indices):
                    uv_text = split[9 + i].split(":")
                    uv_layer.data[loop_idx].uv = [float(uv_text[0]), float(uv_text[1])]

                index += 1
            elif action == "g3": # g3 show v1 v2 v3 color1 color2 color3
                poly = mesh.polygons[index]
                poly.hide = (split[1] == "hide")
                for i in range(0, poly.loop_total):
                    color_layer.data[poly.loop_start + i].color = int_to_rgb_color(int(split[5 + i]))
                index += 1
            elif action == "g4": # g4 show v1 v2 v3 v4 color1 color2 color3 color4
                poly = mesh.polygons[index]
                poly.hide = (split[1] == "hide")
                for i in range(0, poly.loop_total):
                    color_layer.data[poly.loop_start + i].color = int_to_rgb_color(int(split[6 + i]))
                index += 1
            elif action == "gt3": # gt3 show v1 v2 v3 flags texture color1 color2 color3 uv1 uv2 uv3
                poly = mesh.polygons[index]
                poly.hide = (split[1] == "hide")
                poly.material_index = tex_dict[split[6]] # Texture

                # Colors:
                for i in range(0, poly.loop_total):
                    color_layer.data[poly.loop_start + i].color = int_to_rgb_color(int(split[7+ i]))

                # UVs:
                for i, loop_idx in enumerate(poly.loop_indices):
                    uv_text = split[10 + i].split(":")
                    uv_layer.data[loop_idx].uv = [float(uv_text[0]), float(uv_text[1])]

                index += 1
            elif action == "gt4": # gt4 show v1 v2 v3 v4 flags texture color1 color2 color3 color4 uv1 uv2 uv3 uv4
                poly = mesh.polygons[index]
                poly.hide = (split[1] == "hide")
                poly.material_index = tex_dict[split[7]] # Texture

                # Colors:
                for i in range(0, poly.loop_total):
                    color_layer.data[poly.loop_start + i].color = int_to_rgb_color(int(split[8 + i]))

                # UVs:
                for i, loop_idx in enumerate(poly.loop_indices):
                    uv_text = split[12 + i].split(":")
                    uv_layer.data[loop_idx].uv = [float(uv_text[0]), float(uv_text[1])]

                index += 1
            elif action != "v" and action != "anim" and action != "grid":
                print("Unknown Command, Skipping! '" + line + "'.")

        mesh = bpy.data.meshes["LevelMesh"]
        mesh.update(calc_edges=True)


        # Apply data to things which blender doesn't support custom id properties.
        bm = bmesh.new()
        bm.from_mesh(mesh)

        stack_index = 0
        poly_index = 0
        anim_index = 0
        texflag_layer = bm.faces.layers.int.new()
        stack_layer = bm.faces.layers.int.new()
        grid_flag_layer = bm.faces.layers.int.new()
        anim_layer = bm.faces.layers.int.new()
        stack_heights = []
        animations = []
        for line in lines:
            line = line.replace("\r", "").replace("\n", "")
            if line == "":
                continue

            split = line.split(" ")
            action = split[0].lower()

            if action == "anim": # anim animType uChange vChange uvDuration texDuration (textures..) (faces..)
                animation = [split[1], split[2], split[3], split[4], split[5], split[6]]
                for face_str in split[7].split(","):
                    bm.faces.ensure_lookup_table()
                    bm.faces[int(face_str)][anim_layer] = anim_index + 1

                animations.append(animation)
                anim_index += 1
            elif action == "grid": # grid height face:flag..
                stack_heights.append(int(split[1])) # Keep track of stack heights.

                for i in range(2, len(split)):
                    arg = split[i].split(":")
                    face = int(arg[0])
                    flag = int(arg[1])
                    bm.faces.ensure_lookup_table()
                    bm.faces[face][stack_layer] = stack_index + 1
                    bm.faces[face][grid_flag_layer] = flag

                stack_index += 1
            elif action == "ft3" or action == "ft4" or action == "gt3" or action == "gt4":
                flags = int(split[6 if action.endswith("4") else 5])
                bm.faces.ensure_lookup_table()
                bm.faces[poly_index][texflag_layer] = flags
                index += 1
            elif poly_index == "f3" or action == "f4" or action == "g3" or action == "g4":
                poly_index += 1


        bm.to_mesh(mesh)
        bm.free()

        # Finish Setup:
        mesh = bpy.data.meshes["LevelMesh"]
        mesh["GridHeights"] = stack_heights
        mesh["Animations"] = animations
        # print("Mesh Valid: " + str(mesh.validate(verbose = True)))

        obj = bpy.data.objects["LevelObject"]
        bpy.context.collection.objects.link(obj) # Add object to scene.
        bpy.context.view_layer.objects.active = obj # Set the active object.
        obj.select_set(True) # Select the object.

        return {'FINISHED'}

class SaveFfsOperator(bpy.types.Operator):
    """Saves a .ffs file from the scene"""
    bl_idname = "frog.save_ffs"
    bl_label = "Save .FFS File"

    def execute(self, context):
        mesh = bpy.data.meshes["LevelMesh"]
        obj = bpy.data.objects["LevelObject"]

        file = mesh["SaveFile"]

        bm = bmesh.new()
        bm.from_mesh(mesh)
        bm.faces.ensure_lookup_table()
        bm.verts.ensure_lookup_table()
        bm.edges.ensure_lookup_table()

        texflag_layer = bm.faces.layers.int[0]
        stack_layer = bm.faces.layers.int[1]
        grid_flag_layer = bm.faces.layers.int[2]
        anim_layer = bm.faces.layers.int[3]

        out_file = open(file, "w")
        linesep = "\n" # os.linesep

        # Write Vertices:
        for vert in bm.verts:
            out_file.write("v " + str(vert.co.x) + " " + str(vert.co.y) + " " + str(vert.co.z) + linesep)
        out_file.write(linesep)

        # Setup Data:
        grid_stack_data = {}
        animation_faces = {}

        # Write Faces:
        uv_layer = mesh.uv_layers.new() if not mesh.uv_layers else mesh.uv_layers.active
        color_layer = mesh.vertex_colors.new()  if not mesh.vertex_colors else mesh.vertex_colors.active
        for poly_index, poly in enumerate(mesh.polygons):
            # Determine Vertices:
            verts = []
            for vert in poly.vertices:
                verts.append(str(vert))

            # Determine Colors:
            colors = []
            for i in range(0, poly.loop_total):
                colors.append(str(rgb_color_to_int(color_layer.data[poly.loop_start + i].color)))

            # Determine Polygon Type.
            textured = (poly.material_index != 0)
            quad = (len(verts) == 4)
            gouraud = False
            for test_color in colors:
                if test_color != colors[0]:
                    gouraud = True

            # Determine UVs:
            uvs = []
            flags = -1
            tex_id = -1
            if textured:
                flags = bm.faces[poly_index][texflag_layer]
                tex_id = int(bpy.data.materials[poly.material_index].name[3:])
                for i, loop_idx in enumerate(poly.loop_indices):
                    uvs.append(uv_layer.data[loop_idx].uv)

            # Stuff for each poly:
            show_text = "hide" if poly.hide else "show"

            stack_id = bm.faces[poly_index][stack_layer] - 1
            if stack_id != -1:
                if not stack_id in grid_stack_data:
                    grid_stack_data[stack_id] = []
                grid_stack_data[stack_id].append(poly_index)

            anim_id = bm.faces[poly_index][anim_layer] - 1
            if anim_id != -1:
                if not anim_id in animation_faces:
                    animation_faces[anim_id] = []
                animation_faces[anim_id].append(poly_index)

            if not textured and not quad and not gouraud: # f3 show v1 v2 v3 color
                out_file.write("f3 " + show_text + " " + verts[0] + " " + verts[1] + " " + verts[2] + " " + colors[0])
            elif not textured and not quad and gouraud: # g3 show v1 v2 v3 color1 color2 color3
                out_file.write("g3 " + show_text + " " + verts[0] + " " + verts[1] + " " + verts[2] + " " + colors[0] + " " + colors[1] + " " + colors[2])
            elif not textured and quad and not gouraud: # f4 show v1 v2 v3 v4 color
                out_file.write("f4 " + show_text + " " + verts[0] + " " + verts[1] + " " + verts[2] + " " + verts[3] + " " + colors[0])
            elif not textured and quad and gouraud: # g4 show v1 v2 v3 v4 color1 color2 color3 color4
                out_file.write("g4 " + show_text + " " + verts[0] + " " + verts[1] + " " + verts[2] + " " + verts[3] + " " + colors[0] + " " + colors[1] + " " + colors[2] + " " + colors[3])
            elif textured and not quad and not gouraud: # FT3 show v1 v2 v3 flags texture color uv1 uv2 uv3
                out_file.write("ft3 " + show_text + " " + verts[0] + " " + verts[1] + " " + verts[2] + " " + str(flags) + " " + str(tex_id) + " " + colors[0] + " " + uv_str(uvs[0]) + " " + uv_str(uvs[1]) + " " + uv_str(uvs[2]))
            elif textured and not quad and gouraud: # GT3 show v1 v2 v3 flags texture color1 color2 color3 uv1 uv2 uv3
                out_file.write("gt3 " + show_text + " " + verts[0] + " " + verts[1] + " " + verts[2] + " " + str(flags) + " " + str(tex_id) + " " + colors[0] + " " + colors[1] + " " + colors[2] + " " + uv_str(uvs[0]) + " " + uv_str(uvs[1]) + " " + uv_str(uvs[2]))
            elif textured and quad and not gouraud: # ft4 show v1 v2 v3 v4 flags texture color uv1 uv2 uv3 uv4
                out_file.write("ft4 " + show_text + " " + verts[0] + " " + verts[1] + " " + verts[2] + " " + verts[3] + " "  + str(flags) + " " + str(tex_id) + " " + colors[0] + " " + uv_str(uvs[0]) + " " + uv_str(uvs[1]) + " " + uv_str(uvs[2]) + " " + uv_str(uvs[3]))
            elif textured and quad and gouraud: # gt4 show v1 v2 v3 v4 flags texture color1 color2 color3 color4 uv1 uv2 uv3 uv4
                out_file.write("gt4 " + show_text + " " + verts[0] + " " + verts[1] + " " + verts[2] + " " + verts[3] + " "  + str(flags) + " " + str(tex_id) + " " + colors[0] + " " + colors[1] + " " + colors[2] + " " + colors[3] + " " + uv_str(uvs[0]) + " " + uv_str(uvs[1]) + " " + uv_str(uvs[2]) + " " + uv_str(uvs[3]))
            out_file.write(linesep)
        out_file.write(linesep)

        # Grid Data:
        out_file.write("grid-size " + str(mesh["gridX"]) + " " + str(mesh["gridZ"]) + linesep)
        for i in range(0, mesh["gridX"] * mesh["gridZ"]):
            out_file.write("grid " + str(mesh["GridHeights"][i]))

            if i in grid_stack_data:
                for poly_index in grid_stack_data[i]:
                    out_file.write(" " + str(poly_index) + ":" + str(bm.faces[poly_index][grid_flag_layer]))

            out_file.write(linesep)
        out_file.write(linesep)

        # Animation Data:
        for i, anim_data in enumerate(mesh["Animations"]):
            out_file.write("anim ")
            for anim_arg in anim_data:
                out_file.write(anim_arg + " ")

            for j, face_id in enumerate(animation_faces[i]):
                if j > 0:
                    out_file.write(",")
                out_file.write(str(face_id))
            out_file.write(linesep)
        out_file.write(linesep)

        out_file.close()
        print("Saved FFS file.")
        return {'FINISHED'}


def menu_func(self, context):
    self.layout.separator()
    self.layout.operator(LoadFfsOperator.bl_idname)
    self.layout.operator(SaveFfsOperator.bl_idname)


def register():
    bpy.utils.register_class(SaveFfsOperator)
    bpy.utils.register_class(LoadFfsOperator)
    bpy.types.VIEW3D_MT_object.append(menu_func)


def unregister():
    bpy.utils.unregister_class(SaveFfsOperator)
    bpy.utils.unregister_class(LoadFfsOperator)
    bpy.types.VIEW3D_MT_object.remove(menu_func)

if __name__ == "__main__":
    register()