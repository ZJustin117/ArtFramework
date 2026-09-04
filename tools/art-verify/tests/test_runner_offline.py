import unittest
import json
import os
import subprocess
import tempfile
from unittest.mock import patch
from pathlib import Path

from runner import _harness_screenshot, _run_step, run_scenario
from test_png_compare import write_test_png
from scenario_loader import load_scenario
from command_parse import last_command_for_text, parse_command_line

ROOT = Path(__file__).resolve().parents[3]
FIXTURE = ROOT / "tests" / "ui-scenarios" / "fixtures" / "f1_probe_shape.yaml"
COMPOSITION = ROOT / "tests" / "ui-scenarios" / "fixtures" / "f4_composition_tree.yaml"
FULL_PRESENT = ROOT / "tests" / "ui-scenarios" / "fixtures" / "f5_full_present_policy.yaml"
STS1_ASSETS = ROOT / "tests" / "ui-scenarios" / "fixtures" / "f6_sts1_assets_catalog.yaml"
HAND_DRAW = ROOT / "tests" / "ui-scenarios" / "fixtures" / "f7_hand_draw_geometry.yaml"
COMBAT_INPUT = ROOT / "tests" / "ui-scenarios" / "fixtures" / "f8_combat_input_router.yaml"
CONTROLS_DRAW = ROOT / "tests" / "ui-scenarios" / "fixtures" / "f9_controls_draw.yaml"
MAP_DRAW = ROOT / "tests" / "ui-scenarios" / "fixtures" / "f10_map_draw.yaml"
PRESENT_SAFETY = ROOT / "tests" / "ui-scenarios" / "fixtures" / "f11_present_safety.yaml"
PRESENT_PROFILE = ROOT / "tests" / "ui-scenarios" / "fixtures" / "f14_present_profile_lightwave.yaml"
LIGHTWAVE_EFFECTS = ROOT / "tests" / "ui-scenarios" / "fixtures" / "f15_lightwave_demo_effects.yaml"
LIGHTWAVE_COVERAGE = ROOT / "tests" / "ui-scenarios" / "fixtures" / "f19_lightwave_component_coverage.yaml"
DEVICE = ROOT / "tests" / "ui-scenarios" / "smoke" / "s1_mod_loaded.yaml"
SPINE42_SCREENSHOT = ROOT / "tests" / "ui-scenarios" / "device" / "d1_spine42_screenshot.yaml"


