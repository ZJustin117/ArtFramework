import unittest
from unittest.mock import patch
from pathlib import Path

from runner import _run_step, run_scenario
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
        with patch("device_console.console_exec", return_value=raw), patch(
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


if __name__ == "__main__":
    unittest.main()
