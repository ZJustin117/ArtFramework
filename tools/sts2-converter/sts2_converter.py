"""Restricted, source-preserving Godot .tscn to ART VFX bundle converter."""

import argparse
import hashlib
import json
import os
import re
import shutil
from pathlib import Path, PurePosixPath


FORMAT = "art.sts2-vfx-bundle"
SCHEMA_VERSION = 1
SUPPORTED_NODES = {"Node2D", "Sprite2D", "GPUParticles2D", "CPUParticles2D"}
ALLOWED_RESOURCE_EXTENSIONS = {".png", ".atlas"}
KNOWN_UNSUPPORTED_TYPES = {
    "ShaderMaterial", "BackBufferCopy", "CollisionPolygon2D", "GPUParticlesCollision2D",
    "Tween", "AnimationPlayer",
}
KNOWN_UNSUPPORTED_PROPERTIES = {
    "script", "shader", "turbulence_enabled", "collision_type", "sub_emitter",
    "tween", "process_callback", "trail_enabled", "draw_order",
}
KNOWN_UNSUPPORTED_PROPERTY_PREFIXES = (
    "turbulence_", "angle_", "radial_velocity_", "emission_ring_",
    "emission_shape_", "attractor_interaction_", "hue_variation_",
)


class ConversionError(ValueError):
    pass


def _provenance(logical_path, section, line, prop=None):
    value = {"source": logical_path, "section": section, "line": line}
    if prop is not None:
        value["property"] = prop
    return value


def _split_top_level(text, separator=","):
    parts = []
    start = 0
    depth = 0
    quote = None
    escaped = False
    for index, char in enumerate(text):
        if quote:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == quote:
                quote = None
        elif char in "\"'":
            quote = char
        elif char in "([{":
            depth += 1
        elif char in ")]}":
            depth -= 1
        elif char == separator and depth == 0:
            parts.append(text[start:index].strip())
            start = index + 1
    parts.append(text[start:].strip())
    return parts


def _balanced(text):
    depth = 0
    quote = None
    escaped = False
    for char in text:
        if quote:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == quote:
                quote = None
        elif char in "\"'":
            quote = char
        elif char in "([{":
            depth += 1
        elif char in ")]}":
            depth -= 1
            if depth < 0:
                return False
    return depth == 0 and quote is None


def _json_string(text):
    try:
        return json.loads(text)
    except json.JSONDecodeError as exc:
        raise ConversionError("invalid string: {}".format(exc.msg))


def parse_value(text):
    """Parse the deliberately restricted Godot value subset into JSON values."""
    text = text.strip()
    if not text:
        raise ConversionError("empty value")
    if text.startswith('"'):
        return _json_string(text)
    if text == "true":
        return True
    if text == "false":
        return False
    if text in ("null", "Nil"):
        return None
    if re.fullmatch(r"[-+]?\d+", text):
        return int(text)
    if re.fullmatch(r"[-+]?(?:\d+\.\d*|\d*\.\d+|\d+)(?:[eE][-+]?\d+)?", text):
        return float(text)
    if text.startswith("[") and text.endswith("]"):
        inner = text[1:-1].strip()
        values = [] if not inner else [parse_value(part) for part in _split_top_level(inner)]
        return {"type": "Array", "values": values}

    match = re.fullmatch(r"([A-Za-z_][A-Za-z0-9_]*)\s*\((.*)\)", text, re.DOTALL)
    if not match:
        raise ConversionError("unrecognized value syntax")
    name = match.group(1)
    inner = match.group(2).strip()
    args = [] if not inner else [parse_value(part) for part in _split_top_level(inner)]
    if name in ("ExtResource", "SubResource"):
        if len(args) != 1 or not isinstance(args[0], (str, int)):
            raise ConversionError("{} requires one string or integer id".format(name))
        return {"ref": name, "id": str(args[0])}
    if name == "Vector2" and len(args) == 2 and all(_is_number(v) for v in args):
        return {"type": "Vec2", "x": args[0], "y": args[1]}
    if name == "Vector3" and len(args) == 3 and all(_is_number(v) for v in args):
        return {"type": "Vec3", "x": args[0], "y": args[1], "z": args[2]}
    if name == "Color" and len(args) in (3, 4) and all(_is_number(v) for v in args):
        rgba = args + [1] if len(args) == 3 else args
        return {"type": "Color", "r": rgba[0], "g": rgba[1], "b": rgba[2], "a": rgba[3]}
    if name == "Transform2D" and len(args) == 6 and all(_is_number(v) for v in args):
        return {
            "type": "Transform2D", "xx": args[0], "xy": args[1], "yx": args[2],
            "yy": args[3], "origin": {"type": "Vec2", "x": args[4], "y": args[5]},
        }
    packed = {
        "PackedFloat32Array": "Float32", "PackedFloat64Array": "Float64",
        "PackedInt32Array": "Int32", "PackedInt64Array": "Int64",
        "PackedColorArray": "Color",
    }
    if name in packed:
        if packed[name] == "Color" and all(_is_number(v) for v in args):
            if len(args) % 4:
                raise ConversionError("PackedColorArray flat values must be RGBA groups of four")
            args = [
                {"type": "Color", "r": args[index], "g": args[index + 1],
                 "b": args[index + 2], "a": args[index + 3]}
                for index in range(0, len(args), 4)
            ]
        valid = all(isinstance(v, dict) and v.get("type") == "Color" for v in args) \
            if packed[name] == "Color" else all(_is_number(v) for v in args)
        if not valid:
            raise ConversionError("{} contains an invalid element".format(name))
        return {"type": "PackedArray", "elementType": packed[name], "values": args}
    return {"opaque": {"kind": "constructor", "name": name, "args": args}}


