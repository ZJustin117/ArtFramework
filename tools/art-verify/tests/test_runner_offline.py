import unittest
import json
import os
import subprocess
import tempfile
from unittest.mock import patch
from pathlib import Path

from runner import _harness_screenshot, _run_step, run_scenario
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

    def test_spine42_screenshot_scenario_enters_combat_before_screenshot(self):
        sc = load_scenario(SPINE42_SCREENSHOT)
        steps = sc["steps"]
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
        self.assertEqual(True, steps[24]["screenshot"])

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
