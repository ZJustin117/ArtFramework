import json
import hashlib
import os
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
TOOL = ROOT / "tools" / "sts2-converter"
FIXTURES = Path(__file__).resolve().parent / "fixtures"
sys.path.insert(0, str(TOOL))

import sts2_converter as converter


class ValueParsingTest(unittest.TestCase):
    def test_scalars_vectors_colors_arrays_refs_and_opaque_constructor(self):
        self.assertEqual(True, converter.parse_value("true"))
        self.assertIsNone(converter.parse_value("null"))
        self.assertEqual(-2, converter.parse_value("-2"))
        self.assertEqual(1.5, converter.parse_value("1.5"))
        self.assertEqual({"type": "Vec2", "x": 1, "y": 2}, converter.parse_value("Vector2(1, 2)"))
        self.assertEqual("Color", converter.parse_value("Color(1, 0, 0)")["type"])
        self.assertEqual("ExtResource", converter.parse_value('ExtResource("x")')["ref"])
        packed = converter.parse_value("PackedFloat32Array(1, 2.5)")
        self.assertEqual([1, 2.5], packed["values"])
        array = converter.parse_value("[Vector2(1, 2), [3, Color(0.1, 0.2, 0.3, 0.4)]]")
        self.assertEqual("Array", array["type"])
        self.assertEqual(2, array["values"][0]["y"])
        self.assertEqual(0.4, array["values"][1]["values"][1]["a"])
        colors = converter.parse_value("PackedColorArray(1, 0.5, 0.25, 1, 0.1, 0.2, 0.3, 0)")
        self.assertEqual(2, len(colors["values"]))
        self.assertEqual(0.2, colors["values"][1]["g"])
        with self.assertRaises(converter.ConversionError):
            converter.parse_value("PackedColorArray(1, 0.5, 0.25)")
        with self.assertRaises(converter.ConversionError):
            converter.parse_value('PackedColorArray(1, 0.5, "bad", 1)')
        opaque = converter.parse_value("FutureThing(1, Vector3(2, 3, 4))")
        self.assertEqual("FutureThing", opaque["opaque"]["name"])

    def test_multiline_sections_references_and_malformed_retention(self):
        text = FIXTURES.joinpath("synthetic_vfx.tscn").read_text(encoding="utf-8")
        parsed = converter.parse_tscn(text, "fixtures/synthetic_vfx.tscn")
        self.assertEqual("gd_scene", parsed["sections"][0]["kind"])
        curve = next(s for s in parsed["sections"] if s["attributes"].get("id") == "Curve_scale")
        points = next(p for p in curve["properties"] if p["name"] == "_data")
        self.assertEqual(10, len(points["value"]["values"]))
        limits = next(p for p in curve["properties"] if p["name"] == "_limits")
        self.assertEqual([0.0, 2.0, 0.0, 1.5], limits["value"]["values"])
        malformed = [d for d in parsed["diagnostics"] if d["case"] == "malformed"]
        self.assertEqual(1, len(malformed))
        particle = next(s for s in parsed["sections"] if s["attributes"].get("name") == "Particles")
        broken = next(p for p in particle["properties"] if p["name"] == "broken_semantic")
        self.assertEqual("malformed", broken["value"]["opaque"]["kind"])

    def test_invalid_wrappers_and_curve_shapes_are_rejected(self):
        parsed = converter.parse_tscn(
            '\n'.join((
                '[gd_scene format=3]',
                '[sub_resource type="Curve" id="Curve_bad"]',
                '_data = [Vector2(0, 1), 0.0, 0.0, 0, 0]',
                'point_count = 2',
                '[sub_resource type="CurveTexture" id="Cycle_a"]',
                'curve = SubResource("Cycle_b")',
                '[sub_resource type="CurveTexture" id="Cycle_b"]',
                'curve = SubResource("Cycle_a")',
            )),
            "fixtures/invalid_refs.tscn",
        )
        resources = {
            section["attributes"]["id"]: section
            for section in parsed["sections"] if section["kind"] == "sub_resource"
        }
        with self.assertRaisesRegex(converter.ConversionError, "point_count"):
            converter._curve(resources["Curve_bad"])
        with self.assertRaisesRegex(converter.ConversionError, "cycle"):
            converter._resolve_wrapped_ref(
                {"ref": "SubResource", "id": "Cycle_a"},
                "Curve", "CurveTexture", "curve", resources,
            )
        with self.assertRaisesRegex(converter.ConversionError, "missing"):
            converter._resolve_wrapped_ref(
                {"ref": "SubResource", "id": "Absent"},
                "Curve", "CurveTexture", "curve", resources,
            )
        with self.assertRaisesRegex(converter.ConversionError, "wrong resource type"):
            converter._resolve_wrapped_ref(
                {"ref": "SubResource", "id": "Curve_bad"},
                "Gradient", "GradientTexture1D", "gradient", resources,
            )