def _is_number(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool)


def _parse_header(header):
    pieces = _split_header_fields(header.strip()[1:-1].strip())
    kind = pieces.pop(0)
    attrs = {}
    for piece in pieces:
        if "=" not in piece:
            attrs[piece] = True
            continue
        key, raw = piece.split("=", 1)
        attrs[key] = parse_value(raw)
    return kind, attrs


def _split_header_fields(text):
    fields = []
    start = 0
    quote = None
    escaped = False
    for index, char in enumerate(text):
        if quote:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == quote:
                quote = None
        elif char in "\"'":
            quote = char
        elif char.isspace():
            if start < index:
                fields.append(text[start:index])
            start = index + 1
    if start < len(text):
        fields.append(text[start:])
    return fields


def parse_tscn(text, logical_path):
    sections = []
    diagnostics = []
    current = None
    lines = text.splitlines()
    index = 0
    while index < len(lines):
        raw_line = lines[index]
        stripped = raw_line.strip()
        line_number = index + 1
        if not stripped or stripped.startswith(";"):
            index += 1
            continue
        if stripped.startswith("[") and stripped.endswith("]"):
            try:
                kind, attrs = _parse_header(stripped)
                current = {
                    "kind": kind, "attributes": attrs, "properties": [],
                    "provenance": _provenance(logical_path, kind, line_number),
                }
                sections.append(current)
            except ConversionError as exc:
                current = None
                diagnostics.append(_diagnostic(
                    "malformed", "ERROR", str(exc),
                    _provenance(logical_path, "header", line_number), raw=stripped,
                ))
            index += 1
            continue
        if current is None or "=" not in stripped:
            diagnostics.append(_diagnostic(
                "malformed", "ERROR", "content outside a section or assignment",
                _provenance(logical_path, current["kind"] if current else "document", line_number),
                raw=stripped,
            ))
            index += 1
            continue
        name, value_text = stripped.split("=", 1)
        name = name.strip()
        value_text = value_text.strip()
        end = index
        while not _balanced(value_text) and end + 1 < len(lines):
            next_line = lines[end + 1].strip()
            # An unterminated property must not swallow supported sibling sections.
            if next_line.startswith("[") and next_line.endswith("]"):
                break
            end += 1
            value_text += "\n" + lines[end].strip()
        provenance = _provenance(logical_path, current["kind"], line_number, name)
        try:
            value = parse_value(value_text)
            current["properties"].append({
                "name": name, "value": value, "raw": value_text,
                "provenance": provenance,
            })
        except ConversionError as exc:
            opaque = {"opaque": {"kind": "malformed", "raw": value_text}}
            current["properties"].append({
                "name": name, "value": opaque, "raw": value_text,
                "provenance": provenance,
            })
            diagnostics.append(_diagnostic("malformed", "ERROR", str(exc), provenance, raw=value_text))
        index = end + 1
    return {"sections": sections, "diagnostics": diagnostics}


def _diagnostic(case, severity, message, provenance, capability="DEGRADED", **extra):
    value = {
        "case": case, "severity": severity, "capability": capability,
        "message": message, "provenance": provenance,
    }
    value.update(extra)
    return value


def _properties(section):
    return {item["name"]: item for item in section["properties"]}


