#######################################
#  GREAT QUEST MODEL BLENDER PLUGIN   #
#######################################
# Blender add-on for importing/exporting Frogger: The Great Quest model data.
#
# Supported native files:
# - .vtx model files
# - .bhe animation skeleton resources
# - .bae animation track resources
#
# The add-on is intentionally conservative. It preserves the Great Quest model
# partitioning (nodes, primitives, primitive bone id lists, FVF component order)
# and refuses to export topology-changing edits unless the native metadata can
# still describe the result exactly.

bl_info = {
    "name": "Great Quest Model Support (FrogLord)",
    "category": "Import-Export",
    "blender": (2, 80, 0),
}

import base64
import json
import math
import os
import struct
import zlib

import bpy
from bpy.app.handlers import persistent
from bpy_extras.io_utils import ImportHelper, ExportHelper
from mathutils import Matrix, Quaternion, Vector

GAME_IDENTIFIER = "greatquest"

MODEL_SIGNATURE = b"6YTV"
SKELETON_SIGNATURE = b"fEHB"
TRACK_SIGNATURE = b"fEAB"

MODEL_EXT = ".vtx"
SKELETON_EXT = ".bhe"
ANIMATION_EXT = ".bae"

PROP_PREFIX = "greatquest_"
MESH_PROP = PROP_PREFIX + "mesh"
ARMATURE_PROP = PROP_PREFIX + "skeleton"
ACTION_PROP = PROP_PREFIX + "animation"
ARMATURE_ACTIONS_PROP = PROP_PREFIX + "actions"
LAST_ACTIVE_ACTION_PROP = PROP_PREFIX + "last_active_action"
IMPORTING_PROP = PROP_PREFIX + "importing"

UV_LAYER_NAME = "GreatQuest UV0"
COLOR_ATTR_NAME = "GreatQuest Diffuse"
UNTEXTURED_MATERIAL_NAME = "No Texture"
BONE_GROUP_PREFIX = "GQ_Bone_"
GREATQUEST_TICKS_PER_SECOND = 4800
GREATQUEST_ANIMATION_FPS = 60

FVF_FLAG_COMPRESSED = 0x4000
COMPRESSED_POSITION_UNIT = 16 * 150
COMPRESSED_MAIN_UNIT = 4096
COMPRESSED_OTHER_UNIT = 16
COMPRESSED_OLD_UV_UNIT = 32768

COMPONENTS = [
    ("NULL", -1, -1),
    ("POSITION_XYZF", 12, 6),
    ("POSITION_XYZWF", 16, 8),
    ("NORMAL_XYZF", 12, 6),
    ("NORMAL_XYZWF", 16, 8),
    ("DIFFUSE_RGBF", 12, 6),
    ("DIFFUSE_RGBAF", 16, 8),
    ("DIFFUSE_RGBAI", 4, 4),
    ("DIFFUSE_RGBA255F", 16, 8),
    ("SPECULAR_RGBF", 12, 6),
    ("SPECULAR_RGBAF", 16, 8),
    ("SPECULAR_RGBAI", 4, 4),
    ("SPECULAR_RGBA255F", 16, 8),
    ("WEIGHT1F", 4, 2),
    ("WEIGHT2F", 8, 4),
    ("TEX1F", 8, 4),
    ("TEX2F", 16, 8),
    ("TEX1_STQP", 16, 8),
    ("WEIGHT3F", 12, 6),
    ("WEIGHT4F", 16, 8),
    ("MATRIX_INDICES", 16, 8),
    ("PSIZE", 4, 2),
]

COMPONENT_BY_NAME = {name: i for i, (name, _, _) in enumerate(COMPONENTS)}
PRIMITIVE_TYPES = [
    "UNSUPPORTED",
    "POINT_LIST",
    "LINE_LIST",
    "LINE_STRIP",
    "TRIANGLE_LIST",
    "TRIANGLE_STRIP",
    "TRIANGLE_FAN",
]

CONTROL_TYPES = [
    ("USER", 0x0C),
    ("LINEAR_FLOAT", 0x14),
    ("LINEAR_ROTATION", 0x14),
    ("LINEAR_POSITION", 0x14),
    ("LINEAR_SCALE", 0x14),
    ("TCB_FLT", 0x58),
    ("TCB_ROTATION", 0x58),
    ("TCB_POSITION", 0x58),
    ("TCB_SCALE", 0x58),
    ("BEZIER_FLT", 0x38),
    ("BEZIER_POSITION", 0x38),
    ("BEZIER_SCALE", 0x38),
    ("POSITION_ROTATION_SCALE", 0x40),
    ("CAMERA", 0),
    ("LIGHT", 0),
    ("STD", 0),
    ("FLT", 0),
    ("POSITION", 0),
    ("ROTATION", 0),
    ("SCALE", 0),
    ("HIERARCHY", 0),
    ("INVALID", 0),
]

TRANSFORM_CONTROL_VECTOR_OFFSETS = {
    "LINEAR_ROTATION": 0,
    "LINEAR_POSITION": 0,
    "LINEAR_SCALE": 0,
    "TCB_ROTATION": 0,
    "TCB_POSITION": 0,
    "TCB_SCALE": 0,
    "BEZIER_POSITION": 0,
    "BEZIER_SCALE": 0,
}


class BinaryReader:
    def __init__(self, data):
        self.data = data
        self.index = 0

    def remaining(self):
        return len(self.data) - self.index

    def has_more(self):
        return self.index < len(self.data)

    def seek(self, index):
        self.index = index

    def tell(self):
        return self.index

    def read(self, size):
        if self.index + size > len(self.data):
            raise EOFError("Tried to read past end of file.")
        result = self.data[self.index:self.index + size]
        self.index += size
        return result

    def u8(self):
        return self.read(1)[0]

    def i16(self):
        return struct.unpack_from("<h", self.read(2))[0]

    def i32(self):
        return struct.unpack_from("<i", self.read(4))[0]

    def u32(self):
        return struct.unpack_from("<I", self.read(4))[0]

    def f32(self):
        return struct.unpack_from("<f", self.read(4))[0]

    def fixed_string(self, size):
        raw = self.read(size)
        raw = raw.split(b"\0", 1)[0]
        return raw.decode("cp1252", errors="replace")

    def align4(self):
        while self.index % 4:
            self.u8()


class BinaryWriter:
    def __init__(self):
        self.data = bytearray()

    def tell(self):
        return len(self.data)

    def write(self, data):
        self.data.extend(data)

    def u8(self, value):
        self.data.extend(struct.pack("<B", int(value) & 0xFF))

    def i16(self, value):
        self.data.extend(struct.pack("<h", int(value)))

    def i32(self, value):
        self.data.extend(struct.pack("<i", int(value)))

    def u32(self, value):
        self.data.extend(struct.pack("<I", int(value) & 0xFFFFFFFF))

    def f32(self, value):
        self.data.extend(struct.pack("<f", float(value)))

    def fixed_string(self, value, size):
        raw = (value or "").encode("cp1252", errors="replace")
        if len(raw) >= size:
            raise ValueError("String '%s' is too long for a %d byte field." % (value, size))
        self.write(raw)
        self.write(b"\0" * (size - len(raw)))

    def patch_i32(self, offset, value):
        self.data[offset:offset + 4] = struct.pack("<i", int(value))

    def null_pointer(self):
        offset = self.tell()
        self.i32(0)
        return offset

    def align4(self):
        while len(self.data) % 4:
            self.u8(0)

    def bytes(self):
        return bytes(self.data)


def read_file(path):
    with open(path, "rb") as f:
        return f.read()


def write_file(path, data):
    with open(path, "wb") as f:
        f.write(data)


def set_blob(owner, key, value):
    text = json.dumps(value, separators=(",", ":"))
    owner[key] = base64.b64encode(zlib.compress(text.encode("utf-8"), 9)).decode("ascii")


def get_blob(owner, key):
    if key not in owner:
        return None
    return json.loads(zlib.decompress(base64.b64decode(owner[key])).decode("utf-8"))


def int_to_color(value):
    return (
        ((value >> 16) & 0xFF) / 255.0,
        ((value >> 8) & 0xFF) / 255.0,
        (value & 0xFF) / 255.0,
        ((value >> 24) & 0xFF) / 255.0,
    )


def color_to_int(color):
    r = max(0, min(255, round(color[0] * 255)))
    g = max(0, min(255, round(color[1] * 255)))
    b = max(0, min(255, round(color[2] * 255)))
    a = max(0, min(255, round(color[3] * 255)))
    return (a << 24) | (r << 16) | (g << 8) | b


def read_cfloat(reader, unit):
    return reader.i16() / float(unit)


def write_cfloat(writer, value, unit):
    encoded = int(round(float(value) * unit))
    if encoded < -32768 or encoded > 32767:
        raise ValueError("Compressed float %f is outside signed 16-bit range." % value)
    writer.i16(encoded)


def is_compressed_fvf(fvf):
    return (fvf & FVF_FLAG_COMPRESSED) == FVF_FLAG_COMPRESSED


def component_stride(component_id, compressed):
    return COMPONENTS[component_id][2 if compressed else 1]


def vertex_stride(components, compressed):
    return sum(component_stride(component_id, compressed) for component_id in components)


def component_name(component_id):
    return COMPONENTS[component_id][0]


def read_material(reader):
    return {
        "material_name": reader.fixed_string(32),
        "texture_file_name": reader.fixed_string(32),
        "flags": reader.i32(),
        "xp_val": reader.f32(),
        "diffuse": [reader.f32(), reader.f32(), reader.f32(), reader.f32()],
        "ambient": [reader.f32(), reader.f32(), reader.f32(), reader.f32()],
        "specular": [reader.f32(), reader.f32(), reader.f32(), reader.f32()],
        "emissive": [reader.f32(), reader.f32(), reader.f32(), reader.f32()],
        "power": reader.f32(),
        "runtime_texture": reader.i32(),
    }


def write_material(writer, material):
    writer.fixed_string(material.get("material_name", ""), 32)
    writer.fixed_string(material.get("texture_file_name", ""), 32)
    writer.i32(material.get("flags", 0))
    writer.f32(material.get("xp_val", 0.0))
    for key in ("diffuse", "ambient", "specular", "emissive"):
        values = material.get(key, [1.0, 1.0, 1.0, 1.0])
        for i in range(4):
            writer.f32(values[i])
    writer.f32(material.get("power", 1.0))
    writer.i32(0)


