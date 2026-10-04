"""Offline loader/shape tests for the C04b cross-family render-order scenario.

The scenario is device-only and cannot run here, but it must still LOAD, expand to a non-empty step
list, and reference only well-formed probe paths. The render.renderOrder.phaseCounts and
nativeRender.effectBands families are checked against a representative probe shape so a typo in a
path is caught offline.
"""

import unittest
from pathlib import Path

from assert_ops import resolve_path
from scenario_loader import expand_steps, load_scenario

ROOT = Path(__file__).resolve().parents[3]
MIXED = ROOT / "tests" / "ui-scenarios" / "device" / "d1_render_order_mixed.yaml"

# One entry per RenderPhase, in declaration order, as RenderHost.probeRenderOrder emits it.
PHASE_COUNTS = {
    "ART_BACKGROUND": 0,
    "NATIVE_RETAINED": 5,
    "C1_CONTENT": 0,
    "C2_CONTENT": 3,
    "ENTITY_CONTENT": 2,
    "ART_EFFECTS": 0,
    "VERIFY_GUIDES": 0,
}

# Representative (not exhaustive) probe shape covering the path families this scenario asserts on.
PROBE = {
    "render": {
        "renderOrder": {
            "status": "ready",
            "monotonic": True,
            "duplicateStableKeys": [],
            "phaseCounts": dict(PHASE_COUNTS),
        },
        "targetsById": {
            "c2_surface_sts1_combat_hand": {"phase": "C2_CONTENT"},
        },
    },
    "backend": {
        "verify": {
            "configuredMode": "off",
            "submissionStatus": "disabled",
            "modeSupported": True,
        },
        "renderPlan": {
            "aura": {"ready": 1},
            "nativeRender": {
                "effectBands": {
                    "claimed": {"effectListBehind": 2, "effectListFront": 2},
                    "orderViolations": 0,
                    "passesObserved": 5,
                },
            },
        },
    },
}

PREFIXES = (
    "render.renderOrder.phaseCounts.",
    "backend.renderPlan.nativeRender.effectBands.",
)


def _iter_asserts(steps):
    for step in steps:
        if not isinstance(step, dict):
            continue
        spec = step.get("assert")
        if isinstance(spec, dict):
            yield spec
        wait = step.get("wait_probe")
        if isinstance(wait, dict) and isinstance(wait.get("assert"), dict):
            yield wait["assert"]


class RenderOrderMixedScenarioTest(unittest.TestCase):
    def test_scenario_loads_and_expands(self):
        sc = load_scenario(MIXED)
        self.assertEqual("d1_render_order_mixed", sc["name"])
        self.assertEqual(1, sc["schemaVersion"])
        self.assertEqual("device", sc["mode"])
        self.assertEqual("d1", sc["device"])
        self.assertEqual(
            {"ART_D1_SERIAL", "STS_CONNECTOR_PORT", "SLAY_THE_AMETHYST_ROOT"},
            set(sc["require"]["env"]),
        )
        steps = expand_steps(sc["steps"])
        self.assertGreater(len(steps), 0)

    def test_assert_paths_are_well_formed_and_phase_effect_paths_resolve(self):
        steps = expand_steps(load_scenario(MIXED)["steps"])
        scoped = 0
        for spec in _iter_asserts(steps):
            path = spec.get("path")
            self.assertIsInstance(path, str, spec)
            self.assertTrue(path.strip(), spec)
            if path.startswith(PREFIXES):
                scoped += 1
                found, _ = resolve_path(PROBE, path)
                self.assertTrue(found, f"scoped path did not resolve: {path}")
        # Both required families are actually exercised by the scenario.
        self.assertGreater(scoped, 0)

    def test_phase_counts_key_presence_assertions_cover_the_boundary(self):
        steps = expand_steps(load_scenario(MIXED)["steps"])
        asserts = list(_iter_asserts(steps))
        texts = [a.get("path") for a in asserts]
        self.assertIn("render.renderOrder.phaseCounts.NATIVE_RETAINED", texts)
        self.assertIn("render.renderOrder.phaseCounts.C2_CONTENT", texts)
        self.assertIn("render.renderOrder.phaseCounts.C1_CONTENT", texts)
        self.assertIn("render.renderOrder.phaseCounts.VERIFY_GUIDES", texts)
        self.assertIn("render.renderOrder.phaseCounts.ART_EFFECTS", texts)
        # ART_EFFECTS and VERIFY_GUIDES are documented as zero by design (overlay families).
        boundary = {
            a["path"]: a
            for a in asserts
            if a.get("path") in ("render.renderOrder.phaseCounts.ART_EFFECTS", "render.renderOrder.phaseCounts.VERIFY_GUIDES")
        }
        self.assertEqual(0, boundary["render.renderOrder.phaseCounts.ART_EFFECTS"].get("eq"))
        self.assertEqual(0, boundary["render.renderOrder.phaseCounts.VERIFY_GUIDES"].get("eq"))

    def test_effect_band_paths_resolve_and_bogus_path_fails(self):
        steps = expand_steps(load_scenario(MIXED)["steps"])
        band_paths = [
            a["path"]
            for a in _iter_asserts(steps)
            if str(a.get("path", "")).startswith("backend.renderPlan.nativeRender.effectBands.")
        ]
        self.assertIn("backend.renderPlan.nativeRender.effectBands.claimed.effectListBehind", band_paths)
        self.assertIn("backend.renderPlan.nativeRender.effectBands.claimed.effectListFront", band_paths)
        self.assertIn("backend.renderPlan.nativeRender.effectBands.orderViolations", band_paths)
        for path in band_paths:
            found, _ = resolve_path(PROBE, path)
            self.assertTrue(found, f"effect-band path did not resolve: {path}")
        # A bogus path in either family must fail to resolve against the representative shape.
        for bogus in (
            "render.renderOrder.phaseCounts.NOT_A_PHASE",
            "backend.renderPlan.nativeRender.effectBands.claimed.notABand",
        ):
            found, _ = resolve_path(PROBE, bogus)
            self.assertFalse(found, f"bogus path unexpectedly resolved: {bogus}")


if __name__ == "__main__":
    unittest.main()