def _logical_path(source_path, source_root):
    source = Path(source_path).resolve()
    root = Path(source_root).resolve() if source_root else source.parent
    try:
        logical = source.relative_to(root).as_posix()
    except ValueError:
        logical = source.name
    return _safe_relative(logical, "logical source path")


def _safe_relative(value, label):
    value = value.replace("\\", "/")
    path = PurePosixPath(value)
    if not value or path.is_absolute() or any(part in ("", ".", "..") for part in path.parts):
        raise ConversionError("{} contains path traversal: {}".format(label, value))
    if re.match(r"^[A-Za-z]:", value):
        raise ConversionError("{} must not be absolute: {}".format(label, value))
    return path.as_posix()


def _safe_id(value, label):
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", value) or ".." in value:
        raise ConversionError("invalid {}: {}".format(label, value))
    return value


def _stable_id(logical_source, node_path):
    digest = hashlib.sha256((logical_source + "\n" + node_path).encode("utf-8")).hexdigest()
    return "node-" + digest[:16]


def _stable_seed(source_identity, node_id):
    digest = hashlib.sha256((source_identity + "\n" + node_id).encode("utf-8")).digest()
    return int.from_bytes(digest[:8], "big") & 0x7FFFFFFFFFFFFFFF


def _node_paths(node_sections):
    paths = []
    root_name = None
    for section in node_sections:
        attrs = section["attributes"]
        name = str(attrs.get("name", "unnamed"))
        parent = attrs.get("parent")
        if root_name is None:
            path = name
            root_name = name
        elif parent in (None, "."):
            path = root_name + "/" + name
        else:
            parent = str(parent).strip("/")
            if parent == root_name or parent.startswith(root_name + "/"):
                path = parent + "/" + name
            else:
                path = root_name + "/" + parent + "/" + name
        paths.append(path)
    return paths


NODE_FIELDS = {
    "position": "position", "rotation": "rotation", "rotation_degrees": "rotationDegrees",
    "scale": "scale", "transform": "transform", "visible": "visible",
    "modulate": "modulate", "self_modulate": "selfModulate", "z_index": "zIndex",
}
SPRITE_FIELDS = {
    "texture": "texture", "centered": "centered", "offset": "offset",
    "flip_h": "flipH", "flip_v": "flipV", "hframes": "hframes", "vframes": "vframes",
    "frame": "frame", "frame_coords": "frameCoords", "region_enabled": "regionEnabled",
    "material": "material",
}
PARTICLE_FIELDS = {
    "amount": "amount", "lifetime": "lifetime", "randomness": "lifetimeRandomness",
    "process_material": "processMaterial", "texture": "texture", "one_shot": "oneShot",
    "explosiveness": "explosiveness", "preprocess": "preprocess", "speed_scale": "speedScale",
    "fixed_fps": "fixedFps", "interpolate": "interpolate", "fract_delta": "fractionalDelta",
    "visibility_rect": "visibilityRect", "emitting": "emitting",
}
PROCESS_FIELDS = {
    "direction": "direction", "spread": "spreadDegrees", "gravity": "gravity",
    "initial_velocity_min": "initialVelocityMin", "initial_velocity_max": "initialVelocityMax",
    "scale_min": "scaleMin", "scale_max": "scaleMax",
    "angular_velocity_min": "angularVelocityMin", "angular_velocity_max": "angularVelocityMax",
    "scale_curve": "scaleCurve", "alpha_curve": "alphaCurve", "color_ramp": "colorRamp",
    "anim_speed_min": "animationSpeedMin", "anim_speed_max": "animationSpeedMax",
    "anim_offset_min": "animationOffsetMin", "anim_offset_max": "animationOffsetMax",
    "particle_flag_disable_z": "disableZ", "emission_shape": "emissionShape",
    "emission_sphere_radius": "emissionSphereRadius", "color": "color",
    "lifetime_randomness": "lifetimeRandomness",
}


def _valid_node_field(name, value):
    if name in ("position", "scale"):
        return isinstance(value, dict) and value.get("type") == "Vec2"
    if name == "transform":
        return isinstance(value, dict) and value.get("type") == "Transform2D"
    if name in ("modulate", "self_modulate"):
        return isinstance(value, dict) and value.get("type") == "Color"
    if name == "visible":
        return isinstance(value, bool)
    return _is_number(value)


def _range(target, minimum, maximum):
    if minimum in target or maximum in target:
        return {"min": target.pop(minimum, None), "max": target.pop(maximum, None)}
    return None