def read_vertex_component(reader, vertex, component_id, compressed, old_uv_format=False):
    name = component_name(component_id)
    if not compressed:
        if name == "POSITION_XYZF":
            vertex["position"] = [reader.f32(), reader.f32(), reader.f32()]
            vertex["w"] = 1.0
        elif name == "POSITION_XYZWF":
            vertex["position"] = [reader.f32(), reader.f32(), reader.f32()]
            vertex["w"] = reader.f32()
        elif name == "NORMAL_XYZF":
            vertex["normal"] = [reader.f32(), reader.f32(), reader.f32()]
        elif name == "NORMAL_XYZWF":
            vertex["normal"] = [reader.f32(), reader.f32(), reader.f32()]
            vertex.setdefault("component_extra", {})[name] = [reader.f32()]
        elif name == "DIFFUSE_RGBF":
            r, g, b = reader.f32(), reader.f32(), reader.f32()
            vertex["diffuse"] = (round(r * 255) << 16) | (round(g * 255) << 8) | round(b * 255)
        elif name == "DIFFUSE_RGBAF":
            r, g, b, a = reader.f32(), reader.f32(), reader.f32(), reader.f32()
            vertex["diffuse"] = (round(a * 255) << 24) | (round(r * 255) << 16) | (round(g * 255) << 8) | round(b * 255)
        elif name == "DIFFUSE_RGBAI":
            vertex["diffuse"] = reader.u32()
        elif name == "DIFFUSE_RGBA255F":
            r, g, b, a = reader.f32(), reader.f32(), reader.f32(), reader.f32()
            vertex["diffuse"] = (round(a) << 24) | (round(r) << 16) | (round(g) << 8) | round(b)
        elif name in ("WEIGHT1F", "WEIGHT2F", "WEIGHT3F", "WEIGHT4F"):
            count = int(name[6])
            vertex["weights"] = [reader.f32() for _ in range(count)]
        elif name == "TEX1F":
            vertex["uv0"] = [reader.f32(), reader.f32()]
        elif name == "TEX2F":
            vertex["uv0"] = [reader.f32(), reader.f32()]
            vertex["uv1"] = [reader.f32(), reader.f32()]
        elif name == "TEX1_STQP":
            vertex["uv0"] = [reader.f32(), reader.f32()]
            vertex.setdefault("component_extra", {})[name] = [reader.f32(), reader.f32()]
        elif name == "PSIZE":
            vertex["point_size"] = reader.f32()
        else:
            vertex.setdefault("raw_components", {})[name] = list(reader.read(component_stride(component_id, False)))
        return

    if name == "POSITION_XYZF":
        vertex["position"] = [read_cfloat(reader, COMPRESSED_POSITION_UNIT), read_cfloat(reader, COMPRESSED_POSITION_UNIT), read_cfloat(reader, COMPRESSED_POSITION_UNIT)]
        vertex["w"] = 1.0
    elif name == "POSITION_XYZWF":
        vertex["position"] = [read_cfloat(reader, COMPRESSED_POSITION_UNIT), read_cfloat(reader, COMPRESSED_POSITION_UNIT), read_cfloat(reader, COMPRESSED_POSITION_UNIT)]
        vertex["w"] = read_cfloat(reader, COMPRESSED_POSITION_UNIT)
    elif name == "NORMAL_XYZF":
        vertex["normal"] = [read_cfloat(reader, COMPRESSED_MAIN_UNIT), read_cfloat(reader, COMPRESSED_MAIN_UNIT), read_cfloat(reader, COMPRESSED_MAIN_UNIT)]
    elif name == "NORMAL_XYZWF":
        vertex["normal"] = [read_cfloat(reader, COMPRESSED_MAIN_UNIT), read_cfloat(reader, COMPRESSED_MAIN_UNIT), read_cfloat(reader, COMPRESSED_MAIN_UNIT)]
        vertex.setdefault("component_extra", {})[name] = [read_cfloat(reader, COMPRESSED_MAIN_UNIT)]
    elif name in ("DIFFUSE_RGBAF", "DIFFUSE_RGBA255F"):
        r = reader.i16() & 0xFF
        g = reader.i16() & 0xFF
        b = reader.i16() & 0xFF
        a = reader.i16() & 0xFF
        vertex["diffuse"] = (a << 24) | (r << 16) | (g << 8) | b
    elif name in ("WEIGHT1F", "WEIGHT2F", "WEIGHT3F", "WEIGHT4F"):
        count = int(name[6])
        vertex["weights"] = [read_cfloat(reader, COMPRESSED_MAIN_UNIT) for _ in range(count)]
    elif name == "TEX1F":
        vertex["uv0"] = [read_cfloat(reader, COMPRESSED_MAIN_UNIT), read_cfloat(reader, COMPRESSED_MAIN_UNIT)]
    elif name == "TEX2F":
        vertex["uv0"] = [read_cfloat(reader, COMPRESSED_MAIN_UNIT), read_cfloat(reader, COMPRESSED_MAIN_UNIT)]
        vertex["uv1"] = [read_cfloat(reader, COMPRESSED_MAIN_UNIT), read_cfloat(reader, COMPRESSED_MAIN_UNIT)]
    elif name == "TEX1_STQP":
        uv_unit = COMPRESSED_OLD_UV_UNIT if old_uv_format else COMPRESSED_MAIN_UNIT
        vertex["uv0"] = [read_cfloat(reader, uv_unit), read_cfloat(reader, uv_unit)]
        vertex.setdefault("component_extra", {})[name] = [read_cfloat(reader, COMPRESSED_MAIN_UNIT), read_cfloat(reader, COMPRESSED_MAIN_UNIT)]
    elif name == "PSIZE":
        vertex["point_size"] = read_cfloat(reader, COMPRESSED_OTHER_UNIT)
    else:
        vertex.setdefault("raw_components", {})[name] = list(reader.read(component_stride(component_id, True)))


def write_vertex_component(writer, vertex, component_id, compressed, old_uv_format=False):
    name = component_name(component_id)
    weights = vertex.get("weights", [])
    uv0 = vertex.get("uv0", [0.0, 0.0])
    uv1 = vertex.get("uv1", [0.0, 0.0])
    normal = vertex.get("normal", [0.0, 0.0, 1.0])
    position = vertex.get("position", [0.0, 0.0, 0.0])
    diffuse = vertex.get("diffuse", 0xFFFFFFFF)
    extras = vertex.get("component_extra", {})

    if not compressed:
        if name == "POSITION_XYZF":
            writer.f32(position[0]); writer.f32(position[1]); writer.f32(position[2])
        elif name == "POSITION_XYZWF":
            writer.f32(position[0]); writer.f32(position[1]); writer.f32(position[2]); writer.f32(vertex.get("w", 1.0))
        elif name == "NORMAL_XYZF":
            writer.f32(normal[0]); writer.f32(normal[1]); writer.f32(normal[2])
        elif name == "NORMAL_XYZWF":
            writer.f32(normal[0]); writer.f32(normal[1]); writer.f32(normal[2]); writer.f32(extras.get(name, [1.0])[0])
        elif name == "DIFFUSE_RGBF":
            writer.f32(((diffuse >> 16) & 0xFF) / 255.0); writer.f32(((diffuse >> 8) & 0xFF) / 255.0); writer.f32((diffuse & 0xFF) / 255.0)
        elif name == "DIFFUSE_RGBAF":
            writer.f32(((diffuse >> 16) & 0xFF) / 255.0); writer.f32(((diffuse >> 8) & 0xFF) / 255.0); writer.f32((diffuse & 0xFF) / 255.0); writer.f32(((diffuse >> 24) & 0xFF) / 255.0)
        elif name == "DIFFUSE_RGBAI":
            writer.u32(diffuse)
        elif name == "DIFFUSE_RGBA255F":
            writer.f32((diffuse >> 16) & 0xFF); writer.f32((diffuse >> 8) & 0xFF); writer.f32(diffuse & 0xFF); writer.f32((diffuse >> 24) & 0xFF)
        elif name in ("WEIGHT1F", "WEIGHT2F", "WEIGHT3F", "WEIGHT4F"):
            count = int(name[6])
            for i in range(count):
                writer.f32(weights[i] if i < len(weights) else 0.0)
        elif name == "TEX1F":
            writer.f32(uv0[0]); writer.f32(uv0[1])
        elif name == "TEX2F":
            writer.f32(uv0[0]); writer.f32(uv0[1]); writer.f32(uv1[0]); writer.f32(uv1[1])
        elif name == "TEX1_STQP":
            extra = extras.get(name, [1.0, 0.0])
            writer.f32(uv0[0]); writer.f32(uv0[1]); writer.f32(extra[0]); writer.f32(extra[1])
        elif name == "PSIZE":
            writer.f32(vertex.get("point_size", 0.0))
        else:
            writer.write(bytes(vertex.get("raw_components", {}).get(name, [0] * component_stride(component_id, False))))
        return

    if name == "POSITION_XYZF":
        write_cfloat(writer, position[0], COMPRESSED_POSITION_UNIT); write_cfloat(writer, position[1], COMPRESSED_POSITION_UNIT); write_cfloat(writer, position[2], COMPRESSED_POSITION_UNIT)
    elif name == "POSITION_XYZWF":
        write_cfloat(writer, position[0], COMPRESSED_POSITION_UNIT); write_cfloat(writer, position[1], COMPRESSED_POSITION_UNIT); write_cfloat(writer, position[2], COMPRESSED_POSITION_UNIT); write_cfloat(writer, vertex.get("w", 1.0), COMPRESSED_POSITION_UNIT)
    elif name == "NORMAL_XYZF":
        write_cfloat(writer, normal[0], COMPRESSED_MAIN_UNIT); write_cfloat(writer, normal[1], COMPRESSED_MAIN_UNIT); write_cfloat(writer, normal[2], COMPRESSED_MAIN_UNIT)
    elif name == "NORMAL_XYZWF":
        write_cfloat(writer, normal[0], COMPRESSED_MAIN_UNIT); write_cfloat(writer, normal[1], COMPRESSED_MAIN_UNIT); write_cfloat(writer, normal[2], COMPRESSED_MAIN_UNIT); write_cfloat(writer, extras.get(name, [1.0])[0], COMPRESSED_MAIN_UNIT)
    elif name in ("DIFFUSE_RGBAF", "DIFFUSE_RGBA255F"):
        writer.i16((diffuse >> 16) & 0xFF); writer.i16((diffuse >> 8) & 0xFF); writer.i16(diffuse & 0xFF); writer.i16((diffuse >> 24) & 0xFF)
    elif name in ("WEIGHT1F", "WEIGHT2F", "WEIGHT3F", "WEIGHT4F"):
        count = int(name[6])
        for i in range(count):
            write_cfloat(writer, weights[i] if i < len(weights) else 0.0, COMPRESSED_MAIN_UNIT)
    elif name == "TEX1F":
        write_cfloat(writer, uv0[0], COMPRESSED_MAIN_UNIT); write_cfloat(writer, uv0[1], COMPRESSED_MAIN_UNIT)
    elif name == "TEX2F":
        write_cfloat(writer, uv0[0], COMPRESSED_MAIN_UNIT); write_cfloat(writer, uv0[1], COMPRESSED_MAIN_UNIT); write_cfloat(writer, uv1[0], COMPRESSED_MAIN_UNIT); write_cfloat(writer, uv1[1], COMPRESSED_MAIN_UNIT)
    elif name == "TEX1_STQP":
        extra = extras.get(name, [1.0, 1.0])
        uv_unit = COMPRESSED_OLD_UV_UNIT if old_uv_format else COMPRESSED_MAIN_UNIT
        write_cfloat(writer, uv0[0], uv_unit); write_cfloat(writer, uv0[1], uv_unit); write_cfloat(writer, extra[0], COMPRESSED_MAIN_UNIT); write_cfloat(writer, extra[1], COMPRESSED_MAIN_UNIT)
    elif name == "PSIZE":
        write_cfloat(writer, vertex.get("point_size", 0.0), COMPRESSED_OTHER_UNIT)
    else:
        writer.write(bytes(vertex.get("raw_components", {}).get(name, [0] * component_stride(component_id, True))))


def parse_vtx(data):
    reader = BinaryReader(data)
    if reader.read(4) != MODEL_SIGNATURE:
        raise ValueError("Not a Great Quest .vtx file.")
    declared_size = reader.i32()
    fvf = reader.i32()
    component_count = reader.u32()
    material_count = reader.u32()
    node_count = reader.u32()
    primitive_count = reader.u32()
    bones_per_primitive = reader.u32()
    vertex_count = reader.u32()
    components = [reader.i32() for _ in range(component_count)]
    if components[-1] != COMPONENT_BY_NAME["NULL"]:
        raise ValueError("VTX component list is missing its NULL terminator.")
    components = components[:-1]
    materials = [read_material(reader) for _ in range(material_count)]
    nodes = []
    for _ in range(node_count):
        nodes.append({"node_id": reader.i32(), "primitive_count": reader.u32()})
    primitives = []
    for _ in range(primitive_count):
        material_id = reader.i32()
        primitive_type = reader.i32()
        prim_vertex_count = reader.u32()
        primitives.append({
            "material_id": material_id,
            "primitive_type": primitive_type,
            "vertex_count": prim_vertex_count,
            "parent_node_id": 0,
            "bone_ids": [],
            "vertices": [],
        })
    primitive_index = 0
    for node in nodes:
        for _ in range(node["primitive_count"]):
            if primitive_index < len(primitives):
                primitives[primitive_index]["parent_node_id"] = node["node_id"]
            primitive_index += 1
    for prim in primitives:
        prim["bone_ids"] = [reader.u8() for _ in range(bones_per_primitive)]
    reader.align4()
    compressed = is_compressed_fvf(fvf)
    old_uv_format = False
    configured_stride = vertex_stride(components, compressed)
    real_stride_remainder = reader.remaining() % vertex_count if vertex_count else 0
    real_stride = reader.remaining() // vertex_count if vertex_count else 0
    if vertex_count and (real_stride_remainder != 0 or real_stride != configured_stride):
        if compressed and components and component_name(components[-1]) == "WEIGHT1F" and real_stride == 24 and configured_stride == 26:
            old_uv_format = True
            components = components[:-1]
        else:
            raise ValueError("VTX vertex stride mismatch: component stride is %d, but remaining vertex data has stride %d." % (configured_stride, real_stride))

    loaded_vertices = 0
    for prim in primitives:
        for _ in range(prim["vertex_count"]):
            vertex = {"position": [0.0, 0.0, 0.0], "normal": [0.0, 0.0, 1.0], "uv0": [0.0, 0.0], "diffuse": 0xFFFFFFFF}
            for component_id in components:
                read_vertex_component(reader, vertex, component_id, compressed, old_uv_format)
            prim["vertices"].append(vertex)
            loaded_vertices += 1
    if loaded_vertices != vertex_count:
        raise ValueError("VTX vertex count mismatch.")
    return {
        "type": "vtx",
        "declared_size": declared_size,
        "fvf": fvf,
        "components": components,
        "materials": materials,
        "nodes": nodes,
        "primitives": primitives,
        "bones_per_primitive": bones_per_primitive,
        "old_uv_format": old_uv_format,
    }