class ConverterTest(unittest.TestCase):
    def setUp(self):
        self.temp = Path(tempfile.mkdtemp())

    def tearDown(self):
        shutil.rmtree(str(self.temp))

    def convert(self, bundle="fixture"):
        return converter.convert(
            FIXTURES / "synthetic_vfx.tscn", self.temp, bundle,
            source_root=ROOT,
        )

    def load(self, bundle, relative):
        return json.loads((self.temp / bundle / relative).read_text(encoding="utf-8"))

    def test_hierarchy_typed_conversion_curves_gradients_and_seed(self):
        self.convert()
        scene = self.load("fixture", "scenes/synthetic_vfx.json")
        self.assertEqual(1.25, scene["duration"])
        nodes = {node["nodePath"]: node for node in scene["typedNodes"]}
        self.assertIn("Smoke/Sprite", nodes)
        self.assertEqual(nodes["Smoke"]["id"], nodes["Smoke/Sprite"]["parentId"])
        self.assertEqual("ADD", nodes["Smoke/Sprite"]["sprite"]["blendMode"])
        emitter = nodes["Smoke/Particles"]["particleEmitter"]
        self.assertEqual({"min": 20.0, "max": 40.0}, emitter["initialVelocity"])
        self.assertEqual(2, emitter["scaleCurve"]["pointCount"])
        self.assertEqual(0.25, emitter["scaleCurve"]["controlPoints"][0]["value"])
        self.assertEqual(-0.25, emitter["scaleCurve"]["controlPoints"][1]["leftTangent"])
        self.assertEqual([0.0, 2.0, 0.0, 1.5], emitter["scaleCurve"]["limits"])
        self.assertEqual("CurveTexture", emitter["scaleCurve"]["provenance"]["wrappers"][0]["provenance"]["type"])
        self.assertEqual(2, len(emitter["colorRamp"]["stops"]))
        self.assertEqual(0.2, emitter["colorRamp"]["stops"][1]["color"]["g"])
        self.assertEqual("GradientTexture1D", emitter["colorRamp"]["provenance"]["wrappers"][0]["provenance"]["type"])
        self.assertEqual(0.35, emitter["lifetimeRandomness"])
        self.assertEqual(2, emitter["flipbook"]["hFrames"])
        self.assertEqual(2, emitter["flipbook"]["vFrames"])
        self.assertFalse(emitter["flipbook"]["loop"])
        self.assertEqual(4.0, emitter["flipbook"]["animationSpeedMin"])
        self.assertIsInstance(emitter["randomSeed"], int)
        self.assertIn("Smoke/CpuSibling", nodes)
        self.assertNotIn("Smoke/ShaderOnly", nodes)

    def test_source_ir_opaque_retention_and_diagnostic_distinctions(self):
        self.convert()
        scene = self.load("fixture", "scenes/synthetic_vfx.json")
        diagnostics = self.load("fixture", "diagnostics.json")["items"]
        cases = {item["case"] for item in diagnostics}
        self.assertTrue({"degraded", "missing-resource", "unsupported-known", "unknown", "malformed"}.issubset(cases))
        statuses = {
            prop["status"]
            for node in scene["propertyResults"] for prop in node["properties"]
        }
        self.assertTrue({"supported", "unsupported-known", "unknown", "malformed"}.issubset(statuses))
        opaque = {item["property"]: item for item in scene["opaqueProperties"]}
        self.assertTrue(opaque["script"]["nonExecutable"])
        self.assertEqual("malformed", opaque["broken_semantic"]["case"])
        for name in (
            "turbulence_enabled", "turbulence_noise_strength", "angle_min",
            "radial_velocity_max", "emission_ring_radius", "emission_shape_scale",
            "attractor_interaction_enabled",
        ):
            self.assertEqual("unsupported-known", opaque[name]["case"], name)
        malformed_properties = {
            item["provenance"].get("property")
            for item in diagnostics if item["case"] == "malformed"
        }
        self.assertEqual({"broken_semantic"}, malformed_properties)
        material = next(s for s in scene["sourceIr"] if s["attributes"].get("id") == "Particles_main")
        mystery = next(p for p in material["properties"] if p["name"] == "mystery_material_knob")
        self.assertEqual("StrangeCtor", mystery["value"]["opaque"]["name"])
        self.assertIn("broken_semantic = Vector2(1,", scene["sourceText"])

    def test_deterministic_output_and_no_absolute_paths(self):
        self.convert("one")
        self.convert("two")
        one_scene = self.load("one", "scenes/synthetic_vfx.json")
        two_scene = self.load("two", "scenes/synthetic_vfx.json")
        self.assertEqual(one_scene, two_scene)
        for bundle in ("one", "two"):
            for path in ("manifest.json", "diagnostics.json", "scenes/synthetic_vfx.json"):
                text = (self.temp / bundle / path).read_text(encoding="utf-8")
                self.assertNotIn(str(ROOT), text)
                self.assertNotIn(str(FIXTURES), text)

    def test_resource_copy_allowlist_and_missing_resource_fail_open(self):
        manifest = self.convert()
        resources = {item["id"]: item for item in manifest["resources"]}
        self.assertTrue((self.temp / "fixture" / resources["1_tex"]["outputPath"]).is_file())
        self.assertEqual("missing-resource", resources["2_missing"]["status"])
        self.assertNotIn("outputPath", resources["2_missing"])
        self.assertNotIn("outputPath", resources["3_shader"])
        self.assertEqual("unsupported-known", resources["3_shader"]["status"])
        self.assertTrue((self.temp / "fixture" / "manifest.json").is_file())

    def test_cli_runs_from_hyphenated_tool_directory(self):
        result = subprocess.run(
            [
                sys.executable, str(TOOL / "convert.py"),
                str(FIXTURES / "synthetic_vfx.tscn"), str(self.temp),
                "--bundle-id", "cli-bundle", "--source-root", str(ROOT),
            ],
            cwd=str(ROOT), capture_output=True, text=True,
        )
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertTrue((self.temp / "cli-bundle" / "manifest.json").is_file())

    def test_rejects_bundle_scene_and_resource_traversal(self):
        with self.assertRaises(converter.ConversionError):
            converter.convert(FIXTURES / "synthetic_vfx.tscn", self.temp, "../escape")
        with self.assertRaises(converter.ConversionError):
            converter.convert(FIXTURES / "synthetic_vfx.tscn", self.temp, "ok", "../scene")
        bad = self.temp / "bad.tscn"
        bad.write_text(
            '[gd_scene format=3]\n[ext_resource type="Texture2D" path="../secret.png" id="1"]\n'
            '[node name="Root" type="Node2D"]\n', encoding="utf-8"
        )
        with self.assertRaises(converter.ConversionError):
            converter.convert(bad, self.temp, "bad")

    @unittest.skipUnless(os.environ.get("ART_STS2_ROOT"), "ART_STS2_ROOT is not set")
    def test_optional_real_smoke_scene(self):
        root = Path(os.environ["ART_STS2_ROOT"])
        matches = list(root.rglob("vfx_smoke_puff.tscn")) if root.is_dir() else []
        if not matches:
            self.skipTest("vfx_smoke_puff.tscn not found under ART_STS2_ROOT")
        first_output = self.temp / "first"
        second_output = self.temp / "second"
        manifest = converter.convert(matches[0], first_output, "real-smoke", source_root=root)
        self.assertEqual("art.sts2-vfx-bundle", manifest["format"])
        self.assertEqual("DEGRADED", manifest["capability"])
        bundle = first_output / "real-smoke"
        scene = json.loads((bundle / "scenes" / "vfx_smoke_puff.json").read_text(encoding="utf-8"))
        diagnostics = json.loads((bundle / "diagnostics.json").read_text(encoding="utf-8"))["items"]
        nodes = {node["nodePath"]: node for node in scene["typedNodes"]}
        self.assertEqual({"SmokePuffVfx", "SmokePuffVfx/Ember", "SmokePuffVfx/Clouds"}, set(nodes))
        ember = nodes["SmokePuffVfx/Ember"]["particleEmitter"]
        clouds = nodes["SmokePuffVfx/Clouds"]["particleEmitter"]
        self.assertEqual((16, 2.0, 0.8), (ember["amount"], ember["lifetime"], ember["lifetimeRandomness"]))
        self.assertEqual((18, 2.0, 0.5), (clouds["amount"], clouds["lifetime"], clouds["lifetimeRandomness"]))
        self.assertEqual({"min": 25.0, "max": 100.0}, ember["initialVelocity"])
        self.assertEqual({"min": -10.0, "max": 9.99998}, ember["angularVelocity"])
        self.assertEqual({"min": -100.0, "max": 100.0}, clouds["angularVelocity"])
        self.assertEqual(2, ember["scaleCurve"]["pointCount"])
        self.assertEqual(3, ember["alphaCurve"]["pointCount"])
        self.assertEqual(2, len(ember["colorRamp"]["stops"]))
        self.assertEqual(3, clouds["scaleCurve"]["pointCount"])
        self.assertEqual(5, len(clouds["colorRamp"]["stops"]))
        self.assertEqual((2, 2, False), (
            clouds["flipbook"]["hFrames"], clouds["flipbook"]["vFrames"],
            clouds["flipbook"]["loop"],
        ))
        resources = {item["id"]: item for item in manifest["resources"]}
        expected_textures = {
            "2_phpp0": "images/vfx/shared_use/ash_particle.png",
            "2_sr53b": "images/vfx/shared_use/smoke_vfx.png",
        }
        for resource_id, source_path in expected_textures.items():
            resource = resources[resource_id]
            self.assertEqual(source_path, resource["sourcePath"])
            copied = bundle / resource["outputPath"]
            self.assertTrue(copied.is_file())
            self.assertEqual(resource["sha256"], hashlib.sha256(copied.read_bytes()).hexdigest())
            self.assertEqual(resource["sha256"], hashlib.sha256((root / source_path).read_bytes()).hexdigest())
        self.assertEqual("unsupported-known", resources["1_vp0x6"]["status"])
        self.assertFalse([item for item in diagnostics if item["case"] == "malformed"])
        unsupported = {
            item["provenance"].get("property")
            for item in diagnostics if item["case"] == "unsupported-known"
        }
        self.assertIn("script", unsupported)
        self.assertIn("turbulence_enabled", unsupported)
        self.assertIn("turbulence_noise_strength", unsupported)
        self.assertIn("radial_velocity_curve", unsupported)
        expected_known = {
            "script", "turbulence_enabled", "turbulence_noise_strength",
            "turbulence_influence_min", "turbulence_initial_displacement_min",
            "turbulence_initial_displacement_max", "turbulence_noise_scale",
            "angle_min", "angle_max", "radial_velocity_min", "radial_velocity_max",
            "radial_velocity_curve", "emission_shape_scale", "emission_ring_axis",
            "emission_ring_height", "emission_ring_radius", "emission_ring_inner_radius",
            "emission_ring_cone_angle", "attractor_interaction_enabled", "draw_order",
        }
        self.assertTrue(expected_known.issubset(unsupported), expected_known - unsupported)

        def assert_logical_paths(value):
            if isinstance(value, dict):
                for key, child in value.items():
                    if key in ("path", "sourcePath", "outputPath", "source") and isinstance(child, str):
                        self.assertFalse(Path(child).is_absolute(), child)
                    assert_logical_paths(child)
            elif isinstance(value, list):
                for child in value:
                    assert_logical_paths(child)

        assert_logical_paths(manifest)
        assert_logical_paths(scene)
        assert_logical_paths(diagnostics)
        converter.convert(matches[0], second_output, "real-smoke", source_root=root)
        for relative in ("manifest.json", "diagnostics.json", "scenes/vfx_smoke_puff.json"):
            self.assertEqual(
                (first_output / "real-smoke" / relative).read_bytes(),
                (second_output / "real-smoke" / relative).read_bytes(),
                relative,
            )


if __name__ == "__main__":
    unittest.main()