def _resolve_ref(value, expected, subresources):
    if not isinstance(value, dict) or value.get("ref") != "SubResource":
        return None
    section = subresources.get(value["id"])
    if section is None or section["attributes"].get("type") != expected:
        return None
    return section


def _resource_provenance(section):
    return {
        "id": str(section["attributes"].get("id")),
        "type": section["attributes"].get("type"),
        "source": section["provenance"],
    }


def _resolve_wrapped_ref(value, terminal_type, wrapper_type, wrapper_property, subresources):
    if not isinstance(value, dict) or value.get("ref") != "SubResource":
        raise ConversionError("{} reference is not a SubResource".format(terminal_type))
    wrappers = []
    seen = set()
    current = value
    while True:
        resource_id = current["id"]
        if resource_id in seen:
            raise ConversionError("{} reference cycle at {}".format(terminal_type, resource_id))
        seen.add(resource_id)
        section = subresources.get(resource_id)
        if section is None:
            raise ConversionError("{} reference is missing: {}".format(terminal_type, resource_id))
        section_type = section["attributes"].get("type")
        if section_type == terminal_type:
            return section, wrappers
        if section_type != wrapper_type:
            raise ConversionError(
                "{} reference has wrong resource type: {}".format(terminal_type, section_type)
            )
        wrappers.append({
            "provenance": _resource_provenance(section),
            "source": {item["name"]: item["value"] for item in section["properties"]},
        })
        wrapped = _properties(section).get(wrapper_property)
        if wrapped is None:
            raise ConversionError("{} wrapper has no {}".format(wrapper_type, wrapper_property))
        current = wrapped["value"]
        if not isinstance(current, dict) or current.get("ref") != "SubResource":
            raise ConversionError("{} wrapper {} is invalid".format(wrapper_type, wrapper_property))


def _array_values(value, label):
    if not isinstance(value, dict) or value.get("type") != "Array":
        raise ConversionError("{} must be a bracket array".format(label))
    return value["values"]


def _curve(section, wrappers=None):
    props = _properties(section)
    result = {
        "source": {item["name"]: item["value"] for item in section["properties"]},
        "provenance": {
            "curve": _resource_provenance(section),
            "wrappers": wrappers or [],
        },
    }
    if "_data" in props:
        values = _array_values(props["_data"]["value"], "Curve _data")
        if len(values) % 5:
            raise ConversionError("Curve _data must contain groups of five")
        points = []
        for index in range(0, len(values), 5):
            position, left_tangent, right_tangent, left_mode, right_mode = values[index:index + 5]
            if not (isinstance(position, dict) and position.get("type") == "Vec2"):
                raise ConversionError("Curve _data point position/value must be Vector2")
            if not all(_is_number(value) for value in (left_tangent, right_tangent)):
                raise ConversionError("Curve _data tangents must be numeric")
            if not all(isinstance(value, int) and not isinstance(value, bool)
                       for value in (left_mode, right_mode)):
                raise ConversionError("Curve _data modes must be integers")
            points.append({
                "position": position["x"], "value": position["y"],
                "leftTangent": left_tangent, "rightTangent": right_tangent,
                "leftMode": left_mode, "rightMode": right_mode,
            })
        point_count = props.get("point_count", {}).get("value")
        if not isinstance(point_count, int) or isinstance(point_count, bool):
            raise ConversionError("Curve point_count must be an integer")
        if point_count != len(points):
            raise ConversionError("Curve point_count does not match _data")
        result["controlPoints"] = points
        result["pointCount"] = point_count
    elif "points" in props:
        result["controlPoints"] = props["points"]["value"]
    else:
        raise ConversionError("Curve has no point data")
    if "_limits" in props:
        limits = _array_values(props["_limits"]["value"], "Curve _limits")
        if len(limits) != 4 or not all(_is_number(value) for value in limits):
            raise ConversionError("Curve _limits must contain four numeric values")
        result["limits"] = limits
    if "min_value" in props or "max_value" in props:
        result["range"] = {
            "min": props.get("min_value", {}).get("value"),
            "max": props.get("max_value", {}).get("value"),
        }
    return result