def write_vtx(model):
    writer = BinaryWriter()
    writer.write(MODEL_SIGNATURE)
    size_pos = writer.null_pointer()
    writer.i32(model["fvf"])
    writer.u32(len(model["components"]) + 1)
    writer.u32(len(model["materials"]))
    writer.u32(len(model["nodes"]))
    writer.u32(len(model["primitives"]))
    writer.u32(model["bones_per_primitive"])
    writer.u32(sum(len(prim["vertices"]) for prim in model["primitives"]))
    for component_id in model["components"]:
        writer.i32(component_id)
    writer.i32(COMPONENT_BY_NAME["NULL"])
    for material in model["materials"]:
        write_material(writer, material)
    for node in model["nodes"]:
        writer.i32(node["node_id"])
        writer.u32(node["primitive_count"])
    for prim in model["primitives"]:
        writer.i32(prim["material_id"])
        writer.u32(prim["primitive_type"])
        writer.u32(len(prim["vertices"]))
    for prim in model["primitives"]:
        bone_ids = prim.get("bone_ids", [])
        if len(bone_ids) != model["bones_per_primitive"]:
            raise ValueError("Primitive bone id count changed.")
        for bone_id in bone_ids:
            writer.u8(bone_id)
    writer.align4()
    compressed = is_compressed_fvf(model["fvf"])
    old_uv_format = bool(model.get("old_uv_format"))
    for prim in model["primitives"]:
        for vertex in prim["vertices"]:
            for component_id in model["components"]:
                write_vertex_component(writer, vertex, component_id, compressed, old_uv_format)
    writer.patch_i32(size_pos, writer.tell() - size_pos - 4)
    return writer.bytes()


def prim_faces(primitive_type, start, count):
    kind = PRIMITIVE_TYPES[primitive_type]
    faces = []
    if kind == "TRIANGLE_LIST":
        for i in range(0, count - 2, 3):
            faces.append((start + i, start + i + 1, start + i + 2))
    elif kind == "TRIANGLE_STRIP":
        for i in range(0, count - 2):
            if i % 2:
                faces.append((start + i + 1, start + i, start + i + 2))
            else:
                faces.append((start + i, start + i + 1, start + i + 2))
    elif kind == "TRIANGLE_FAN":
        for i in range(1, count - 1):
            faces.append((start, start + i, start + i + 1))
    return faces


def get_or_load_image(image_path):
    return bpy.data.images.load(image_path, check_existing=True)


def set_principled_input(bsdf, names, value):
    if not bsdf:
        return
    for name in names:
        if name in bsdf.inputs:
            bsdf.inputs[name].default_value = value
            return


def create_material(folder, material_info, material_cache=None):
    if material_cache is not None:
        cache_key = json.dumps(material_info, sort_keys=True, separators=(",", ":"))
        cached_material = material_cache.get(cache_key)
        if cached_material:
            return cached_material

    mat_name = material_info.get("material_name") or UNTEXTURED_MATERIAL_NAME
    mat = bpy.data.materials.new(mat_name)
    mat.use_nodes = True
    diffuse = list(material_info.get("diffuse", [1.0, 1.0, 1.0, 1.0]))
    if len(diffuse) < 4:
        diffuse += [1.0] * (4 - len(diffuse))

    # Great Quest material alpha and exported texture alpha are not always usable
    # as Blender material transparency. Keep imports visible by default; the
    # original values remain in the material metadata for export.
    viewport_alpha = diffuse[3] if 0.0 < diffuse[3] <= 1.0 else 1.0
    mat.diffuse_color = (diffuse[0], diffuse[1], diffuse[2], viewport_alpha)
    mat.blend_method = "OPAQUE"
    mat.use_screen_refraction = False

    bsdf = mat.node_tree.nodes.get("Principled BSDF") if mat.node_tree else None
    if bsdf:
        set_principled_input(bsdf, ("Metallic",), 0.0)
        set_principled_input(bsdf, ("Roughness",), 1.0)
        set_principled_input(bsdf, ("Specular IOR Level", "Specular"), 0.0)
        if "Alpha" in bsdf.inputs:
            bsdf.inputs["Alpha"].default_value = 1.0
        if "Base Color" in bsdf.inputs:
            bsdf.inputs["Base Color"].default_value = (diffuse[0], diffuse[1], diffuse[2], 1.0)

    texture_name = material_info.get("texture_file_name") or ""
    if texture_name:
        png_path = os.path.join(folder, os.path.splitext(texture_name)[0] + ".png")
        original_path = os.path.join(folder, texture_name)
        image_path = png_path if os.path.exists(png_path) else original_path
        if os.path.exists(image_path):
            nodes = mat.node_tree.nodes
            bsdf = nodes.get("Principled BSDF")
            tex = nodes.new(type="ShaderNodeTexImage")
            tex.image = get_or_load_image(image_path)
            tex.interpolation = "Linear"
            if bsdf and "Base Color" in bsdf.inputs:
                mat.node_tree.links.new(tex.outputs["Color"], bsdf.inputs["Base Color"])
    set_blob(mat, PROP_PREFIX + "material", material_info)
    if material_cache is not None:
        material_cache[cache_key] = mat
    return mat


def smooth_mesh_shading(mesh):
    for poly in mesh.polygons:
        poly.use_smooth = True
    mesh.update()


def import_vtx(context, filepath):
    model = parse_vtx(read_file(filepath))
    folder = os.path.dirname(filepath)
    verts = []
    faces = []
    face_materials = []
    vertex_to_prim = []
    for prim_index, prim in enumerate(model["primitives"]):
        start = len(verts)
        for vertex in prim["vertices"]:
            verts.append(tuple(vertex.get("position", [0.0, 0.0, 0.0])))
            vertex_to_prim.append(prim_index)
        prim_faces_list = prim_faces(prim["primitive_type"], start, len(prim["vertices"]))
        faces.extend(prim_faces_list)
        face_materials.extend([prim["material_id"]] * len(prim_faces_list))
    mesh = bpy.data.meshes.new(os.path.basename(filepath))
    mesh.from_pydata(verts, [], faces)
    mesh.update()
    obj = bpy.data.objects.new(os.path.splitext(os.path.basename(filepath))[0], mesh)
    context.collection.objects.link(obj)
    context.view_layer.objects.active = obj
    obj.select_set(True)
    material_cache = {}
    for material in model["materials"]:
        obj.data.materials.append(create_material(folder, material, material_cache))
    for poly, mat_index in zip(mesh.polygons, face_materials):
        if mat_index < len(mesh.materials):
            poly.material_index = mat_index
    uv_layer = mesh.uv_layers.new(name=UV_LAYER_NAME)
    flat_vertices = []
    for prim in model["primitives"]:
        flat_vertices.extend(prim["vertices"])
    smooth_mesh_shading(mesh)
    for poly in mesh.polygons:
        for loop_index in poly.loop_indices:
            vertex_index = mesh.loops[loop_index].vertex_index
            uv = flat_vertices[vertex_index].get("uv0", [0.0, 0.0])
            uv_layer.data[loop_index].uv = (uv[0], uv[1])
    try:
        color_attr = mesh.color_attributes.new(name=COLOR_ATTR_NAME, type="BYTE_COLOR", domain="CORNER")
        for poly in mesh.polygons:
            for loop_index in poly.loop_indices:
                vertex_index = mesh.loops[loop_index].vertex_index
                color_attr.data[loop_index].color = int_to_color(flat_vertices[vertex_index].get("diffuse", 0xFFFFFFFF))
    except Exception:
        pass
    vertex_start = 0
    for prim in model["primitives"]:
        parent_node_id = prim.get("parent_node_id", 0)
        parent_group = obj.vertex_groups.get("%s%03d" % (BONE_GROUP_PREFIX, parent_node_id)) or obj.vertex_groups.new(name="%s%03d" % (BONE_GROUP_PREFIX, parent_node_id))
        blend_bone_id = prim.get("bone_ids", [0])[0] if prim.get("bone_ids") else 0
        blend_group = obj.vertex_groups.get("%s%03d" % (BONE_GROUP_PREFIX, blend_bone_id)) or obj.vertex_groups.new(name="%s%03d" % (BONE_GROUP_PREFIX, blend_bone_id))
        for i, vertex in enumerate(prim["vertices"]):
            weights = vertex.get("weights", [])
            parent_weight = weights[0] if weights else 1.0
            blend_weight = 1.0 - parent_weight
            vertex_index = vertex_start + i
            if parent_weight:
                parent_group.add([vertex_index], parent_weight, "REPLACE")
            if blend_bone_id != 0 and blend_weight:
                blend_group.add([vertex_index], blend_weight, "REPLACE")
        vertex_start += len(prim["vertices"])
    meta = dict(model)
    meta["source_file"] = os.path.basename(filepath)
    meta["vertex_count"] = len(flat_vertices)
    meta["face_indices"] = [list(face) for face in faces]
    set_blob(obj, MESH_PROP, meta)
    return obj


def collect_mesh_edits(obj):
    model = get_blob(obj, MESH_PROP)
    if not model:
        raise ValueError("Selected object does not contain Great Quest model metadata.")
    mesh = obj.data
    expected_count = model.get("vertex_count")
    if len(mesh.vertices) != expected_count:
        raise ValueError("Topology-changing edits are not currently exportable: vertex count changed from %d to %d." % (expected_count, len(mesh.vertices)))
    expected_faces = model.get("face_indices", [])
    current_faces = [[vertex for vertex in poly.vertices] for poly in mesh.polygons]
    if current_faces != expected_faces:
        raise ValueError("Topology-changing edits are not currently exportable: Great Quest primitive layout no longer matches the imported mesh faces.")
    flat_vertices = []
    for prim in model["primitives"]:
        flat_vertices.extend(prim["vertices"])
    for i, mesh_vertex in enumerate(mesh.vertices):
        flat_vertices[i]["position"] = [mesh_vertex.co.x, mesh_vertex.co.y, mesh_vertex.co.z]
        normal = mesh_vertex.normal
        flat_vertices[i]["normal"] = [normal.x, normal.y, normal.z]
    uv_layer = mesh.uv_layers.get(UV_LAYER_NAME) or (mesh.uv_layers.active if mesh.uv_layers else None)
    if uv_layer:
        uv_totals = [[0.0, 0.0, 0] for _ in mesh.vertices]
        for poly in mesh.polygons:
            for loop_index in poly.loop_indices:
                vi = mesh.loops[loop_index].vertex_index
                uv = uv_layer.data[loop_index].uv
                uv_totals[vi][0] += uv.x
                uv_totals[vi][1] += uv.y
                uv_totals[vi][2] += 1
        for i, total in enumerate(uv_totals):
            if total[2]:
                flat_vertices[i]["uv0"] = [total[0] / total[2], total[1] / total[2]]
    color_attr = mesh.color_attributes.get(COLOR_ATTR_NAME) if hasattr(mesh, "color_attributes") else None
    if color_attr:
        color_totals = [[0.0, 0.0, 0.0, 0.0, 0] for _ in mesh.vertices]
        for poly in mesh.polygons:
            for loop_index in poly.loop_indices:
                vi = mesh.loops[loop_index].vertex_index
                color = color_attr.data[loop_index].color
                for j in range(4):
                    color_totals[vi][j] += color[j]
                color_totals[vi][4] += 1
        for i, total in enumerate(color_totals):
            if total[4]:
                flat_vertices[i]["diffuse"] = color_to_int([total[j] / total[4] for j in range(4)])
    cursor = 0
    for prim in model["primitives"]:
        bone_ids = prim.get("bone_ids", [])
        if bone_ids:
            parent_group = obj.vertex_groups.get("%s%03d" % (BONE_GROUP_PREFIX, prim.get("parent_node_id", 0)))
            for i, vertex in enumerate(prim["vertices"]):
                while len(vertex.setdefault("weights", [])) < len(bone_ids):
                    vertex["weights"].append(0.0)
                if not parent_group:
                    continue
                for g in mesh.vertices[cursor + i].groups:
                    if g.group == parent_group.index:
                        vertex["weights"][0] = g.weight
                        break
        cursor += len(prim["vertices"])
    return model