class RunnerOfflineTest(unittest.TestCase):
    def test_command_parser_reads_structured_result(self):
        line = 'INFO ART_COMMAND {"status":"ERROR","command":"art open bad","message":"layout missing"}'
        self.assertEqual("ERROR", parse_command_line(line)["status"])
        self.assertEqual("layout missing", last_command_for_text(line, "art open bad")["message"])

    def test_command_parser_ignores_other_commands_and_invalid_lines(self):
        text = "\n".join(
            [
                "ART_COMMAND not-json",
                'ART_COMMAND {"status":"ERROR","command":"art open other","message":"old"}',
                'ART_COMMAND {"status":"OK","command":"art open bad","message":"opened bad"}',
            ]
        )
        self.assertIsNone(parse_command_line("ordinary log line"))
        self.assertEqual("OK", last_command_for_text(text, "art open bad")["status"])
        self.assertIsNone(last_command_for_text(text, "art open missing"))

    def test_device_command_error_fails_step(self):
        raw = {"executed": True, "command": "art open bad", "output": "ok"}
        error = {"status": "ERROR", "command": "art open bad", "message": "textfield style missing"}
        with patch("device_console.console_exec_once", return_value=raw), patch(
            "device_console.scrape_command_log", return_value=None
        ), patch("device_console.wait_for_command_log", return_value=error):
            rec = _run_step(
                {"console": "art open bad"},
                0,
                mode="device",
                last_probe=None,
                vars_map={},
                client=object(),
            )
        self.assertEqual("fail", rec["status"])
        self.assertEqual("textfield style missing", rec["error"])

    def test_lab_command_without_fresh_structured_result_fails_step(self):
        raw = {"executed": True, "command": "art lab start-run IRONCLAD", "output": "ok"}
        with patch("device_console.console_exec_once", return_value=raw), patch(
            "device_console.scrape_command_log", return_value=None
        ), patch("device_console.wait_for_command_log", return_value=None):
            rec = _run_step(
                {"console": "art lab start-run IRONCLAD"},
                0,
                mode="device",
                last_probe=None,
                vars_map={},
                client=object(),
            )
        self.assertEqual("fail", rec["status"])
        self.assertIn("fresh ART_COMMAND", rec["error"])

    def test_fresh_lab_result_recovers_lost_connector_response(self):
        raw = {"executed": False, "error": "unexpected response: "}
        result = {
            "sequence": 8,
            "status": "OK",
            "command": "art lab start-run IRONCLAD",
            "message": "start-run armed",
        }
        with patch("device_console.console_exec_once", return_value=raw), patch(
            "device_console.scrape_command_log", return_value=None
        ), patch("device_console.wait_for_command_log", return_value=result):
            rec = _run_step(
                {"console": "art lab start-run IRONCLAD"},
                0,
                mode="device",
                last_probe=None,
                vars_map={},
                client=object(),
            )
        self.assertEqual("pass", rec["status"])
        self.assertEqual(result, rec["command_log"])

    def test_art_command_reconnects_after_freshness_baseline_scrape(self):
        client = unittest.mock.Mock()
        with patch("device_console.console_exec_once", return_value={"executed": True}), patch(
            "device_console.scrape_command_log", return_value=None
        ), patch(
            "device_console.wait_for_command_log",
            return_value={"sequence": 1, "status": "OK"},
        ):
            rec = _run_step(
                {"console": "art lab ensure-fresh-menu"},
                0,
                mode="device",
                last_probe=None,
                vars_map={},
                client=client,
            )
        self.assertEqual("pass", rec["status"])

    def test_fixture_pass(self):
        r = run_scenario(FIXTURE)
        self.assertEqual(r["status"], "pass", r.get("error"))

    def test_composition_fixture_pass(self):
        r = run_scenario(COMPOSITION)
        self.assertEqual(r["status"], "pass", r.get("error"))

    def test_full_present_policy_fixture_pass(self):
        r = run_scenario(FULL_PRESENT)
        self.assertEqual(r["status"], "pass", r.get("error"))

    def test_sts1_assets_catalog_fixture_pass(self):
        r = run_scenario(STS1_ASSETS)
        self.assertEqual(r["status"], "pass", r.get("error"))

    def test_hand_draw_geometry_fixture_pass(self):
        r = run_scenario(HAND_DRAW)
        self.assertEqual(r["status"], "pass", r.get("error"))

    def test_combat_input_router_fixture_pass(self):
        r = run_scenario(COMBAT_INPUT)
        self.assertEqual(r["status"], "pass", r.get("error"))

    def test_controls_draw_fixture_pass(self):
        r = run_scenario(CONTROLS_DRAW)
        self.assertEqual(r["status"], "pass", r.get("error"))

    def test_map_draw_fixture_pass(self):
        r = run_scenario(MAP_DRAW)
        self.assertEqual(r["status"], "pass", r.get("error"))

    def test_present_safety_fixture_pass(self):
        r = run_scenario(PRESENT_SAFETY)
        self.assertEqual(r["status"], "pass", r.get("error"))

    def test_present_profile_lightwave_fixture_pass(self):
        r = run_scenario(PRESENT_PROFILE)
        self.assertEqual(r["status"], "pass", r.get("error"))

    def test_lightwave_demo_effects_fixture_pass(self):
        r = run_scenario(LIGHTWAVE_EFFECTS)
        self.assertEqual(r["status"], "pass", r.get("error"))

    def test_lightwave_component_coverage_fixture_pass(self):
        r = run_scenario(LIGHTWAVE_COVERAGE)
        self.assertEqual(r["status"], "pass", r.get("error"))

    def test_device_skips_without_serial(self):
        import os

        old = os.environ.pop("ART_D1_SERIAL", None)
        try:
            r = run_scenario(DEVICE)
            self.assertEqual(r["status"], "skip")
            self.assertIn("ART_D1_SERIAL", r.get("error") or "")
        finally:
            if old is not None:
                os.environ["ART_D1_SERIAL"] = old

    def test_spine42_screenshot_scenario_enters_combat_and_compares_capture(self):
        sc = load_scenario(SPINE42_SCREENSHOT)
        steps = sc["steps"]
        self.assertIn("ART_SPINE42_CROP", sc["require"]["env"])
        self.assertEqual("art lab ensure-fresh-menu", steps[1]["console"])
        self.assertEqual("art lab start-run IRONCLAD", steps[3]["console"])
        self.assertEqual("lab.runReady", steps[5]["wait_probe"]["assert"]["path"])
        self.assertEqual(True, steps[5]["wait_probe"]["assert"]["eq"])
        self.assertEqual("fight Cultist", steps[6]["console"])
        self.assertEqual("lab.inCombat", steps[7]["wait_probe"]["assert"]["path"])
        self.assertEqual(True, steps[7]["wait_probe"]["assert"]["eq"])
        self.assertEqual("lab.roomPhase", steps[8]["wait_probe"]["assert"]["path"])
        self.assertEqual("COMBAT", steps[8]["wait_probe"]["assert"]["eq"])
        self.assertEqual("art present skeleton on", steps[9]["console"])
        self.assertEqual(
            "render.targetsById.c2_surface_sts1_skeleton.enabled",
            steps[23]["assert"]["path"],
        )
        self.assertEqual("backend.skeleton.drawEvidence.count", steps[24]["assert"]["path"])
        self.assertEqual(1, steps[24]["assert"]["gte"])
        self.assertEqual(True, steps[25]["screenshot"])
        self.assertEqual(True, steps[27]["screenshot"])
        self.assertEqual(
            {
                "reference": "${ART_SPINE42_REFERENCE_PNG}",
                "reference_kind": "native_capture",
                "crop": "${ART_SPINE42_CROP}",
                "threshold": 16,
                "max_diff_ratio": 0.01,
                "max_diff_pixels": 20000,
                "diff": "../../../debug-artifacts/d1_spine42_screenshot_diff.png",
            },
            steps[28]["compare_screenshot"],
        )

    def test_wait_probe_checks_fixture_without_sleep(self):
        probe = {"projection": {"scene": "combat"}}
        rec = _run_step(
            {"wait_probe": {"assert": {"path": "projection.scene", "eq": "combat"}}},
            0,
            mode="fixture",
            last_probe=probe,
            vars_map={},
            client=None,
        )
        self.assertEqual("pass", rec["status"])
        self.assertEqual(1, rec["attempts"])

    def test_fixture_screenshot_is_skipped(self):
        with patch("runner._harness_screenshot") as capture:
            rec = _run_step(
                {"screenshot": True}, 0, mode="fixture", last_probe=None, vars_map={}, client=None
            )
        self.assertEqual("skip", rec["status"])
        self.assertIn("device-only", rec["error"])
        capture.assert_not_called()

    def test_device_screenshot_records_harness_artifacts(self):
        capture = {"result_json": "/tmp/result.json", "png": "/tmp/capture.png", "harness": {}}
        with patch("runner._harness_screenshot", return_value=capture) as mocked:
            rec = _run_step(
                {"screenshot": True}, 0, mode="device", last_probe=None, vars_map={}, client=None
            )
        self.assertEqual("pass", rec["status"])
        self.assertEqual(
            {"result_json": "/tmp/result.json", "png": "/tmp/capture.png"}, rec["screenshot"]
        )
        mocked.assert_called_once_with()

    def test_compare_screenshot_passes_and_records_metrics_and_diff(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            reference = root / "reference.png"
            actual = root / "actual.png"
            diff = root / "artifacts" / "diff.png"
            write_test_png(reference, 1, 1, bytes((0, 0, 0, 255)))
            write_test_png(actual, 1, 1, bytes((1, 0, 0, 255)))
            with patch("runner._harness_screenshot", return_value={"result_json": "r", "png": str(actual)}):
                vars_map = {}
                _run_step({"screenshot": True}, 0, mode="device", last_probe=None, vars_map=vars_map, client=None, scenario_path=root / "s.yaml")
            rec = _run_step(
                {"compare_screenshot": {"reference": "reference.png", "threshold": 1, "max_diff_pixels": 0, "diff": "artifacts/diff.png"}},
                1, mode="device", last_probe=None, vars_map=vars_map, client=None, scenario_path=root / "s.yaml",
            )
            self.assertEqual("pass", rec["status"])
            self.assertEqual(0, rec["comparison"]["differing_pixels"])
            self.assertEqual(str(diff), rec["comparison"]["diff"])
            self.assertTrue(diff.is_file())

    def test_compare_screenshot_expands_explicit_environment_paths(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            reference, actual = root / "reference.png", root / "actual.png"
            diff = root / "artifacts" / "diff.png"
            write_test_png(reference, 1, 1, bytes((0, 0, 0, 255)))
            write_test_png(actual, 1, 1, bytes((0, 0, 0, 255)))
            with patch.dict(
                os.environ,
                {"ART_SPINE42_REFERENCE_PNG": str(reference), "ART_SPINE42_DIFF_PNG": str(diff)},
                clear=False,
            ):
                rec = _run_step(
                    {"compare_screenshot": {"reference": "${ART_SPINE42_REFERENCE_PNG}", "diff": "${ART_SPINE42_DIFF_PNG}"}},
                    0, mode="device", last_probe=None,
                    vars_map={"_last_screenshot": {"png": str(actual)}}, client=None,
                    scenario_path=root / "s.yaml",
                )
            self.assertEqual("pass", rec["status"])
            self.assertEqual(str(reference), rec["comparison"]["reference"])
            self.assertEqual(str(diff), rec["comparison"]["diff"])

    def test_compare_screenshot_expands_environment_crop_and_records_reference_kind(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            reference, actual = root / "reference.png", root / "actual.png"
            write_test_png(reference, 2, 1, bytes((0, 0, 0, 255) * 2))
            write_test_png(actual, 2, 1, bytes((100, 0, 0, 255, 0, 0, 0, 255)))
            with patch.dict(os.environ, {"ART_SPINE42_CROP": "1, 0, 1, 1"}, clear=False):
                rec = _run_step(
                    {"compare_screenshot": {"reference": "reference.png", "reference_kind": "native_capture", "crop": "${ART_SPINE42_CROP}"}},
                    0, mode="device", last_probe=None,
                    vars_map={"_last_screenshot": {"png": str(actual)}}, client=None,
                    scenario_path=root / "s.yaml",
                )
            self.assertEqual("pass", rec["status"])
            self.assertEqual([1, 0, 1, 1], rec["comparison"]["crop"])
            self.assertEqual("native_capture", rec["comparison"]["reference_kind"])

    def test_compare_screenshot_rejects_unset_or_invalid_environment_crop(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            reference, actual = root / "reference.png", root / "actual.png"
            write_test_png(reference, 1, 1, bytes((0, 0, 0, 255)))
            write_test_png(actual, 1, 1, bytes((0, 0, 0, 255)))
            step = {"compare_screenshot": {"reference": "reference.png", "crop": "${ART_SPINE42_CROP}"}}
            kwargs = {"mode": "device", "last_probe": None, "vars_map": {"_last_screenshot": {"png": str(actual)}}, "client": None, "scenario_path": root / "s.yaml"}
            with patch.dict(os.environ, {}, clear=True), self.assertRaisesRegex(
                ValueError, "environment variable is unset: ART_SPINE42_CROP"
            ):
                _run_step(step, 0, **kwargs)
            with patch.dict(os.environ, {"ART_SPINE42_CROP": "bad-crop"}, clear=False), self.assertRaisesRegex(
                ValueError, "ART_SPINE42_CROP.*X,Y,W,H"
            ):
                _run_step(step, 0, **kwargs)
            with patch.dict(os.environ, {"ART_SPINE42_CROP": "0,0,0,1"}, clear=False), self.assertRaisesRegex(
                ValueError, "ART_SPINE42_CROP.*positive W,H"
            ):
                _run_step(step, 0, **kwargs)

    def test_compare_screenshot_rejects_unset_environment_path_without_value(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            actual = root / "actual.png"
            write_test_png(actual, 1, 1, bytes((0, 0, 0, 255)))
            with patch.dict(os.environ, {}, clear=True), self.assertRaisesRegex(
                ValueError, "environment variable is unset: ART_SPINE42_REFERENCE_PNG"
            ):
                _run_step(
                    {"compare_screenshot": {"reference": "${ART_SPINE42_REFERENCE_PNG}"}},
                    0, mode="device", last_probe=None,
                    vars_map={"_last_screenshot": {"png": str(actual)}}, client=None,
                    scenario_path=root / "s.yaml",
                )

    def test_compare_screenshot_fails_for_difference_crop_and_bad_config(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            reference, actual = root / "reference.png", root / "actual.png"
            write_test_png(reference, 2, 1, bytes((0, 0, 0, 255) * 2))
            write_test_png(actual, 2, 1, bytes((100, 0, 0, 255, 0, 0, 0, 255)))
            vars_map = {"_last_screenshot": {"png": str(actual)}}
            rec = _run_step({"compare_screenshot": {"reference": "reference.png"}}, 0, mode="device", last_probe=None, vars_map=vars_map, client=None, scenario_path=root / "s.yaml")
            self.assertEqual("fail", rec["status"])
            rec = _run_step({"compare_screenshot": {"reference": "reference.png", "crop": [1, 0, 1, 1]}}, 0, mode="device", last_probe=None, vars_map=vars_map, client=None, scenario_path=root / "s.yaml")
            self.assertEqual("pass", rec["status"])
            with self.assertRaisesRegex(ValueError, "threshold"):
                _run_step({"compare_screenshot": {"reference": "reference.png", "threshold": 256}}, 0, mode="device", last_probe=None, vars_map=vars_map, client=None, scenario_path=root / "s.yaml")

    def test_compare_screenshot_missing_reference_and_prior_capture_fail(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            actual = root / "actual.png"
            write_test_png(actual, 1, 1, bytes((0, 0, 0, 255)))
            with self.assertRaisesRegex(ValueError, "prior screenshot"):
                _run_step({"compare_screenshot": {"reference": "missing.png"}}, 0, mode="device", last_probe=None, vars_map={}, client=None, scenario_path=root / "s.yaml")
            with self.assertRaisesRegex(ValueError, "reference PNG"):
                _run_step({"compare_screenshot": {"reference": "missing.png"}}, 0, mode="device", last_probe=None, vars_map={"_last_screenshot": {"png": str(actual)}}, client=None, scenario_path=root / "s.yaml")

    def test_compare_screenshot_is_skipped_in_fixture_mode(self):
        rec = _run_step({"compare_screenshot": {"reference": "reference.png"}}, 0, mode="fixture", last_probe=None, vars_map={}, client=None)
        self.assertEqual("skip", rec["status"])

    def test_compare_screenshot_metrics_are_written_to_result_json(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            actual = root / "actual.png"
            reference = root / "reference.png"
            scenario = root / "compare.yaml"
            out_dir = root / "out"
            write_test_png(reference, 1, 1, bytes((0, 0, 0, 255)))
            write_test_png(actual, 1, 1, bytes((0, 0, 0, 255)))
            scenario.write_text(
                "name: compare\nmode: device\nsteps:\n  - screenshot: true\n  - compare_screenshot:\n      reference: reference.png\n",
                encoding="utf-8",
            )
            with patch.dict(os.environ, {"ART_D1_SERIAL": "d", "STS_CONNECTOR_PORT": "p", "SLAY_THE_AMETHYST_ROOT": "r"}, clear=False), patch(
                "device_console.connect_console", return_value=(object(), lambda: None)
            ), patch("runner._harness_screenshot", return_value={"result_json": "result.json", "png": str(actual)}):
                result = run_scenario(scenario, out_dir=out_dir)
            self.assertEqual("pass", result["status"])
            payload = json.loads(Path(result["out_file"]).read_text(encoding="utf-8"))
            self.assertEqual(0, payload["steps"][1]["comparison"]["differing_pixels"])

    def test_harness_screenshot_builds_expected_command_and_reads_result(self):
        with tempfile.TemporaryDirectory() as tmp:
            out_dir = Path(tmp) / "harness"
            result_dir = out_dir / "screenshot-123"
            result_dir.mkdir(parents=True)
            png = result_dir / "capture.png"
            png.write_bytes(b"PNG")
            result = {
                "success": True,
                "status": "SCREENSHOT_CAPTURED",
                "message": "Screenshot captured.",
                "artifacts": {"screenshot": str(png)},
            }
            result_path = result_dir / "result.json"
            result_path.write_text(json.dumps(result), encoding="utf-8")

            def run_process(command, **kwargs):
                self.assertEqual(command[1:4], ["/tmp/amethyst/scripts/tools/main.py", "sts-harness", "-Command"])
                self.assertIn("screenshot", command)
                self.assertIn("-DeviceSerial", command)
                self.assertIn("device-1", command)
                self.assertIn("-ConnectorPort", command)
                self.assertIn("39999", command)
                self.assertIn("-AgentPort", command)
                self.assertIn("9099", command)
                self.assertIn("-OutDir", command)
                self.assertIn(str(out_dir), command)
                self.assertEqual("/tmp/amethyst", kwargs["env"]["SLAY_THE_AMETHYST_ROOT"])
                return unittest.mock.Mock(returncode=0, stdout=f"Harness result: {result_path}\n", stderr="")

            with patch.dict(
                os.environ,
                {
                    "ART_D1_SERIAL": "device-1",
                    "STS_CONNECTOR_PORT": "39999",
                    "ART_GAME_PROBE_PORT": "9099",
                    "ART_HARNESS_OUT_DIR": str(out_dir),
                    "SLAY_THE_AMETHYST_ROOT": "/tmp/amethyst",
                },
                clear=False,
            ), patch("runner.subprocess.run", side_effect=run_process):
                capture = _harness_screenshot()
            self.assertEqual(str(result_path), capture["result_json"])
            self.assertEqual(str(png), capture["png"])

    def test_harness_screenshot_timeout_is_bounded(self):
        with patch.dict(
            os.environ,
            {
                "ART_D1_SERIAL": "device-1",
                "STS_CONNECTOR_PORT": "39999",
                "ART_GAME_PROBE_PORT": "9099",
                "ART_HARNESS_OUT_DIR": "/tmp/harness",
                "SLAY_THE_AMETHYST_ROOT": "/tmp/amethyst",
                "ART_SCREENSHOT_TIMEOUT_SECONDS": "0.1",
            },
            clear=False,
        ), patch(
            "runner.subprocess.run",
            side_effect=subprocess.TimeoutExpired(["harness"], 0.1),
        ):
            with self.assertRaisesRegex(RuntimeError, "timed out after 0.1s"):
                _harness_screenshot()


if __name__ == "__main__":
    unittest.main()
