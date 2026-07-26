import unittest
from pathlib import Path

from runner import run_scenario

ROOT = Path(__file__).resolve().parents[3]
FIXTURE = ROOT / "tests" / "ui-scenarios" / "fixtures" / "f1_probe_shape.yaml"
DEVICE = ROOT / "tests" / "ui-scenarios" / "smoke" / "s1_mod_loaded.yaml"


class RunnerOfflineTest(unittest.TestCase):
    def test_fixture_pass(self):
        r = run_scenario(FIXTURE)
        self.assertEqual(r["status"], "pass", r.get("error"))

    def test_device_skips_without_serial(self):
        import os

        old = os.environ.pop("SPIREUI_D1_SERIAL", None)
        try:
            r = run_scenario(DEVICE)
            self.assertEqual(r["status"], "skip")
            self.assertIn("SPIREUI_D1_SERIAL", r.get("error") or "")
        finally:
            if old is not None:
                os.environ["SPIREUI_D1_SERIAL"] = old


if __name__ == "__main__":
    unittest.main()