def export_vtx(context, filepath):
    obj = context.object
    if obj is None or obj.type != "MESH":
        raise ValueError("Select a Great Quest mesh object before exporting .vtx.")
    export_vtx_object(obj, filepath)


def export_vtx_object(obj, filepath):
    model = collect_mesh_edits(obj)
    write_file(filepath, write_vtx(model))


def parse_resource_header(reader, expected_signature):
    name = reader.fixed_string(32)
    signature = reader.read(4)
    if signature != expected_signature:
        raise ValueError("Expected %s resource, got %s." % (expected_signature.decode("ascii"), signature.decode("ascii", errors="replace")))
    size = reader.i32()
    return name, size


def write_resource_header(writer, name, signature):
    writer.fixed_string(name, 32)
    data_start = writer.tell()
    writer.write(signature)
    size_pos = writer.null_pointer()
    return data_start, size_pos


def read_vec3(reader):
    return [reader.f32(), reader.f32(), reader.f32()]


def write_vec3(writer, values):
    writer.f32(values[0]); writer.f32(values[1]); writer.f32(values[2])


def read_vec4(reader):
    return [reader.f32(), reader.f32(), reader.f32(), reader.f32()]


def write_vec4(writer, values):
    writer.f32(values[0]); writer.f32(values[1]); writer.f32(values[2]); writer.f32(values[3])


def parse_bhe_node(reader, base, parent_index, nodes):
    node_base = reader.tell()
    name = reader.fixed_string(32)
    tag = reader.i32()
    flags_with_child_count = reader.i32()
    reader.i32(); reader.i32()
    child_count = reader.i32()
    data_length = reader.i32()
    reader.i32()
    flags = flags_with_child_count & ~(0x1F << 16)
    if data_length != 64:
        raise ValueError("Unexpected skeleton node data length %d." % data_length)
    position = read_vec3(reader); reader.f32()
    rotation = read_vec4(reader)
    scale = read_vec3(reader); reader.f32()
    unused = read_vec4(reader)
    node_index = len(nodes)
    nodes.append({
        "name": name,
        "tag": tag,
        "flags": flags,
        "parent": parent_index,
        "children": [],
        "position": position,
        "rotation": rotation,
        "scale": scale,
        "unused": unused,
    })
    child_offsets = [reader.i32() for _ in range(child_count)]
    for offset in child_offsets:
        reader.seek(node_base + offset)
        child_index = parse_bhe_node(reader, node_base + offset, node_index, nodes)
        nodes[node_index]["children"].append(child_index)
    return node_index


def parse_bhe(data):
    reader = BinaryReader(data)
    name, size = parse_resource_header(reader, SKELETON_SIGNATURE)
    nodes = []
    parse_bhe_node(reader, reader.tell(), -1, nodes)
    return {"type": "bhe", "name": name, "size": size, "nodes": nodes}


def write_bhe_node(writer, nodes, node_index):
    node = nodes[node_index]
    children = node.get("children", [])
    if len(children) > 31:
        raise ValueError("Skeleton node '%s' has too many children." % node["name"])
    node_base = writer.tell()
    writer.fixed_string(node["name"], 32)
    writer.i32(node["tag"])
    writer.i32(node["flags"] | (len(children) << 16))
    writer.i32(0); writer.i32(0)
    writer.i32(len(children))
    writer.i32(64)
    writer.i32(0)
    write_vec3(writer, node["position"]); writer.f32(1.0)
    write_vec4(writer, node["rotation"])
    write_vec3(writer, node["scale"]); writer.f32(1.0)
    write_vec4(writer, node.get("unused", [1.0, 2.0, 3.0, 4.0]))
    child_table = writer.tell()
    for _ in children:
        writer.i32(0)
    for i, child_index in enumerate(children):
        writer.patch_i32(child_table + i * 4, writer.tell() - node_base)
        write_bhe_node(writer, nodes, child_index)


def write_bhe(skeleton):
    writer = BinaryWriter()
    data_start, size_pos = write_resource_header(writer, skeleton["name"], SKELETON_SIGNATURE)
    root_index = next((i for i, n in enumerate(skeleton["nodes"]) if n["parent"] < 0), 0)
    write_bhe_node(writer, skeleton["nodes"], root_index)
    writer.patch_i32(size_pos, writer.tell() - data_start)
    return writer.bytes()


def kc_quat_to_matrix(quat):
    x, y, z, w = quat
    sqx = x * x
    sqy = y * y
    sqz = z * z
    sqw = w * w
    dot = sqx + sqy + sqz + sqw
    xy = x * y
    xz = x * z
    xw = x * w
    yz = y * z
    yw = y * w
    zw = z * w
    s2 = (2.0 / dot) if dot > 0.0 else 0.0
    return [
        [1.0 - (s2 * (sqy + sqz)), s2 * (xy - zw), s2 * (xz + yw), 0.0],
        [s2 * (xy + zw), 1.0 - (s2 * (sqx + sqz)), s2 * (yz - xw), 0.0],
        [s2 * (xz - yw), s2 * (yz + xw), 1.0 - (s2 * (sqx + sqy)), 0.0],
        [0.0, 0.0, 0.0, 1.0],
    ]


def kc_matrix_multiply(left, right):
    result = [[0.0, 0.0, 0.0, 0.0] for _ in range(4)]
    for row in range(4):
        for col in range(4):
            result[row][col] = sum(left[row][i] * right[i][col] for i in range(4))
    return result


def kc_scale_matrix(scale):
    return [
        [scale[0], 0.0, 0.0, 0.0],
        [0.0, scale[1], 0.0, 0.0],
        [0.0, 0.0, scale[2], 0.0],
        [0.0, 0.0, 0.0, 1.0],
    ]


def kc_matrix_transform_point(matrix, point):
    x, y, z = point
    return Vector((
        matrix[0][0] * x + matrix[1][0] * y + matrix[2][0] * z + matrix[3][0],
        matrix[0][1] * x + matrix[1][1] * y + matrix[2][1] * z + matrix[3][1],
        matrix[0][2] * x + matrix[1][2] * y + matrix[2][2] * z + matrix[3][2],
    ))


def kc_matrix_transform_vector(matrix, vector):
    x, y, z = vector
    return Vector((
        matrix[0][0] * x + matrix[1][0] * y + matrix[2][0] * z,
        matrix[0][1] * x + matrix[1][1] * y + matrix[2][1] * z,
        matrix[0][2] * x + matrix[1][2] * y + matrix[2][2] * z,
    ))


def kc_local_offset_matrix(node):
    matrix = kc_quat_to_matrix(node.get("rotation", [0.0, 0.0, 0.0, 1.0]))
    # Matches kcNode.getLocalOffsetMatrix(): root translation is ignored.
    if node.get("tag", 0) != 0:
        position = node.get("position", [0.0, 0.0, 0.0])
        matrix[3][0] = position[0]
        matrix[3][1] = position[1]
        matrix[3][2] = position[2]
    return matrix


def kc_anim_local_matrix(node, position, rotation, scale):
    matrix = kc_quat_to_matrix(rotation)
    matrix[3][0] = position[0]
    matrix[3][1] = position[1]
    matrix[3][2] = position[2]
    if scale[0] != 1.0 or scale[1] != 1.0 or scale[2] != 1.0:
        matrix = kc_matrix_multiply(matrix, kc_scale_matrix(scale))
    return matrix


def kc_bind_matrix(node):
    matrix = kc_quat_to_matrix(node.get("rotation", [0.0, 0.0, 0.0, 1.0]))
    # Matches kcNode.getBoneToModelMatrix()/ComputeWToLInit(): root
    # translation is included for the bind/inverse-bind pose.
    position = node.get("position", [0.0, 0.0, 0.0])
    matrix[3][0] = position[0]
    matrix[3][1] = position[1]
    matrix[3][2] = position[2]
    return matrix


def calculate_froglord_bone_matrices(skeleton, bind_pose=False):
    matrices = {}
    root_indices = [i for i, node in enumerate(skeleton["nodes"]) if node.get("parent", -1) < 0]
    queue = list(root_indices)
    while queue:
        node_index = queue.pop(0)
        node = skeleton["nodes"][node_index]
        local_matrix = kc_bind_matrix(node) if bind_pose else kc_local_offset_matrix(node)
        parent_index = node.get("parent", -1)
        parent_matrix = matrices.get(parent_index)
        matrices[node_index] = kc_matrix_multiply(local_matrix, parent_matrix) if parent_matrix else local_matrix
        queue.extend(node.get("children", []))
    return matrices


def calculate_froglord_animation_matrices(skeleton, tracks_by_tag, tick):
    matrices = {}
    root_indices = [i for i, node in enumerate(skeleton["nodes"]) if node.get("parent", -1) < 0]
    queue = list(root_indices)
    while queue:
        node_index = queue.pop(0)
        node = skeleton["nodes"][node_index]
        position = get_rest_position(node)
        rotation = get_rest_rotation(node)
        scale = [1.0, 1.0, 1.0, 1.0]

        for track, control_name, transform_kind in tracks_by_tag.get(node.get("tag"), []):
            vector = interpolate_track_vector(control_name, track["keys"], tick)
            if vector is None:
                continue
            if transform_kind == "position":
                position = vector
            elif transform_kind == "rotation":
                rotation = vector
            elif transform_kind == "scale":
                scale = vector

        local_matrix = kc_anim_local_matrix(node, position, rotation, scale)
        parent_index = node.get("parent", -1)
        parent_matrix = matrices.get(parent_index)
        matrices[node_index] = kc_matrix_multiply(local_matrix, parent_matrix) if parent_matrix else local_matrix
        queue.extend(node.get("children", []))
    return matrices


def gq_global_matrix_to_blender_pose_matrix(gq_matrix):
    origin = kc_matrix_transform_point(gq_matrix, (0.0, 0.0, 0.0))
    gq_x = kc_matrix_transform_point(gq_matrix, (1.0, 0.0, 0.0)) - origin
    gq_y = kc_matrix_transform_point(gq_matrix, (0.0, 1.0, 0.0)) - origin
    gq_z = kc_matrix_transform_point(gq_matrix, (0.0, 0.0, 1.0)) - origin

    blender_x = -gq_y
    blender_y = gq_x
    blender_z = gq_z
    return Matrix((
        (blender_x.x, blender_y.x, blender_z.x, origin.x),
        (blender_x.y, blender_y.y, blender_z.y, origin.y),
        (blender_x.z, blender_y.z, blender_z.z, origin.z),
        (0.0, 0.0, 0.0, 1.0),
    ))