def _gradient(section, wrappers=None):
    props = _properties(section)
    offsets = props.get("offsets", {}).get("value", {}).get("values", [])
    colors = props.get("colors", {}).get("value", {}).get("values", [])
    if ("offsets" not in props or "colors" not in props or not offsets
            or not all(_is_number(value) for value in offsets)
            or not all(isinstance(value, dict) and value.get("type") == "Color" for value in colors)
            or len(offsets) != len(colors)):
        raise ConversionError("Gradient offsets and colors must be valid equal-length arrays")
    return {
        "stops": [
            {"offset": offsets[index], "color": colors[index]}
            for index in range(min(len(offsets), len(colors)))
        ],
        "source": {item["name"]: item["value"] for item in section["properties"]},
        "provenance": {
            "gradient": _resource_provenance(section),
            "wrappers": wrappers or [],
        },
    }


def _convert_process_material(section, subresources, results, diagnostics):
    props = _properties(section)
    typed = {}
    for name, item in props.items():
        value = item["value"]
        if name in PROCESS_FIELDS:
            key = PROCESS_FIELDS[name]
            if name.endswith("_curve"):
                try:
                    curve, wrappers = _resolve_wrapped_ref(
                        value, "Curve", "CurveTexture", "curve", subresources
                    )
                    typed[key] = _curve(curve, wrappers)
                    results.append(_result(item, "supported"))
                except ConversionError as exc:
                    results.append(_result(item, "malformed"))
                    diagnostics.append(_diagnostic("malformed", "ERROR", str(exc), item["provenance"]))
            elif name == "color_ramp":
                try:
                    gradient, wrappers = _resolve_wrapped_ref(
                        value, "Gradient", "GradientTexture1D", "gradient", subresources
                    )
                    typed[key] = _gradient(gradient, wrappers)
                    results.append(_result(item, "supported"))
                except ConversionError as exc:
                    results.append(_result(item, "malformed"))
                    diagnostics.append(_diagnostic("malformed", "ERROR", str(exc), item["provenance"]))
            elif name in ("gravity", "direction") and isinstance(value, dict) and value.get("type") == "Vec3":
                typed[key] = {"type": "Vec2", "x": value["x"], "y": value["y"]}
                results.append(_result(item, "degraded", "z component is not executable in 2D"))
                diagnostics.append(_diagnostic(
                    "degraded", "WARNING", "{} z component retained only in source IR".format(name),
                    item["provenance"],
                ))
            elif not isinstance(value, dict) or "opaque" not in value:
                typed[key] = value
                results.append(_result(item, "supported"))
            else:
                results.append(_result(item, "malformed"))
                diagnostics.append(_diagnostic("malformed", "ERROR", "supported field has no valid typed value", item["provenance"]))
        else:
            _classify_opaque(item, results, diagnostics)
    initial_velocity = _range(typed, "initialVelocityMin", "initialVelocityMax")
    if initial_velocity:
        typed["initialVelocity"] = initial_velocity
    scale = _range(typed, "scaleMin", "scaleMax")
    if scale:
        typed["scale"] = scale
    angular = _range(typed, "angularVelocityMin", "angularVelocityMax")
    if angular:
        typed["angularVelocity"] = angular
    flipbook = {}
    for key in ("animationSpeedMin", "animationSpeedMax", "animationOffsetMin", "animationOffsetMax"):
        if key in typed:
            flipbook[key] = typed.pop(key)
    if flipbook:
        typed["flipbook"] = flipbook
    return typed


def _result(item, status, reason=None):
    value = {"property": item["name"], "status": status, "provenance": item["provenance"]}
    if reason:
        value["reason"] = reason
    return value


def _classify_opaque(item, results, diagnostics):
    name = item["name"]
    malformed = item["value"].get("opaque", {}).get("kind") == "malformed" \
        if isinstance(item["value"], dict) else False
    if malformed:
        case = "malformed"
        message = "unparseable property retained as opaque data"
    elif (name in KNOWN_UNSUPPORTED_PROPERTIES
          or name.startswith(KNOWN_UNSUPPORTED_PROPERTY_PREFIXES)):
        case = "unsupported-known"
        message = "known property has no runtime semantics"
    else:
        case = "unknown"
        message = "unrecognized property retained as opaque data"
    results.append(_result(item, case, message))
    diagnostics.append(_diagnostic(
        case, "ERROR" if malformed else "WARNING", message, item["provenance"]
    ))


def _material_blend(value, subresources, results, diagnostics, item):
    section = _resolve_ref(value, "CanvasItemMaterial", subresources)
    if section is None:
        return None
    props = _properties(section)
    blend = props.get("blend_mode")
    if blend is None:
        results.append(_result(item, "supported"))
        return None
    if blend and isinstance(blend["value"], int):
        results.append(_result(item, "supported"))
        return {0: "MIX", 1: "ADD", 2: "SUB", 3: "MUL", 4: "PREMULT_ALPHA"}.get(
            blend["value"], "UNKNOWN"
        )
    diagnostics.append(_diagnostic("malformed", "ERROR", "CanvasItemMaterial blend_mode is invalid", item["provenance"]))
    results.append(_result(item, "malformed"))
    return None


