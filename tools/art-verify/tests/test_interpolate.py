"""Offline tests for ${var} interpolation (A02)."""

import unittest
from unittest.mock import patch
from pathlib import Path

from runner import _run_step, interpolate
from scenario_loader import expand_steps, load_scenario

ROOT = Path(__file__).resolve().parents[3]
ISOLATION = ROOT / "tests" / "ui-scenarios" / "device" / "d1_vfx_claim_isolation.yaml"


class InterpolateTest(unittest.TestCase):
    def test_resolves_single_token(self):
        self.assertEqual(
            "art claim spawn cardtrail 20",
            interpolate("art claim spawn ${target_kind} 20", {"target_kind": "cardtrail"}),
        )

    def test_leaves_token_free_string_unchanged(self):
        self.assertEqual("art claim off", interpolate("art claim off", {"target_kind": "cardtrail"}))

    def test_raises_clear_error_on_missing_variable(self):
        with self.assertRaisesRegex(ValueError, "target_kind"):
            interpolate("art claim spawn ${target_kind} 20", {})

    def test_handles_multiple_tokens(self):
        self.assertEqual("a b", interpolate("${x} ${y}", {"x": "a", "y": "b"}))

    def test_non_string_values_are_unchanged(self):
        self.assertEqual(3, interpolate(3, {"x": "a"}))
        self.assertIsNone(interpolate(None, {"x": "a"}))
        payload = [1, 2]
        self.assertIs(payload, interpolate(payload, {"x": "a"}))

    def test_empty_token_is_not_substituted(self):
        self.assertEqual("${}", interpolate("${}", {"x": "a"}))

    def test_bare_dollar_is_unchanged(self):
        self.assertEqual("cost $5", interpolate("cost $5", {"x": "a"}))

    def test_dollar_name_without_braces_is_unchanged(self):
        self.assertEqual("$FOO", interpolate("$FOO", {"FOO": "a"}))

    def test_invalid_identifier_is_unchanged(self):
        self.assertEqual("${1bad}", interpolate("${1bad}", {"1bad": "a"}))

    def test_missing_second_token_raises(self):
        # A missing second token must raise rather than returning a half-substituted command.
        with self.assertRaisesRegex(ValueError, "missing"):
            interpolate("${present} ${missing}", {"present": "ok"})

    def test_partial_substitution_is_never_returned(self):
        with self.assertRaises(ValueError):
            interpolate("${x} ${missing}", {"x": "a"})


class IsolationScenarioTest(unittest.TestCase):
    def test_scenario_parses_and_expands(self):
        sc = load_scenario(ISOLATION)
        self.assertEqual("d1_vfx_claim_isolation", sc["name"])
        self.assertEqual("device", sc["mode"])
        self.assertEqual(
            {"ART_D1_SERIAL", "STS_CONNECTOR_PORT", "SLAY_THE_AMETHYST_ROOT"},
            set(sc["require"]["env"]),
        )
        steps = expand_steps(sc["steps"])
        self.assertEqual(
            {"target_kind": "cardtrail", "unsupported_kind": "totallyBogusKind"}, steps[0]["set"]
        )
        for step in steps:
            self.assertNotIn("expect_error", step)
        unsupported = [
            s for s in steps
            if isinstance(s.get("console"), str) and "${unsupported_kind}" in s["console"]
        ]
        self.assertEqual(1, len(unsupported))

    def test_set_target_kind_interpolates_to_cardtrail_and_spawns(self):
        sc = load_scenario(ISOLATION)
        steps = expand_steps(sc["steps"])
        vars_map = {}
        _run_step(steps[0], 0, mode="device", last_probe=None, vars_map=vars_map, client=None)
        self.assertEqual("cardtrail", vars_map["target_kind"])
        spawn = next(
            s for s in steps
            if isinstance(s.get("console"), str) and "${target_kind}" in s["console"]
        )
        ok = {"status": "OK", "command": "art claim spawn cardtrail 20", "message": "spawned"}
        with patch("device_console.console_exec_once", return_value={"executed": True}), patch(
            "device_console.scrape_command_log", return_value=None
        ), patch("device_console.wait_for_command_log", return_value=ok):
            rec = _run_step(spawn, 1, mode="device", last_probe=None, vars_map=vars_map, client=object())
        self.assertEqual("pass", rec["status"])
        self.assertEqual("art claim spawn cardtrail 20", rec["console"])

    def test_op_values_are_interpolated(self):
        ok = {"status": "OK", "command": "art op set cardtrail", "message": "ok"}
        with patch("device_console.console_exec_once", return_value={"executed": True}), patch(
            "device_console.scrape_command_log", return_value=None
        ), patch("device_console.wait_for_command_log", return_value=ok):
            rec = _run_step(
                {"op": ["set", "${target_kind}"]},
                0, mode="device", last_probe=None, vars_map={"target_kind": "cardtrail"}, client=object(),
            )
        self.assertEqual("pass", rec["status"])
        self.assertEqual("art op set cardtrail", rec["console"])

    def test_assert_and_capture_path_is_interpolated(self):
        probe = {"reports": {"a": {"draws": 7}}}
        vars_map = {"suffix": "a"}
        rec = _run_step(
            {"capture": {"path": "reports.${suffix}.draws", "var": "n"}},
            0, mode="fixture", last_probe=probe, vars_map=vars_map, client=None,
        )
        self.assertEqual("pass", rec["status"])
        self.assertEqual(7, vars_map["n"])
        rec = _run_step(
            {"assert": {"path": "reports.${suffix}.draws", "eq_var": "n"}},
            1, mode="fixture", last_probe=probe, vars_map=vars_map, client=None,
        )
        self.assertEqual("pass", rec["status"])


if __name__ == "__main__":
    unittest.main()