def import_bhe(context, filepath):
    skeleton = parse_bhe(read_file(filepath))
    arm_data = bpy.data.armatures.new(os.path.splitext(os.path.basename(filepath))[0] + "_Armature")
    arm_obj = bpy.data.objects.new(arm_data.name, arm_data)
    context.collection.objects.link(arm_obj)
    context.view_layer.objects.active = arm_obj
    arm_obj.select_set(True)
    arm_data.pose_position = "POSE"
    set_blob(arm_obj, ARMATURE_PROP, skeleton)
    bpy.ops.object.mode_set(mode="EDIT")
    matrices = calculate_froglord_bone_matrices(skeleton, bind_pose=True)
    for i, node in enumerate(skeleton["nodes"]):
        bone = arm_data.edit_bones.new(node["name"] or ("Bone_%03d" % node["tag"]))
        head = kc_matrix_transform_point(matrices[i], (0.0, 0.0, 0.0))
        tail = kc_matrix_transform_point(matrices[i], (0.05, 0.0, 0.0))
        if (tail - head).length < 0.001:
            tail = head + Vector((0.05, 0.0, 0.0))
        bone.head = head
        bone.tail = tail
        roll_axis = kc_matrix_transform_vector(matrices[i], (0.0, 0.0, 1.0))
        if roll_axis.length > 0.0001:
            bone.align_roll(roll_axis.normalized())
        if node["parent"] >= 0:
            parent_name = skeleton["nodes"][node["parent"]]["name"]
            bone.parent = arm_data.edit_bones.get(parent_name)
    bpy.ops.object.mode_set(mode="OBJECT")
    for bone in arm_data.bones:
        node = next((n for n in skeleton["nodes"] if n["name"] == bone.name), None)
        if node:
            bone[PROP_PREFIX + "tag"] = node["tag"]
            bone[PROP_PREFIX + "flags"] = node["flags"]
    return arm_obj


def export_bhe(context, filepath):
    obj = context.object
    if obj is None or obj.type != "ARMATURE":
        raise ValueError("Select a Great Quest armature before exporting .bhe.")
    export_bhe_object(obj, filepath)


def export_bhe_object(obj, filepath):
    skeleton = get_blob(obj, ARMATURE_PROP)
    if not skeleton:
        raise ValueError("Selected armature does not contain Great Quest skeleton metadata.")
    write_file(filepath, write_bhe(skeleton))


def parse_track_key(reader, control_type):
    control_name, expected_size = CONTROL_TYPES[control_type] if control_type < len(CONTROL_TYPES) else ("INVALID", 0)
    start = reader.tell()
    tick = reader.i32()
    payload_size = max(0, expected_size - 4)
    payload = list(reader.read(payload_size))
    key = {"tick": tick, "payload": payload}
    vector_offset = TRANSFORM_CONTROL_VECTOR_OFFSETS.get(control_name)
    if vector_offset is not None and payload_size >= vector_offset + 16:
        key["vector"] = list(struct.unpack("<ffff", bytes(payload[vector_offset:vector_offset + 16])))
    return key


def write_track_key(writer, control_type, key):
    control_name, expected_size = CONTROL_TYPES[control_type] if control_type < len(CONTROL_TYPES) else ("INVALID", 0)
    writer.i32(key["tick"])
    payload_size = max(0, expected_size - 4)
    payload = bytearray(key.get("payload", []))
    if len(payload) < payload_size:
        payload.extend(b"\0" * (payload_size - len(payload)))
    vector_offset = TRANSFORM_CONTROL_VECTOR_OFFSETS.get(control_name)
    if vector_offset is not None and "vector" in key and payload_size >= vector_offset + 16:
        payload[vector_offset:vector_offset + 16] = struct.pack("<ffff", *key["vector"])
    writer.write(bytes(payload[:payload_size]))


def get_transform_kind(control_name):
    if control_name not in TRANSFORM_CONTROL_VECTOR_OFFSETS:
        return None
    if "ROTATION" in control_name:
        return "rotation"
    if "POSITION" in control_name:
        return "position"
    if "SCALE" in control_name:
        return "scale"
    return None


def get_skeleton_node_by_tag(armature, tag):
    skeleton = get_blob(armature, ARMATURE_PROP) if armature else None
    if not skeleton:
        return None
    for node in skeleton.get("nodes", []):
        if node.get("tag") == tag:
            return node
    return None


def get_rest_position(node):
    if not node or node.get("tag", 0) == 0:
        return [0.0, 0.0, 0.0]
    return node.get("position", [0.0, 0.0, 0.0])


def get_rest_rotation(node):
    if not node:
        return [0.0, 0.0, 0.0, 1.0]
    return node.get("rotation", [0.0, 0.0, 0.0, 1.0])


def game_quat_to_blender(quat):
    return Quaternion((quat[3], quat[0], quat[1], quat[2]))


def blender_quat_to_game(quat):
    return [quat.x, quat.y, quat.z, quat.w]


def safe_matrix_inverse(matrix):
    if hasattr(matrix, "inverted_safe"):
        try:
            return matrix.inverted_safe()
        except Exception:
            pass
    try:
        return matrix.inverted()
    except Exception:
        return Matrix.Identity(4)


def matrix_has_inverse(matrix, epsilon=1.0e-8):
    try:
        return abs(matrix.determinant()) > epsilon
    except Exception:
        try:
            matrix.inverted()
            return True
        except Exception:
            return False


def clear_action_fcurves(action):
    for fcurve in list(action.fcurves):
        action.fcurves.remove(fcurve)


# Blender local X = -Great Quest Y, local Y = Great Quest X, local Z = Great Quest Z.
# This matrix maps Blender bone-local vectors into Great Quest bone-local vectors.
BLENDER_TO_GQ_BONE_BASIS = Matrix((
    (0.0, 1.0, 0.0, 0.0),
    (-1.0, 0.0, 0.0, 0.0),
    (0.0, 0.0, 1.0, 0.0),
    (0.0, 0.0, 0.0, 1.0),
))
GQ_TO_BLENDER_BONE_BASIS = BLENDER_TO_GQ_BONE_BASIS.inverted()


def gq_rotation_matrix_to_blender(matrix):
    # Great Quest matrices are evaluated with row-vector semantics and store
    # translation in row 3. mathutils uses column-vector multiplication, so the
    # rotational 3x3 must be transposed before Blender quaternion conversion.
    return Matrix((
        (matrix[0][0], matrix[1][0], matrix[2][0], 0.0),
        (matrix[0][1], matrix[1][1], matrix[2][1], 0.0),
        (matrix[0][2], matrix[1][2], matrix[2][2], 0.0),
        (0.0, 0.0, 0.0, 1.0),
    ))


def gq_local_rotation_matrix(node, quat):
    matrix = kc_quat_to_matrix(quat)
    return gq_rotation_matrix_to_blender(matrix)


def gq_to_blender_pose_quat(node, vector):
    rest = gq_local_rotation_matrix(node, get_rest_rotation(node))
    anim = gq_local_rotation_matrix(node, vector)
    pose_gq = safe_matrix_inverse(rest) @ anim
    pose_blender = BLENDER_TO_GQ_BONE_BASIS @ pose_gq @ GQ_TO_BLENDER_BONE_BASIS
    return pose_blender.to_quaternion()


def blender_pose_quat_to_gq(node, pose_quat):
    rest = gq_local_rotation_matrix(node, get_rest_rotation(node))
    pose_blender = pose_quat.to_matrix().to_4x4()
    pose_gq = GQ_TO_BLENDER_BONE_BASIS @ pose_blender @ BLENDER_TO_GQ_BONE_BASIS
    anim = rest @ pose_gq
    return blender_quat_to_game(anim.to_quaternion())


def transform_basis_vector(matrix, vector):
    x, y, z = vector
    return [
        matrix[0][0] * x + matrix[0][1] * y + matrix[0][2] * z,
        matrix[1][0] * x + matrix[1][1] * y + matrix[1][2] * z,
        matrix[2][0] * x + matrix[2][1] * y + matrix[2][2] * z,
    ]


def gq_position_delta_to_blender(delta):
    return transform_basis_vector(GQ_TO_BLENDER_BONE_BASIS, delta)


def blender_position_delta_to_gq(delta):
    return transform_basis_vector(BLENDER_TO_GQ_BONE_BASIS, delta)


def game_vector_to_pose_channel(transform_kind, node, vector, axis):
    if transform_kind == "position":
        rest = get_rest_position(node)
        delta = [vector[0] - rest[0], vector[1] - rest[1], vector[2] - rest[2]]
        return gq_position_delta_to_blender(delta)[axis]
    if transform_kind == "scale":
        return vector[axis]

    pose_quat = gq_to_blender_pose_quat(node, vector)
    return pose_quat.w if axis == 0 else pose_quat[axis]


def pose_channels_to_game_vector(transform_kind, node, values, old):
    if transform_kind == "position":
        rest = get_rest_position(node)
        delta = blender_position_delta_to_gq(values)
        return [rest[0] + delta[0], rest[1] + delta[1], rest[2] + delta[2], old[3] if len(old) > 3 else 1.0]
    if transform_kind == "scale":
        return [values[0], values[1], values[2], old[3] if len(old) > 3 else 1.0]

    pose_quat = Quaternion((values[0], values[1], values[2], values[3]))
    return blender_pose_quat_to_gq(node, pose_quat)


def ticks_to_frame(context, tick):
    return (float(tick) / GREATQUEST_TICKS_PER_SECOND) * GREATQUEST_ANIMATION_FPS


def frame_to_ticks(context, frame):
    return int(round((float(frame) / GREATQUEST_ANIMATION_FPS) * GREATQUEST_TICKS_PER_SECOND))


def set_greatquest_scene_fps(context):
    scene = context.scene
    scene.render.fps = GREATQUEST_ANIMATION_FPS
    scene.render.fps_base = 1.0


def build_armature_tag_to_bone_map(armature):
    tag_to_bone = {}
    if not armature:
        return tag_to_bone

    for bone in armature.data.bones:
        tag = bone.get(PROP_PREFIX + "tag")
        if tag is not None:
            tag_to_bone[int(tag)] = bone.name

    skeleton = get_blob(armature, ARMATURE_PROP)
    if skeleton:
        for node in skeleton.get("nodes", []):
            bone_name = node.get("name")
            tag = node.get("tag")
            if bone_name in armature.data.bones and tag is not None:
                tag_to_bone.setdefault(int(tag), bone_name)

    # Last-resort viewing fallback for old imports missing tag metadata.
    if not tag_to_bone:
        for index, bone in enumerate(armature.data.bones):
            tag_to_bone[index] = bone.name

    return tag_to_bone


def copy_vertex_group_weights(obj, source_group, target_group):
    if not obj or not source_group or not target_group or obj.type != "MESH":
        return
    mesh = obj.data
    for vertex in mesh.vertices:
        weight = None
        for group_ref in vertex.groups:
            if group_ref.group == source_group.index:
                weight = group_ref.weight
                break
        if weight is not None:
            target_group.add([vertex.index], weight, "REPLACE")


def find_key_pair(keys, tick):
    if not keys:
        return None, None, 0.0
    if tick <= keys[0]["tick"]:
        return keys[0], keys[0], 0.0
    for index in range(len(keys) - 1):
        current_key = keys[index]
        next_key = keys[index + 1]
        if current_key["tick"] <= tick <= next_key["tick"]:
            span = next_key["tick"] - current_key["tick"]
            t = 0.0 if span == 0 else (tick - current_key["tick"]) / span
            return current_key, next_key, t
    return keys[-1], keys[-1], 1.0


def interpolate_track_vector(control_name, keys, tick):
    current_key, next_key, t = find_key_pair(keys, tick)
    if not current_key:
        return None

    current = current_key.get("vector")
    next_value = next_key.get("vector") if next_key else None
    if current is None:
        return None
    if next_value is None or current_key is next_key:
        return list(current)

    # FrogLord has richer TCB/Bezier interpolation. For Action editing, exact
    # key poses matter most; linear interpolation is a Blender-friendly preview.
    if "ROTATION" in control_name:
        q1 = game_quat_to_blender(current)
        q2 = game_quat_to_blender(next_value)
        return blender_quat_to_game(q1.slerp(q2, max(0.0, min(1.0, t))))
    return [
        current[0] + (next_value[0] - current[0]) * t,
        current[1] + (next_value[1] - current[1]) * t,
        current[2] + (next_value[2] - current[2]) * t,
        current[3] + (next_value[3] - current[3]) * t,
    ]