CANVAS_PARTICLE_FIELDS = {
    "particles_animation": "enabled",
    "particles_anim_h_frames": "hFrames",
    "particles_anim_v_frames": "vFrames",
    "particles_anim_loop": "loop",
}


def _convert_particle_canvas_material(value, subresources, results, diagnostics, item):
    section = _resolve_ref(value, "CanvasItemMaterial", subresources)
    if section is None:
        results.append(_result(item, "malformed"))
        diagnostics.append(_diagnostic(
            "malformed", "ERROR", "invalid CanvasItemMaterial reference", item["provenance"]
        ))
        return {}
    typed = {}
    flipbook = {}
    for name, material_item in _properties(section).items():
        value = material_item["value"]
        if name == "blend_mode" and isinstance(value, int) and not isinstance(value, bool):
            blend = {0: "MIX", 1: "ADD", 2: "SUB", 3: "MUL", 4: "PREMULT_ALPHA"}.get(value)
            if blend is None:
                results.append(_result(material_item, "malformed"))
                diagnostics.append(_diagnostic(
                    "malformed", "ERROR", "CanvasItemMaterial blend_mode is invalid",
                    material_item["provenance"],
                ))
            else:
                typed["blendMode"] = blend
                results.append(_result(material_item, "supported"))
        elif name in CANVAS_PARTICLE_FIELDS:
            valid = isinstance(value, bool) if name in ("particles_animation", "particles_anim_loop") \
                else isinstance(value, int) and not isinstance(value, bool) and value > 0
            if valid:
                flipbook[CANVAS_PARTICLE_FIELDS[name]] = value
                results.append(_result(material_item, "supported"))
            else:
                results.append(_result(material_item, "malformed"))
                diagnostics.append(_diagnostic(
                    "malformed", "ERROR", "invalid particle CanvasItemMaterial field",
                    material_item["provenance"],
                ))
        else:
            _classify_opaque(material_item, results, diagnostics)
    if flipbook:
        typed["flipbook"] = flipbook
    results.append(_result(item, "supported"))
    return typed


def _merge_particle_emitter(target, addition):
    flipbook = addition.pop("flipbook", None)
    target.update(addition)
    if flipbook:
        target.setdefault("flipbook", {}).update(flipbook)


