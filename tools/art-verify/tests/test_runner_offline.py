import unittest
from pathlib import Path

from runner import _run_step, run_scenario

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