def bake_greatquest_action(context, armature, action, animation, tag_to_bone):
    skeleton = get_blob(armature, ARMATURE_PROP) if armature else None
    if not skeleton:
        return 0

    tracks_by_tag = {}
    for track in animation["tracks"]:
        control_name = CONTROL_TYPES[track["control_type"]][0] if track["control_type"] < len(CONTROL_TYPES) else "INVALID"
        transform_kind = get_transform_kind(control_name)
        if transform_kind is None or not track["keys"]:
            continue
        tracks_by_tag.setdefault(track["tag"], []).append((track, control_name, transform_kind))

    ticks = sorted({key["tick"] for track_infos in tracks_by_tag.values() for track, _control_name, _transform_kind in track_infos for key in track["keys"]})
    if not ticks:
        return 0

    animated_tags = set(tracks_by_tag.keys())
    pose_bones_by_node = []
    rest_inverse_by_node = []
    parent_rest_by_node = []
    for node in skeleton["nodes"]:
        bone_name = tag_to_bone.get(node.get("tag"))
        pose_bone = armature.pose.bones.get(bone_name) if bone_name else None
        pose_bones_by_node.append(pose_bone)
        if pose_bone and not matrix_has_inverse(pose_bone.bone.matrix_local):
            clear_action_fcurves(action)
            return 0
        rest_inverse_by_node.append(safe_matrix_inverse(pose_bone.bone.matrix_local) if pose_bone else None)
        parent_index = node.get("parent", -1)
        parent_pose_bone = pose_bones_by_node[parent_index] if parent_index >= 0 and parent_index < len(pose_bones_by_node) else None
        parent_rest_by_node.append(parent_pose_bone.bone.matrix_local if parent_pose_bone else None)
    pose_bones_by_tag = {
        tag: pose_bones_by_node[index]
        for index, node in enumerate(skeleton["nodes"])
        for tag in [node.get("tag")]
        if tag in animated_tags and pose_bones_by_node[index]
    }
    for tick in ticks:
        frame = ticks_to_frame(context, tick)
        matrices = calculate_froglord_animation_matrices(skeleton, tracks_by_tag, tick)

        for pose_bone in armature.pose.bones:
            pose_bone.location = (0.0, 0.0, 0.0)
            pose_bone.rotation_mode = "QUATERNION"
            pose_bone.rotation_quaternion = (1.0, 0.0, 0.0, 0.0)
            pose_bone.scale = (1.0, 1.0, 1.0)

        pose_matrices = []
        for node_index, node in enumerate(skeleton["nodes"]):
            pose_matrices.append(gq_global_matrix_to_blender_pose_matrix(matrices[node_index]))
        pose_inverse_matrices = [None] * len(pose_matrices)

        for node_index, node in enumerate(skeleton["nodes"]):
            pose_bone = pose_bones_by_node[node_index]
            if not pose_bone:
                continue

            pose_bone.rotation_mode = "QUATERNION"
            pose_matrix = pose_matrices[node_index]
            parent_index = node.get("parent", -1)
            rest_inverse = rest_inverse_by_node[node_index]
            parent_rest = parent_rest_by_node[node_index]
            if parent_index >= 0 and parent_rest is not None:
                if pose_inverse_matrices[parent_index] is None:
                    parent_pose_matrix = pose_matrices[parent_index]
                    if not matrix_has_inverse(parent_pose_matrix):
                        clear_action_fcurves(action)
                        return 0
                    pose_inverse_matrices[parent_index] = safe_matrix_inverse(parent_pose_matrix)
                pose_bone.matrix_basis = rest_inverse @ parent_rest @ pose_inverse_matrices[parent_index] @ pose_matrix
            else:
                pose_bone.matrix_basis = rest_inverse @ pose_matrix

        context.view_layer.update()

        for tag in animated_tags:
            pose_bone = pose_bones_by_tag.get(tag)
            if not pose_bone:
                continue

            pose_bone.keyframe_insert(data_path="location", frame=frame)
            pose_bone.keyframe_insert(data_path="rotation_quaternion", frame=frame)
            pose_bone.keyframe_insert(data_path="scale", frame=frame)

    for track in animation["tracks"]:
        for key in track["keys"]:
            key["frame"] = ticks_to_frame(context, key["tick"])

    created_curve_count = len(action.fcurves)
    if created_curve_count <= 0:
        clear_action_fcurves(action)
        return 0

    for fcurve in action.fcurves:
        for point in fcurve.keyframe_points:
            point.interpolation = "LINEAR"
        fcurve.extrapolation = "CONSTANT"
        fcurve.update()

    action[PROP_PREFIX + "matrix_bake"] = True
    return created_curve_count


def parse_bae(data):
    reader = BinaryReader(data)
    name, size = parse_resource_header(reader, TRACK_SIGNATURE)
    base = reader.tell()
    tracks = []
    while reader.has_more():
        track_start = reader.tell()
        packed = reader.i32()
        tag = reader.i32()
        key_count = reader.i32()
        reader.i32()
        next_track = reader.i32()
        control_type = (packed >> 24) & 0xFF
        offsets_start = reader.tell()
        offsets = [reader.i32() for _ in range(key_count)]
        data_start = reader.tell()
        data_end = base + next_track if next_track else len(data)
        keys = []
        for offset in offsets:
            reader.seek(data_start + offset)
            keys.append(parse_track_key(reader, control_type))
        reader.seek(data_end)
        tracks.append({"packed": packed, "tag": tag, "control_type": control_type, "keys": keys, "track_start": track_start - base})
    return {"type": "bae", "name": name, "size": size, "tracks": tracks}


def write_bae(animation):
    writer = BinaryWriter()
    data_start, size_pos = write_resource_header(writer, animation["name"], TRACK_SIGNATURE)
    base = writer.tell()
    tracks = animation["tracks"]
    for track_index, track in enumerate(tracks):
        packed = track["packed"]
        flags = (packed >> 17) & 0x7F
        if track_index == 0:
            flags |= 0x20
        else:
            flags &= ~0x20
        if track_index + 1 < len(tracks):
            flags |= 0x10
        else:
            flags &= ~0x10
        packed = (packed & ~(0x7F << 17)) | (flags << 17)
        writer.i32(packed)
        writer.i32(track["tag"])
        writer.i32(len(track["keys"]))
        writer.i32(0)
        next_track_pos = writer.null_pointer()
        offsets_start = writer.tell()
        for _ in track["keys"]:
            writer.i32(0)
        keys_start = writer.tell()
        last_tick = -2147483648
        for key_index, key in enumerate(track["keys"]):
            if key["tick"] < last_tick:
                raise ValueError("Animation keys must be sorted by tick.")
            last_tick = key["tick"]
            writer.patch_i32(offsets_start + key_index * 4, writer.tell() - keys_start)
            write_track_key(writer, track["control_type"], key)
        if track_index + 1 < len(tracks):
            writer.patch_i32(next_track_pos, writer.tell() - base)
    writer.patch_i32(size_pos, writer.tell() - data_start)
    return writer.bytes()


def import_bae(context, filepath, armature=None, use_context_armature=True):
    set_greatquest_scene_fps(context)
    animation = parse_bae(read_file(filepath))
    action = bpy.data.actions.new(os.path.splitext(os.path.basename(filepath))[0])
    action.use_fake_user = True
    set_blob(action, ACTION_PROP, animation)
    if armature is None and use_context_armature:
        armature = context.object if context.object and context.object.type == "ARMATURE" else None
    tag_to_bone = build_armature_tag_to_bone_map(armature)
    created_curve_count = 0
    skipped_no_bone = 0
    skipped_unsupported = 0
    skipped_no_keys = 0
    if armature:
        armature.animation_data_create()
        armature.animation_data.action = action
        was_importing = bool(context.scene.get(IMPORTING_PROP))
        context.scene[IMPORTING_PROP] = True
        try:
            created_curve_count = bake_greatquest_action(context, armature, action, animation, tag_to_bone)
        finally:
            context.scene[IMPORTING_PROP] = was_importing

    if created_curve_count > 0:
        action[PROP_PREFIX + "track_count"] = len(animation["tracks"])
        action[PROP_PREFIX + "fcurve_count"] = len(action.fcurves)
        action[PROP_PREFIX + "skipped_no_bone"] = 0
        action[PROP_PREFIX + "skipped_unsupported"] = 0
        action[PROP_PREFIX + "skipped_no_keys"] = 0
        return action

    for track in animation["tracks"]:
        control_name = CONTROL_TYPES[track["control_type"]][0] if track["control_type"] < len(CONTROL_TYPES) else "INVALID"
        bone_name = tag_to_bone.get(track["tag"])
        node = get_skeleton_node_by_tag(armature, track["tag"])
        transform_kind = get_transform_kind(control_name)
        if not bone_name:
            skipped_no_bone += 1
            continue
        if transform_kind is None:
            skipped_unsupported += 1
            continue
        if not track["keys"]:
            skipped_no_keys += 1
            continue
        pose_bone = armature.pose.bones.get(bone_name) if armature else None
        if not pose_bone:
            skipped_no_bone += 1
            continue
        if transform_kind == "rotation":
            pose_bone.rotation_mode = "QUATERNION"

        for key in track["keys"]:
            vector = key.get("vector", [0, 0, 0, 1])
            frame = ticks_to_frame(context, key["tick"])
            key["frame"] = frame
            if transform_kind == "position":
                pose_bone.location = [game_vector_to_pose_channel(transform_kind, node, vector, axis) for axis in range(3)]
                pose_bone.keyframe_insert(data_path="location", frame=frame)
            elif transform_kind == "scale":
                pose_bone.scale = [game_vector_to_pose_channel(transform_kind, node, vector, axis) for axis in range(3)]
                pose_bone.keyframe_insert(data_path="scale", frame=frame)
            else:
                pose_bone.rotation_quaternion = [game_vector_to_pose_channel(transform_kind, node, vector, axis) for axis in range(4)]
                pose_bone.keyframe_insert(data_path="rotation_quaternion", frame=frame)

    for fcurve in action.fcurves:
        for point in fcurve.keyframe_points:
            point.interpolation = "LINEAR"
        fcurve.update()
    created_curve_count = len(action.fcurves)
    action[PROP_PREFIX + "track_count"] = len(animation["tracks"])
    action[PROP_PREFIX + "fcurve_count"] = created_curve_count
    action[PROP_PREFIX + "skipped_no_bone"] = skipped_no_bone
    action[PROP_PREFIX + "skipped_unsupported"] = skipped_unsupported
    action[PROP_PREFIX + "skipped_no_keys"] = skipped_no_keys
    return action


def apply_action_edits(animation, action, armature):
    if not action or not armature:
        return animation
    if action.get(PROP_PREFIX + "matrix_bake"):
        return animation
    tag_to_bone = build_armature_tag_to_bone_map(armature)
    for track in animation["tracks"]:
        control_name = CONTROL_TYPES[track["control_type"]][0] if track["control_type"] < len(CONTROL_TYPES) else "INVALID"
        bone_name = tag_to_bone.get(track["tag"])
        node = get_skeleton_node_by_tag(armature, track["tag"])
        transform_kind = get_transform_kind(control_name)
        if not bone_name or transform_kind is None:
            continue
        if transform_kind == "position":
            data_path = 'pose.bones["%s"].location' % bone_name
        elif transform_kind == "scale":
            data_path = 'pose.bones["%s"].scale' % bone_name
        else:
            data_path = 'pose.bones["%s"].rotation_quaternion' % bone_name
        curves = {fc.array_index: fc for fc in action.fcurves if fc.data_path == data_path}
        if not curves:
            continue
        timing_curve = curves.get(0) or next(iter(curves.values()))
        for key_index, key in enumerate(track["keys"]):
            frame = key.get("frame", ticks_to_frame(bpy.context, key["tick"]))
            key["tick"] = frame_to_ticks(bpy.context, frame)
            old = key.get("vector", [0.0, 0.0, 0.0, 1.0])
            if transform_kind == "rotation":
                values = [
                    curves.get(0).evaluate(frame) if 0 in curves else old[3],
                    curves.get(1).evaluate(frame) if 1 in curves else old[0],
                    curves.get(2).evaluate(frame) if 2 in curves else old[1],
                    curves.get(3).evaluate(frame) if 3 in curves else old[2],
                ]
            else:
                values = [
                    curves.get(0).evaluate(frame) if 0 in curves else old[0],
                    curves.get(1).evaluate(frame) if 1 in curves else old[1],
                    curves.get(2).evaluate(frame) if 2 in curves else old[2],
                ]
            key["vector"] = pose_channels_to_game_vector(transform_kind, node, values, old)
    return animation