def convert_document(parsed, logical_source, source_identity):
    diagnostics = list(parsed["diagnostics"])
    sections = parsed["sections"]
    for section in sections:
        section_type = section["attributes"].get("type")
        if section_type in KNOWN_UNSUPPORTED_TYPES and section["kind"] != "node":
            diagnostics.append(_diagnostic(
                "unsupported-known", "WARNING",
                "{} section is retained but has no runtime semantics".format(section_type),
                section["provenance"], capability="UNSUPPORTED",
            ))
    subresources = {
        str(section["attributes"].get("id")): section
        for section in sections if section["kind"] == "sub_resource"
    }
    node_sections = [section for section in sections if section["kind"] == "node"]
    node_paths = _node_paths(node_sections)
    ids_by_path = {path: _stable_id(logical_source, path) for path in node_paths}
    typed_nodes = []
    property_results = []
    for section, node_path in zip(node_sections, node_paths):
        attrs = section["attributes"]
        node_type = attrs.get("type", "Node")
        node_results = []
        property_results.append({"nodePath": node_path, "properties": node_results})
        if node_type not in SUPPORTED_NODES:
            case = "unsupported-known" if node_type in KNOWN_UNSUPPORTED_TYPES else "unknown"
            diagnostics.append(_diagnostic(
                case, "WARNING", "node type is not instantiated: {}".format(node_type),
                section["provenance"], capability="UNSUPPORTED", nodePath=node_path,
            ))
            for item in section["properties"]:
                node_results.append(_result(item, case))
            continue
        parent_path = node_path.rsplit("/", 1)[0] if "/" in node_path else None
        typed = {
            "id": ids_by_path[node_path], "nodePath": node_path, "parentId": ids_by_path.get(parent_path),
            "nodeType": node_type, "source": section["provenance"], "transform": {},
        }
        props = _properties(section)
        for name, item in props.items():
            value = item["value"]
            if name in NODE_FIELDS:
                if _valid_node_field(name, value):
                    typed["transform"][NODE_FIELDS[name]] = value
                    node_results.append(_result(item, "supported"))
                else:
                    node_results.append(_result(item, "malformed"))
                    diagnostics.append(_diagnostic("malformed", "ERROR", "invalid typed node field", item["provenance"]))
            elif node_type == "Sprite2D" and name in SPRITE_FIELDS:
                typed.setdefault("sprite", {})
                if name == "material":
                    blend = _material_blend(value, subresources, node_results, diagnostics, item)
                    if blend is not None:
                        typed["sprite"]["blendMode"] = blend
                elif not (isinstance(value, dict) and "opaque" in value):
                    typed["sprite"][SPRITE_FIELDS[name]] = value
                    node_results.append(_result(item, "supported"))
                else:
                    node_results.append(_result(item, "malformed"))
                    diagnostics.append(_diagnostic("malformed", "ERROR", "invalid sprite field", item["provenance"]))
            elif (node_type in ("GPUParticles2D", "CPUParticles2D")
                  and (name in PARTICLE_FIELDS or name == "material")):
                emitter = typed.setdefault("particleEmitter", {})
                if name == "process_material":
                    material = _resolve_ref(value, "ParticleProcessMaterial", subresources)
                    if material:
                        _merge_particle_emitter(
                            emitter,
                            _convert_process_material(
                                material, subresources, node_results, diagnostics
                            ),
                        )
                        node_results.append(_result(item, "supported"))
                    else:
                        node_results.append(_result(item, "malformed"))
                        diagnostics.append(_diagnostic("malformed", "ERROR", "invalid ParticleProcessMaterial reference", item["provenance"]))
                elif name == "material":
                    _merge_particle_emitter(
                        emitter,
                        _convert_particle_canvas_material(
                            value, subresources, node_results, diagnostics, item
                        ),
                    )
                elif not (isinstance(value, dict) and "opaque" in value):
                    emitter[PARTICLE_FIELDS[name]] = value
                    node_results.append(_result(item, "supported"))
                else:
                    node_results.append(_result(item, "malformed"))
                    diagnostics.append(_diagnostic("malformed", "ERROR", "invalid particle field", item["provenance"]))
            else:
                _classify_opaque(item, node_results, diagnostics)
        if node_type == "GPUParticles2D":
            diagnostics.append(_diagnostic(
                "degraded", "INFO", "GPU particles use the ART CPU baseline",
                section["provenance"], nodePath=node_path,
            ))
        if "particleEmitter" in typed:
            typed["particleEmitter"]["randomSeed"] = _stable_seed(source_identity, typed["id"])
        if not typed["transform"]:
            del typed["transform"]
        typed_nodes.append(typed)
    capability = "SUPPORTED" if not diagnostics else "DEGRADED"
    return typed_nodes, property_results, diagnostics, capability


def _resource_source(item_path, source_path, source_root):
    normalized = item_path
    if normalized.startswith("res://"):
        normalized = normalized[6:]
        base = Path(source_root).resolve() if source_root else Path(source_path).resolve().parent
    else:
        base = Path(source_path).resolve().parent
    relative = _safe_relative(normalized, "resource path")
    candidate = (base / Path(relative)).resolve()
    try:
        candidate.relative_to(base)
    except ValueError:
        raise ConversionError("resource path escapes source root: {}".format(item_path))
    return relative, candidate


def _collect_resources(parsed, source_path, source_root, output_root, logical_source):
    entries = []
    diagnostics = []
    seen_outputs = set()
    for section in parsed["sections"]:
        if section["kind"] != "ext_resource":
            continue
        attrs = section["attributes"]
        path = attrs.get("path")
        if not isinstance(path, str):
            diagnostics.append(_diagnostic("malformed", "ERROR", "ext_resource path is not a string", section["provenance"]))
            continue
        relative, candidate = _resource_source(path, source_path, source_root)
        suffix = candidate.suffix.lower()
        resource_id = str(attrs.get("id", ""))
        if suffix not in ALLOWED_RESOURCE_EXTENSIONS:
            diagnostics.append(_diagnostic(
                "unsupported-known", "WARNING", "resource type is retained but not copied",
                section["provenance"], capability="UNSUPPORTED", resourceId=resource_id,
            ))
            entries.append({
                "id": resource_id, "kind": "OTHER", "sourcePath": relative,
                "status": "unsupported-known",
            })
            continue
        output_relative = _safe_relative("resources/" + relative, "resource output path")
        if output_relative in seen_outputs:
            continue
        seen_outputs.add(output_relative)
        entry = {
            "id": resource_id, "kind": "TEXTURE" if suffix == ".png" else "ATLAS",
            "sourcePath": relative,
        }
        if candidate.is_file():
            destination = output_root / output_relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(str(candidate), str(destination))
            entry["status"] = "supported"
            entry["outputPath"] = output_relative
            entry["sha256"] = hashlib.sha256(candidate.read_bytes()).hexdigest()
        else:
            entry["status"] = "missing-resource"
            diagnostics.append(_diagnostic(
                "missing-resource", "WARNING", "allowed resource was not found",
                section["provenance"], resourceId=resource_id, sourcePath=relative,
            ))
        entries.append(entry)
    return sorted(entries, key=lambda item: (item["id"], item.get("sourcePath", ""))), diagnostics


