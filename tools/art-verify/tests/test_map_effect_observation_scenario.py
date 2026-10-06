"""Offline loader/shape test for the B06b map-effect-through-container-seam scenario.

The scenario is device-only and cannot run here, but it must LOAD, expand to a non-empty step list,
and reference only well-formed probe paths. The claim-draw and topLevelFront band paths are checked
against a representative probe shape so a typo is caught offline.

B06b finding (see docs/task.md): there is NO separate map-screen AbstractGameEffect.render call
site; MapRoomNode adds MapCircleEffect to AbstractDungeon.topLevelEffects, so the container seam
(TransientEffectContainerPatches.ObserveContainerEffectRenders, band topLevelFront) already covers
the map effect path.
"""

import unittest
from pathlib import Path

from assert_ops import resolve_path
from scenario_loader import expand_steps, load_scenario

ROOT = Path(__file__).resolve().parents[3]
SCENARIO = ROOT / "tests" / "ui-scenarios" / "device" / "d1_map_effect_observation.yaml"

# Representative (not exhaustive) probe shape covering the paths this scenario asserts on.
PROBE = {
    "projection": {"available": True, "scene": "map"},
    "backend": {
        "renderPlan": {
            "aura": {"gate": True, "ready": 1, "draws": 7},
            "nativeRender": {
                "effectBands": {
                    "claimed": {
                        "effectListBehind": 0,
                        "effectListFront": 0,
                        "topLevelFront": 3,
                        "unknown": 0,
                    },
                },
            },
        },
    },
}

BAND_PREFIXES = (
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


class MapEffectObservationScenarioTest(unittest.TestCase):
    def test_scenario_loads_and_expands(self):
        sc = load_scenario(SCENARIO)
        self.assertEqual("d1_map_effect_observation", sc["name"])
        self.assertEqual(1, sc["schemaVersion"])
        self.assertEqual("device", sc["mode"])
        self.assertEqual("d1", sc["device"])
        self.assertEqual(
            {"ART_D1_SERIAL", "STS_CONNECTOR_PORT", "SLAY_THE_AMETHYST_ROOT"},
            set(sc["require"]["env"]),
        )
        steps = expand_steps(sc["steps"])
        self.assertGreater(len(steps), 0)

    def test_assert_paths_are_well_formed_and_band_paths_resolve(self):
        steps = expand_steps(load_scenario(SCENARIO)["steps"])
        scoped = 0
        for spec in _iter_asserts(steps):
            path = spec.get("path")
            self.assertIsInstance(path, str, spec)
            self.assertTrue(path.strip(), spec)
            if path.startswith(BAND_PREFIXES):
                scoped += 1
                found, _ = resolve_path(PROBE, path)
                self.assertTrue(found, f"band path did not resolve: {path}")
        self.assertGreater(scoped, 0)

    def test_claims_the_top_level_front_band_and_draw_delta(self):
        steps = expand_steps(load_scenario(SCENARIO)["steps"])
        asserts = list(_iter_asserts(steps))
        paths = [a.get("path") for a in asserts]
        # The claimed topLevelFront band (the map effect's native band) is asserted.
        self.assertIn(
            "backend.renderPlan.nativeRender.effectBands.claimed.topLevelFront",
            paths,
        )
        # Claim-gate readiness is asserted before the draw delta.
        self.assertIn("backend.renderPlan.aura.gate", paths)
        self.assertIn("backend.renderPlan.aura.ready", paths)
        # The draw counters are asserted as delta vs a captured baseline, never absolute.
        band_asserts = {
            a["path"]: a
            for a in asserts
            if str(a.get("path", "")).startswith(BAND_PREFIXES)
        }
        tlf = band_asserts[
            "backend.renderPlan.nativeRender.effectBands.claimed.topLevelFront"
        ]
        self.assertIn("gt_var", tlf)

    def test_bogus_band_path_fails_to_resolve(self):
        for bogus in (
            "backend.renderPlan.nativeRender.effectBands.claimed.notABand",
            "backend.renderPlan.aura.notAGauge",
        ):
            found, _ = resolve_path(PROBE, bogus)
            self.assertFalse(found, f"bogus path unexpectedly resolved: {bogus}")


if __name__ == "__main__":
    unittest.main()