def export_bae(context, filepath):
    action = None
    armature = context.object if context.object and context.object.type == "ARMATURE" else None
    if armature and armature.animation_data:
        action = armature.animation_data.action
    if action is None:
        action = context.scene.animation_data.action if context.scene.animation_data else None
    if action is None:
        raise ValueError("Select an armature with a Great Quest action before exporting .bae.")
    export_bae_action(action, armature, filepath)


def export_bae_action(action, armature, filepath):
    animation = get_blob(action, ACTION_PROP)
    if not animation:
        raise ValueError("Selected action does not contain Great Quest animation metadata.")
    animation = apply_action_edits(animation, action, armature)
    write_file(filepath, write_bae(animation))


def get_action_frame_range(action):
    if not action:
        return 0, 1
    start = None
    end = None

    animation = get_blob(action, ACTION_PROP)
    if animation:
        for track in animation.get("tracks", []):
            control_name = CONTROL_TYPES[track["control_type"]][0] if track["control_type"] < len(CONTROL_TYPES) else "INVALID"
            if get_transform_kind(control_name) is None:
                continue
            for key in track.get("keys", []):
                frame = key.get("frame")
                if frame is None:
                    frame = ticks_to_frame(bpy.context, key["tick"])
                start = frame if start is None else min(start, frame)
                end = frame if end is None else max(end, frame)

    for fcurve in action.fcurves:
        for key in fcurve.keyframe_points:
            frame = key.co.x
            start = frame if start is None else min(start, frame)
            end = frame if end is None else max(end, frame)
    if start is None or end is None:
        return 0, 1
    start_frame = math.floor(start)
    end_frame = math.ceil(end)
    return start_frame, max(end_frame, start_frame + 1)


def remember_armature_action(armature, action):
    if not armature or not action:
        return
    actions = list(armature.get(ARMATURE_ACTIONS_PROP, []))
    if action.name not in actions:
        actions.append(action.name)
        armature[ARMATURE_ACTIONS_PROP] = actions


def set_action_display_range(action, start, end):
    if not action:
        return
    for attr, value in (
        ("use_frame_range", False),
        ("frame_start", start),
        ("frame_end", end),
    ):
        if hasattr(action, attr):
            try:
                setattr(action, attr, value)
            except Exception:
                pass


def set_scene_playback_range(context, start, end, reset_frame=True):
    scene = context.scene
    scene.frame_start = start
    scene.frame_end = end
    for attr, value in (
        ("frame_preview_start", start),
        ("frame_preview_end", end),
    ):
        if hasattr(scene, attr):
            try:
                setattr(scene, attr, value)
            except Exception:
                pass
    scene.use_preview_range = False
    if reset_frame:
        scene.frame_set(start)


def register_armature_action(armature, action):
    if not armature or not action:
        return 0, 1
    start, end = get_action_frame_range(action)
    set_action_display_range(action, start, end)
    remember_armature_action(armature, action)
    return start, end


def set_active_greatquest_action(context, armature, action):
    if not armature or not action:
        return
    set_greatquest_scene_fps(context)
    armature.animation_data_create()
    start, end = register_armature_action(armature, action)
    armature.animation_data.action = action
    set_scene_playback_range(context, start, end)
    context.scene[LAST_ACTIVE_ACTION_PROP] = "%s|%s|%d|%d" % (armature.name, action.name, start, end)


@persistent
def greatquest_active_action_range_handler(scene, depsgraph):
    context = bpy.context
    if not context or not context.scene:
        return
    if context.scene.get(IMPORTING_PROP):
        return
    sync_selected_greatquest_action_range(context, reset_frame=False)


def bind_mesh_to_armature(mesh_obj, armature_obj):
    if not mesh_obj or not armature_obj:
        return
    modifier = mesh_obj.modifiers.get("Great Quest Skeleton") or mesh_obj.modifiers.new("Great Quest Skeleton", "ARMATURE")
    modifier.object = armature_obj

    tag_to_bone = build_armature_tag_to_bone_map(armature_obj)
    for tag, bone_name in tag_to_bone.items():
        source_group = mesh_obj.vertex_groups.get("%s%03d" % (BONE_GROUP_PREFIX, tag))
        if not source_group:
            continue
        target_group = mesh_obj.vertex_groups.get(bone_name) or mesh_obj.vertex_groups.new(name=bone_name)
        copy_vertex_group_weights(mesh_obj, source_group, target_group)


def move_object_to_collection(obj, collection):
    if not obj or not collection:
        return
    if obj.name not in collection.objects.keys():
        collection.objects.link(obj)
    for existing_collection in list(obj.users_collection):
        if existing_collection != collection:
            existing_collection.objects.unlink(obj)


def get_import_files_in_directory(directory, recursive=False):
    supported = {MODEL_EXT, SKELETON_EXT, ANIMATION_EXT}
    results = []
    if recursive:
        for root, _, files in os.walk(directory):
            for file_name in files:
                if os.path.splitext(file_name)[1].lower() in supported:
                    results.append(os.path.join(root, file_name))
    else:
        for file_name in os.listdir(directory):
            file_path = os.path.join(directory, file_name)
            if os.path.isfile(file_path) and os.path.splitext(file_name)[1].lower() in supported:
                results.append(file_path)
    return sorted(results, key=lambda path: (os.path.splitext(path)[1].lower() != SKELETON_EXT,
                                            os.path.splitext(path)[1].lower() != MODEL_EXT,
                                            os.path.basename(path).lower()))


def find_best_armature_for_name(file_stem, armatures_by_stem):
    lower_stem = file_stem.lower()
    exact = armatures_by_stem.get(lower_stem)
    if exact:
        return exact
    prefix_matches = [(stem, armature) for stem, armature in armatures_by_stem.items() if lower_stem.startswith(stem)]
    if prefix_matches:
        prefix_matches.sort(key=lambda entry: len(entry[0]), reverse=True)
        return prefix_matches[0][1]
    if len(armatures_by_stem) == 1:
        return next(iter(armatures_by_stem.values()))
    return None


def import_directory(context, directory, recursive=False):
    files = get_import_files_in_directory(directory, recursive)
    if not files:
        raise ValueError("No .vtx, .bhe, or .bae files were found in '%s'." % directory)

    collection_name = os.path.basename(os.path.normpath(directory)) or "Great Quest Import"
    collection = bpy.data.collections.new(collection_name)
    context.scene.collection.children.link(collection)

    armatures_by_stem = {}
    meshes = []
    actions = []

    for file_path in files:
        if os.path.splitext(file_path)[1].lower() != SKELETON_EXT:
            continue
        armature = import_bhe(context, file_path)
        move_object_to_collection(armature, collection)
        armatures_by_stem[os.path.splitext(os.path.basename(file_path))[0].lower()] = armature

    for file_path in files:
        if os.path.splitext(file_path)[1].lower() != MODEL_EXT:
            continue
        mesh_obj = import_vtx(context, file_path)
        move_object_to_collection(mesh_obj, collection)
        meshes.append(mesh_obj)
        file_stem = os.path.splitext(os.path.basename(file_path))[0]
        bind_mesh_to_armature(mesh_obj, find_best_armature_for_name(file_stem, armatures_by_stem))

    for file_path in files:
        if os.path.splitext(file_path)[1].lower() != ANIMATION_EXT:
            continue
        file_stem = os.path.splitext(os.path.basename(file_path))[0]
        armature = find_best_armature_for_name(file_stem, armatures_by_stem)
        if armature:
            context.view_layer.objects.active = armature
            armature.select_set(True)
        action = import_bae(context, file_path, armature=armature, use_context_armature=False)
        actions.append(action)
        if armature:
            register_armature_action(armature, action)

    for armature in armatures_by_stem.values():
        action_names = [name for name in armature.get(ARMATURE_ACTIONS_PROP, []) if name in bpy.data.actions]
        visible_actions = [bpy.data.actions[name] for name in action_names if bpy.data.actions[name].fcurves]
        if visible_actions:
            set_active_greatquest_action(context, armature, visible_actions[0])
        elif action_names:
            set_active_greatquest_action(context, armature, bpy.data.actions[action_names[0]])

    if armatures_by_stem:
        for obj in context.scene.objects:
            obj.select_set(False)
        first_armature = next(iter(armatures_by_stem.values()))
        first_armature.select_set(True)
        context.view_layer.objects.active = first_armature

    return {
        "files": files,
        "collection": collection,
        "armatures": list(armatures_by_stem.values()),
        "meshes": meshes,
        "actions": actions,
    }


def import_related_files(context, filepath):
    imported = []
    ext = os.path.splitext(filepath)[1].lower()
    if ext == MODEL_EXT:
        mesh_obj = import_vtx(context, filepath)
        imported.append(mesh_obj)
        stem = os.path.splitext(filepath)[0]
        candidate = stem + SKELETON_EXT
        if os.path.exists(candidate):
            armature_obj = import_bhe(context, candidate)
            imported.append(armature_obj)
            bind_mesh_to_armature(mesh_obj, armature_obj)
    elif ext == SKELETON_EXT:
        imported.append(import_bhe(context, filepath))
    elif ext == ANIMATION_EXT:
        armature = context.object if context.object and context.object.type == "ARMATURE" else None
        action = import_bae(context, filepath, armature=armature, use_context_armature=True)
        imported.append(action)
        if armature:
            register_armature_action(armature, action)
            set_active_greatquest_action(context, armature, action)
    else:
        raise ValueError("Unsupported Great Quest model file extension: %s" % ext)
    return imported


class ImportGreatQuestModelOperator(bpy.types.Operator, ImportHelper):
    """Import Great Quest .vtx, .bhe, or .bae files"""
    bl_idname = GAME_IDENTIFIER + ".import_model"
    bl_label = "Import Great Quest Model (.vtx/.bhe/.bae)"
    filename_ext = MODEL_EXT
    filter_glob: bpy.props.StringProperty(default="*.vtx;*.bhe;*.bae", options={"HIDDEN"}, maxlen=255)

    def execute(self, context):
        context.scene[IMPORTING_PROP] = True
        try:
            import_related_files(context, self.filepath)
        except Exception as ex:
            self.report({"ERROR"}, str(ex))
            return {"CANCELLED"}
        finally:
            context.scene[IMPORTING_PROP] = False
        self.report({"INFO"}, "Imported Great Quest file.")
        return {"FINISHED"}


class ImportGreatQuestDirectoryOperator(bpy.types.Operator):
    """Import all Great Quest model files in a directory"""
    bl_idname = GAME_IDENTIFIER + ".import_model_directory"
    bl_label = "Import Great Quest Model Directory"

    directory: bpy.props.StringProperty(name="Directory", subtype="DIR_PATH")
    recursive: bpy.props.BoolProperty(name="Recursive", description="Import supported files from subdirectories too", default=False)

    def invoke(self, context, event):
        context.window_manager.fileselect_add(self)
        return {"RUNNING_MODAL"}

    def execute(self, context):
        context.scene[IMPORTING_PROP] = True
        try:
            result = import_directory(context, self.directory, self.recursive)
        except Exception as ex:
            self.report({"ERROR"}, str(ex))
            return {"CANCELLED"}
        finally:
            context.scene[IMPORTING_PROP] = False
        self.report({"INFO"}, "Imported %d Great Quest file(s): %d mesh(es), %d skeleton(s), %d animation(s)." %
                    (len(result["files"]), len(result["meshes"]), len(result["armatures"]), len(result["actions"])))
        return {"FINISHED"}


