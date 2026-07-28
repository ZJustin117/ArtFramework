import unittest
from pathlib import Path

from runner import run_scenario

ROOT = Path(__file__).resolve().parents[3]
FIXTURE = ROOT / "tests" / "ui-scenarios" / "fixtures" / "f1_probe_shape.yaml"
COMPOSITION = ROOT / "tests" / "ui-scenarios" / "fixtures" / "f4_composition_tree.yaml"
FULL_PRESENT = ROOT / "tests" / "ui-scenarios" / "fixtures" / "f5_full_present_policy.yaml"
STS1_ASSETS = ROOT / "tests" / "ui-scenarios" / "fixtures" / "f6_sts1_assets_catalog.yaml"
HAND_DRAW = ROOT / "tests" / "ui-scenarios" / "fixtures" / "f7_hand_draw_geometry.yaml"
COMBAT_INPUT = ROOT / "tests" / "ui-scenarios" / "fixtures" / "f8_combat_input_router.yaml"
CONTROLS_DRAW = ROOT / "tests" / "ui-scenarios" / "fixtures" / "f9_controls_draw.yaml"
DEVICE = ROOT / "tests" / "ui-scenarios" / "smoke" / "s1_mod_loaded.yaml"


class RunnerOfflineTest(unittest.TestCase):
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


if __name__ == "__main__":
    unittest.main()