def _write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True, ensure_ascii=True) + "\n", encoding="utf-8")


def _opaque_properties(parsed, property_results):
    source_properties = {}
    for section in parsed["sections"]:
        for item in section["properties"]:
            provenance = item["provenance"]
            key = (provenance["section"], provenance["line"], item["name"])
            source_properties[key] = item
    opaque = []
    for node in property_results:
        for result in node["properties"]:
            if result["status"] not in ("unsupported-known", "unknown", "malformed"):
                continue
            provenance = result["provenance"]
            key = (provenance["section"], provenance["line"], result["property"])
            item = source_properties.get(key)
            if item:
                opaque.append({
                    "nodePath": node["nodePath"], "property": item["name"],
                    "case": result["status"], "value": item["value"], "raw": item["raw"],
                    "provenance": provenance, "nonExecutable": True,
                })
    return opaque


def convert(source, output_dir, bundle_id, scene_id=None, source_root=None):
    bundle_id = _safe_id(bundle_id, "bundle id")
    scene_id = _safe_id(scene_id or Path(source).stem, "scene id")
    source_path = Path(source)
    if not source_path.is_file():
        raise ConversionError("source scene does not exist: {}".format(source_path.name))
    output_root = Path(output_dir).resolve() / bundle_id
    output_root.mkdir(parents=True, exist_ok=True)
    logical_source = _logical_path(source_path, source_root)
    text = source_path.read_text(encoding="utf-8")
    source_identity = hashlib.sha256(text.encode("utf-8")).hexdigest()
    parsed = parse_tscn(text, logical_source)
    typed_nodes, property_results, diagnostics, capability = convert_document(
        parsed, logical_source, source_identity
    )
    resources, resource_diagnostics = _collect_resources(
        parsed, source_path, source_root, output_root, logical_source
    )
    diagnostics.extend(resource_diagnostics)
    if diagnostics:
        capability = "DEGRADED"
    scene_path = "scenes/{}.json".format(scene_id)
    scene = {
        "format": "art.sts2-vfx-scene", "schemaVersion": SCHEMA_VERSION,
        "id": scene_id, "source": {"path": logical_source, "sha256": source_identity},
        "capability": capability, "sourceText": text,
        "sourceIr": parsed["sections"], "propertyResults": property_results,
        "opaqueProperties": _opaque_properties(parsed, property_results),
        "typedNodes": typed_nodes, "resources": resources,
    }
    diagnostics_doc = {
        "format": "art.sts2-vfx-diagnostics", "schemaVersion": SCHEMA_VERSION,
        "bundleId": bundle_id, "items": diagnostics,
    }
    manifest = {
        "format": FORMAT, "schemaVersion": SCHEMA_VERSION, "bundleId": bundle_id,
        "source": {"path": logical_source, "sha256": source_identity},
        "capability": capability,
        "scenes": [{"id": scene_id, "path": scene_path, "capability": capability}],
        "resources": resources, "diagnostics": "diagnostics.json",
    }
    _write_json(output_root / scene_path, scene)
    _write_json(output_root / "diagnostics.json", diagnostics_doc)
    _write_json(output_root / "manifest.json", manifest)
    return manifest


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", help="source .tscn")
    parser.add_argument("output", help="directory that will contain the bundle")
    parser.add_argument("--bundle-id", required=True)
    parser.add_argument("--scene-id")
    parser.add_argument("--source-root")
    args = parser.parse_args(argv)
    try:
        convert(args.source, args.output, args.bundle_id, args.scene_id, args.source_root)
    except (ConversionError, OSError) as exc:
        parser.error(str(exc))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