def get_selected_greatquest_armature(context):
    obj = context.object
    if obj and obj.type == "ARMATURE":
        return obj
    if obj and obj.type == "MESH":
        for modifier in obj.modifiers:
            if modifier.type == "ARMATURE" and modifier.object:
                return modifier.object
    return None


def enumerate_greatquest_actions_for_armature(armature):
    names = []
    if armature:
        names.extend([name for name in armature.get(ARMATURE_ACTIONS_PROP, []) if name in bpy.data.actions])
    for action in bpy.data.actions:
        if get_blob(action, ACTION_PROP) and action.name not in names:
            names.append(action.name)
    return names


def enumerate_export_actions_for_armature(armature):
    names = []
    if not armature:
        return names
    for name in armature.get(ARMATURE_ACTIONS_PROP, []):
        action = bpy.data.actions.get(name)
        if action and get_blob(action, ACTION_PROP) and name not in names:
            names.append(name)
    if armature.animation_data and armature.animation_data.action:
        action = armature.animation_data.action
        if get_blob(action, ACTION_PROP) and action.name not in names:
            names.append(action.name)
    return names


def get_selected_greatquest_mesh(context):
    obj = context.object
    if obj and obj.type == "MESH" and get_blob(obj, MESH_PROP):
        return obj
    return None


def find_mesh_for_armature(context, armature):
    if not armature:
        return None
    for obj in context.scene.objects:
        if obj.type != "MESH" or not get_blob(obj, MESH_PROP):
            continue
        for modifier in obj.modifiers:
            if modifier.type == "ARMATURE" and modifier.object == armature:
                return obj
    return None


def safe_export_name(name, extension):
    base = os.path.splitext(os.path.basename(name or ""))[0]
    if base.endswith("_Armature"):
        base = base[:-len("_Armature")]
    invalid_chars = '<>:"/\\|?*'
    base = "".join("_" if ch in invalid_chars or ord(ch) < 32 else ch for ch in base).strip(" .")
    if not base:
        base = "GreatQuest"
    return base + extension


def uniquify_export_path(path, used_paths):
    directory = os.path.dirname(path)
    stem, ext = os.path.splitext(os.path.basename(path))
    candidate = path
    index = 2
    while os.path.normcase(os.path.abspath(candidate)) in used_paths:
        candidate = os.path.join(directory, "%s_%d%s" % (stem, index, ext))
        index += 1
    used_paths.add(os.path.normcase(os.path.abspath(candidate)))
    return candidate


def export_related_files_to_directory(context, directory):
    if not directory:
        raise ValueError("No export directory was selected.")
    os.makedirs(directory, exist_ok=True)

    mesh_obj = get_selected_greatquest_mesh(context)
    armature_obj = get_selected_greatquest_armature(context)
    if armature_obj and not mesh_obj:
        mesh_obj = find_mesh_for_armature(context, armature_obj)

    if not mesh_obj and not armature_obj:
        raise ValueError("Select a Great Quest mesh or armature before exporting a directory.")

    exported = {"files": [], "meshes": 0, "armatures": 0, "actions": 0}
    used_paths = set()
    if mesh_obj:
        model = get_blob(mesh_obj, MESH_PROP)
        model_name = safe_export_name(model.get("source_file") if model else mesh_obj.name, MODEL_EXT)
        model_path = uniquify_export_path(os.path.join(directory, model_name), used_paths)
        export_vtx_object(mesh_obj, model_path)
        exported["files"].append(model_path)
        exported["meshes"] += 1

    if armature_obj:
        skeleton_name = safe_export_name(armature_obj.name, SKELETON_EXT)
        skeleton_path = uniquify_export_path(os.path.join(directory, skeleton_name), used_paths)
        export_bhe_object(armature_obj, skeleton_path)
        exported["files"].append(skeleton_path)
        exported["armatures"] += 1

        for action_name in enumerate_export_actions_for_armature(armature_obj):
            action = bpy.data.actions.get(action_name)
            if not action or not get_blob(action, ACTION_PROP):
                continue
            action_path = uniquify_export_path(os.path.join(directory, safe_export_name(action.name, ANIMATION_EXT)), used_paths)
            export_bae_action(action, armature_obj, action_path)
            exported["files"].append(action_path)
            exported["actions"] += 1

    return exported


def greatquest_action_items(self, context):
    armature = get_selected_greatquest_armature(context)
    names = enumerate_greatquest_actions_for_armature(armature)
    if not names:
        return [("__NONE__", "No Great Quest animations", "No imported Great Quest .bae actions were found")]
    return [(name, name, "Preview/edit %s" % name) for name in names]


def sync_selected_greatquest_action_range(context, reset_frame=False):
    armature = get_selected_greatquest_armature(context)
    if not armature or not armature.animation_data:
        return
    action = armature.animation_data.action
    if not action or ACTION_PROP not in action:
        return
    start, end = get_action_frame_range(action)
    cache_key = "%s|%s|%d|%d" % (armature.name, action.name, start, end)
    if (context.scene.get(LAST_ACTIVE_ACTION_PROP) == cache_key
            and context.scene.frame_start == start
            and context.scene.frame_end == end
            and not context.scene.use_preview_range):
        return
    set_action_display_range(action, start, end)
    set_scene_playback_range(context, start, end, reset_frame=reset_frame)
    context.scene[LAST_ACTIVE_ACTION_PROP] = cache_key


class GreatQuestSelectActionOperator(bpy.types.Operator):
    """Assign a Great Quest animation action to the selected armature"""
    bl_idname = GAME_IDENTIFIER + ".select_action"
    bl_label = "Preview Great Quest Animation"
    bl_property = "action_name"

    action_name: bpy.props.EnumProperty(name="Animation", items=greatquest_action_items)

    def invoke(self, context, event):
        context.window_manager.invoke_search_popup(self)
        return {"RUNNING_MODAL"}

    def execute(self, context):
        if self.action_name == "__NONE__":
            return {"CANCELLED"}
        armature = get_selected_greatquest_armature(context)
        if not armature:
            self.report({"ERROR"}, "Select a Great Quest armature or a mesh bound to one.")
            return {"CANCELLED"}
        action = bpy.data.actions.get(self.action_name)
        if not action:
            self.report({"ERROR"}, "Could not find action '%s'." % self.action_name)
            return {"CANCELLED"}
        set_active_greatquest_action(context, armature, action)
        self.report({"INFO"}, "Selected animation '%s'." % action.name)
        return {"FINISHED"}


class GreatQuestModelPanel(bpy.types.Panel):
    bl_label = "Great Quest Model"
    bl_idname = "VIEW3D_PT_greatquest_model"
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "Great Quest"

    def draw(self, context):
        layout = self.layout
        armature = get_selected_greatquest_armature(context)
        if not armature:
            layout.label(text="Select a Great Quest armature or mesh.")
            return
        sync_selected_greatquest_action_range(context, reset_frame=False)
        layout.label(text=armature.name)
        action = armature.animation_data.action if armature.animation_data else None
        layout.label(text="Active: %s" % (action.name if action else "None"))
        action_names = enumerate_greatquest_actions_for_armature(armature)
        if not action_names:
            layout.label(text="No imported .bae animations found.")
            return

        layout.operator(GreatQuestSelectActionOperator.bl_idname, text="Search Animations")
        layout.label(text="%d imported animation(s)" % len(action_names))
        for action_name in action_names:
            row = layout.row()
            row.enabled = action_name in bpy.data.actions
            op = row.operator(GreatQuestSelectActionOperator.bl_idname, text=action_name)
            op.action_name = action_name


class ExportGreatQuestModelOperator(bpy.types.Operator, ExportHelper):
    """Export selected Great Quest object/action"""
    bl_idname = GAME_IDENTIFIER + ".export_model"
    bl_label = "Export Great Quest Model (.vtx/.bhe/.bae)"
    filename_ext = MODEL_EXT
    filter_glob: bpy.props.StringProperty(default="*.vtx;*.bhe;*.bae", options={"HIDDEN"}, maxlen=255)

    def execute(self, context):
        try:
            ext = os.path.splitext(self.filepath)[1].lower()
            if ext == MODEL_EXT:
                export_vtx(context, self.filepath)
            elif ext == SKELETON_EXT:
                export_bhe(context, self.filepath)
            elif ext == ANIMATION_EXT:
                export_bae(context, self.filepath)
            else:
                raise ValueError("Export path must end with .vtx, .bhe, or .bae.")
        except Exception as ex:
            self.report({"ERROR"}, str(ex))
            return {"CANCELLED"}
        self.report({"INFO"}, "Exported Great Quest file.")
        return {"FINISHED"}


class ExportGreatQuestDirectoryOperator(bpy.types.Operator):
    """Export the selected Great Quest mesh, skeleton, and animations to a directory"""
    bl_idname = GAME_IDENTIFIER + ".export_model_directory"
    bl_label = "Export Great Quest Model Directory"

    directory: bpy.props.StringProperty(name="Directory", subtype="DIR_PATH")

    def invoke(self, context, event):
        context.window_manager.fileselect_add(self)
        return {"RUNNING_MODAL"}

    def execute(self, context):
        try:
            result = export_related_files_to_directory(context, self.directory)
        except Exception as ex:
            self.report({"ERROR"}, str(ex))
            return {"CANCELLED"}
        self.report({"INFO"}, "Exported %d Great Quest file(s): %d mesh(es), %d skeleton(s), %d animation(s)." %
                    (len(result["files"]), result["meshes"], result["armatures"], result["actions"]))
        return {"FINISHED"}


def menu_func_import(self, context):
    self.layout.operator(ImportGreatQuestModelOperator.bl_idname, text=ImportGreatQuestModelOperator.bl_label)
    self.layout.operator(ImportGreatQuestDirectoryOperator.bl_idname, text=ImportGreatQuestDirectoryOperator.bl_label)


def menu_func_export(self, context):
    self.layout.operator(ExportGreatQuestModelOperator.bl_idname, text=ExportGreatQuestModelOperator.bl_label)
    self.layout.operator(ExportGreatQuestDirectoryOperator.bl_idname, text=ExportGreatQuestDirectoryOperator.bl_label)


def register():
    bpy.utils.register_class(ImportGreatQuestModelOperator)
    bpy.utils.register_class(ImportGreatQuestDirectoryOperator)
    bpy.utils.register_class(GreatQuestSelectActionOperator)
    bpy.utils.register_class(GreatQuestModelPanel)
    bpy.utils.register_class(ExportGreatQuestModelOperator)
    bpy.utils.register_class(ExportGreatQuestDirectoryOperator)
    bpy.types.TOPBAR_MT_file_import.append(menu_func_import)
    bpy.types.TOPBAR_MT_file_export.append(menu_func_export)
    if greatquest_active_action_range_handler not in bpy.app.handlers.depsgraph_update_post:
        bpy.app.handlers.depsgraph_update_post.append(greatquest_active_action_range_handler)


def unregister():
    if greatquest_active_action_range_handler in bpy.app.handlers.depsgraph_update_post:
        bpy.app.handlers.depsgraph_update_post.remove(greatquest_active_action_range_handler)
    bpy.types.TOPBAR_MT_file_import.remove(menu_func_import)
    bpy.types.TOPBAR_MT_file_export.remove(menu_func_export)
    bpy.utils.unregister_class(ExportGreatQuestDirectoryOperator)
    bpy.utils.unregister_class(ExportGreatQuestModelOperator)
    bpy.utils.unregister_class(GreatQuestModelPanel)
    bpy.utils.unregister_class(GreatQuestSelectActionOperator)
    bpy.utils.unregister_class(ImportGreatQuestDirectoryOperator)
    bpy.utils.unregister_class(ImportGreatQuestModelOperator)


if __name__ == "__main__":
    register()
