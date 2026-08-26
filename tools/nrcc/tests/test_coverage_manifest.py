import os
import sys
import tempfile
import unittest


ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "nrcc"))

import coverage_manifest


class CoverageManifestTest(unittest.TestCase):
    def write_manifest(self, text):
        handle = tempfile.NamedTemporaryFile(mode="w", suffix=".yaml", delete=False)
        try:
            handle.write(text)
            return handle.name
        finally:
            handle.close()

    def tearDown(self):
        for path in getattr(self, "paths", []):
            os.unlink(path)

    def test_accepts_complete_exact_manifest(self):
        path = self.write_manifest(
            "schema: nrcc.coverage-manifest.v1\n"
            "entries:\n"
            "  - ownerId: owner\n"
            "    nativeClass: native.Owner\n"
            "    nativeMethod: render\n"
            "    pathKind: native_surface\n"
            "    surfaceId: surface\n"
            "    effectFamily: none\n"
            "    hook: Hook\n"
            "    policy: ART_DELEGATED\n"
        )
        self.paths = [path]
        result = coverage_manifest.check_manifest(
            {"paths": [{"nativeClass": "native.Owner", "nativeMethod": "render"}]},
            path,
        )
        self.assertTrue(result["ok"])
        self.assertEqual([], result["errors"])

    def test_reports_missing_stale_and_unknown_paths(self):
        path = self.write_manifest(
            "entries:\n"
            "  - ownerId: unknown\n"
            "    nativeClass: native.Unknown\n"
            "    nativeMethod: render\n"
            "    pathKind: native_surface\n"
            "    surfaceId: surface\n"
            "    effectFamily: none\n"
            "    hook: Hook\n"
            "    policy: UNKNOWN\n"
            "  - ownerId: stale\n"
            "    nativeClass: native.Stale\n"
            "    nativeMethod: draw\n"
            "    pathKind: native_surface\n"
            "    surfaceId: surface\n"
            "    effectFamily: none\n"
            "    hook: Hook\n"
            "    policy: OUT_OF_SCOPE\n"
        )
        self.paths = [path]
        result = coverage_manifest.check_manifest(
            {"paths": [{"nativeClass": "native.Live", "nativeMethod": "render"}]},
            path,
        )
        self.assertFalse(result["ok"])
        self.assertEqual([("native.Live", "render")], result["missing"])
        self.assertEqual([("native.Stale", "draw"), ("native.Unknown", "render")], result["stale"])
        self.assertEqual([("native.Unknown", "render")], result["unknown"])

    def test_rejects_invalid_policy(self):
        path = self.write_manifest("entries:\n  - policy: MAYBE\n")
        self.paths = [path]
        result = coverage_manifest.check_manifest({"paths": []}, path)
        self.assertFalse(result["ok"])
        self.assertTrue(any("invalid policy" in error for error in result["errors"]))

    def test_allows_unknown_until_strict_acceptance(self):
        path = self.write_manifest(
            "entries:\n"
            "  - ownerId: unknown\n"
            "    nativeClass: native.Unknown\n"
            "    nativeMethod: render\n"
            "    pathKind: native_surface\n"
            "    surfaceId: surface\n"
            "    effectFamily: none\n"
            "    hook: Hook\n"
            "    policy: UNKNOWN\n"
        )
        self.paths = [path]
        report = {"paths": [{"nativeClass": "native.Unknown", "nativeMethod": "render"}]}
        self.assertTrue(coverage_manifest.check_manifest(report, path)["ok"])
        self.assertFalse(
            coverage_manifest.check_manifest(report, path, strict_unknown=True)["ok"]
        )

    def test_native_with_art_overlay_is_valid_policy(self):
        path = self.write_manifest(
            "schema: nrcc.coverage-manifest.v1\n"
            "entries:\n"
            "  - ownerId: owner\n"
            "    nativeClass: native.Owner\n"
            "    nativeMethod: render\n"
            "    pathKind: native_surface\n"
            "    surfaceId: surface\n"
            "    effectFamily: none\n"
            "    hook: Hook\n"
            "    policy: NATIVE_WITH_ART_OVERLAY\n"
        )
        self.paths = [path]
        result = coverage_manifest.check_manifest(
            {"paths": [{"nativeClass": "native.Owner", "nativeMethod": "render"}]},
            path,
        )
        self.assertTrue(result["ok"])

    def test_patch_ownership_requires_art_delegated_with_justification(self):
        path = self.write_manifest(
            "schema: nrcc.coverage-manifest.v1\n"
            "entries:\n"
            "  - ownerId: owner\n"
            "    nativeClass: native.Owner\n"
            "    nativeMethod: render\n"
            "    pathKind: native_surface\n"
            "    surfaceId: surface\n"
            "    effectFamily: none\n"
            "    hook: Hook\n"
            "    policy: NATIVE_WITH_ART_OVERLAY\n"
        )
        self.paths = [path]
        report = {
            "patches": [
                {
                    "source": "artframework/sts1/patch/NativeRenderPatches.java",
                    "targetClass": "native.Owner",
                    "targetMethod": "render",
                    "hasSpireReturn": True,
                    "continuationHint": [],
                }
            ]
        }
        errors = coverage_manifest.check_patch_ownership(report, path)
        self.assertTrue(any("suppresses native draw" in e for e in errors))

    def test_patch_ownership_passes_for_approved_art_delegated(self):
        path = self.write_manifest(
            "schema: nrcc.coverage-manifest.v1\n"
            "entries:\n"
            "  - ownerId: owner\n"
            "    nativeClass: native.Owner\n"
            "    nativeMethod: render\n"
            "    pathKind: native_surface\n"
            "    surfaceId: surface\n"
            "    effectFamily: none\n"
            "    hook: Hook\n"
            "    policy: ART_DELEGATED\n"
            "    justification: test justification\n"
            "    test: test.Class#method\n"
        )
        self.paths = [path]
        report = {
            "patches": [
                {
                    "source": "artframework/sts1/patch/NativeRenderPatches.java",
                    "targetClass": "native.Owner",
                    "targetMethod": "render",
                    "hasSpireReturn": True,
                    "continuationHint": [],
                }
            ]
        }
        errors = coverage_manifest.check_patch_ownership(report, path)
        self.assertEqual([], errors)

    def test_inventory_entries_preserve_existing_and_add_unknown(self):
        entries = coverage_manifest.inventory_entries(
            {"paths": [
                {"nativeClass": "com.megacrit.cardcrawl.cards.CardGroup", "nativeMethod": "render", "kind": "native-surface"},
                {"nativeClass": "com.megacrit.cardcrawl.vfx.combat.StrikeEffect", "nativeMethod": "draw", "kind": "draw-owner"},
            ]},
            [{
                "ownerId": "owner.known", "nativeClass": "com.megacrit.cardcrawl.cards.CardGroup", "nativeMethod": "render",
                "pathKind": "native_surface", "surfaceId": "surface", "effectFamily": "none",
                "hook": "Hook", "policy": "ART_DELEGATED",
            }],
        )
        self.assertEqual("owner.known", entries[0]["ownerId"])
        self.assertEqual("UNKNOWN", entries[1]["policy"])
        self.assertEqual(2, len(entries))

    def test_unpatched_native_card_render_is_explicitly_out_of_scope(self):
        # AbstractCard.render is not hooked: card pixels come from the live,
        # un-intercepted native call while ART owns hand pose/layout only.
        entries = coverage_manifest.inventory_entries({
            "paths": [{
                "nativeClass": "com.megacrit.cardcrawl.cards.AbstractCard",
                "nativeMethod": "render",
                "kind": "render-owner",
            }]
        })
        self.assertEqual("OUT_OF_SCOPE", entries[0]["policy"])

    def test_entry_without_policy_inherits_family_default(self):
        path = self.write_manifest(
            "schema: nrcc.coverage-manifest.v1\n"
            "entries:\n"
            "  - ownerId: owner\n"
            "    nativeClass: com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen\n"
            "    nativeMethod: render\n"
            "    family: meta-outofrun-screens\n"
            "    pathKind: native_surface\n"
            "    surfaceId: surface\n"
            "    effectFamily: none\n"
            "    hook: Hook\n"
        )
        self.paths = [path]
        report = {"paths": [{
            "nativeClass": "com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen",
            "nativeMethod": "render",
        }]}
        result = coverage_manifest.check_manifest(report, path)
        self.assertTrue(result["ok"])
        self.assertEqual([], result["errors"])
        # Inherited defaults are not manual annotations: strict UNKNOWN
        # reporting must not flag them.
        self.assertEqual([], result["unknown"])
        self.assertTrue(
            coverage_manifest.check_manifest(report, path, strict_unknown=True)["ok"]
        )

    def test_explicit_policy_overrides_family_default(self):
        path = self.write_manifest(
            "schema: nrcc.coverage-manifest.v1\n"
            "entries:\n"
            "  - ownerId: owner\n"
            "    nativeClass: com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen\n"
            "    nativeMethod: render\n"
            "    family: meta-outofrun-screens\n"
            "    pathKind: native_surface\n"
            "    surfaceId: surface\n"
            "    effectFamily: none\n"
            "    hook: Hook\n"
            "    policy: NATIVE_PASSTHROUGH\n"
        )
        self.paths = [path]
        result = coverage_manifest.check_manifest(
            {"paths": [{
                "nativeClass": "com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen",
                "nativeMethod": "render",
            }]},
            path,
        )
        self.assertTrue(result["ok"])
        entry = coverage_manifest.load_manifest(path)[1][0]
        self.assertEqual("NATIVE_PASSTHROUGH", coverage_manifest.effective_policy(entry))

    def test_inherited_family_unknown_is_not_reported_as_annotation(self):
        path = self.write_manifest(
            "schema: nrcc.coverage-manifest.v1\n"
            "entries:\n"
            "  - ownerId: owner\n"
            "    nativeClass: com.megacrit.cardcrawl.vfx.combat.StrikeEffect\n"
            "    nativeMethod: render\n"
            "    family: vfx-combat\n"
            "    pathKind: transient_effect\n"
            "    surfaceId: surface\n"
            "    effectFamily: none\n"
            "    hook: Hook\n"
        )
        self.paths = [path]
        report = {"paths": [{
            "nativeClass": "com.megacrit.cardcrawl.vfx.combat.StrikeEffect",
            "nativeMethod": "render",
        }]}
        result = coverage_manifest.check_manifest(report, path)
        self.assertTrue(result["ok"])
        self.assertEqual([], result["unknown"])
        self.assertTrue(
            coverage_manifest.check_manifest(report, path, strict_unknown=True)["ok"]
        )

    def test_explicit_unknown_is_still_reported(self):
        path = self.write_manifest(
            "schema: nrcc.coverage-manifest.v1\n"
            "entries:\n"
            "  - ownerId: owner\n"
            "    nativeClass: native.Unknown\n"
            "    nativeMethod: render\n"
            "    family: vfx-combat\n"
            "    pathKind: native_surface\n"
            "    surfaceId: surface\n"
            "    effectFamily: none\n"
            "    hook: Hook\n"
            "    policy: UNKNOWN\n"
        )
        self.paths = [path]
        result = coverage_manifest.check_manifest(
            {"paths": [{"nativeClass": "native.Unknown", "nativeMethod": "render"}]},
            path,
        )
        self.assertTrue(result["ok"])
        self.assertEqual([("native.Unknown", "render")], result["unknown"])

    def test_unknown_family_id_is_rejected(self):
        path = self.write_manifest(
            "entries:\n"
            "  - ownerId: owner\n"
            "    nativeClass: native.Owner\n"
            "    nativeMethod: render\n"
            "    family: not-a-family\n"
            "    pathKind: native_surface\n"
            "    surfaceId: surface\n"
            "    effectFamily: none\n"
            "    hook: Hook\n"
            "    policy: UNKNOWN\n"
        )
        self.paths = [path]
        result = coverage_manifest.check_manifest({"paths": []}, path)
        self.assertFalse(result["ok"])
        self.assertTrue(any("unknown family" in e for e in result["errors"]))

    def test_patch_ownership_honors_inherited_policy(self):
        path = self.write_manifest(
            "schema: nrcc.coverage-manifest.v1\n"
            "entries:\n"
            "  - ownerId: owner\n"
            "    nativeClass: native.Owner\n"
            "    nativeMethod: render\n"
            "    family: meta-outofrun-screens\n"
            "    pathKind: native_surface\n"
            "    surfaceId: surface\n"
            "    effectFamily: none\n"
            "    hook: Hook\n"
        )
        self.paths = [path]
        report = {
            "patches": [
                {
                    "source": "artframework/sts1/patch/NativeRenderPatches.java",
                    "targetClass": "native.Owner",
                    "targetMethod": "render",
                    "hasSpireReturn": True,
                    "continuationHint": [],
                }
            ]
        }
        errors = coverage_manifest.check_patch_ownership(report, path)
        self.assertTrue(any("manifest policy is OUT_OF_SCOPE" in e for e in errors))

    def test_inventory_entries_write_family_and_meta_out_of_scope(self):
        entries = coverage_manifest.inventory_entries({
            "paths": [
                {
                    "nativeClass": "com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen",
                    "nativeMethod": "render",
                    "kind": "native-surface",
                },
                {
                    "nativeClass": "com.megacrit.cardcrawl.vfx.combat.StrikeEffect",
                    "nativeMethod": "render",
                    "kind": "transient-effect",
                },
            ]
        })
        by_key = dict(
            ((entry["nativeClass"], entry["nativeMethod"]), entry)
            for entry in entries
        )
        menu = by_key[("com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen", "render")]
        strike = by_key[("com.megacrit.cardcrawl.vfx.combat.StrikeEffect", "render")]
        self.assertEqual("meta-outofrun-screens", menu["family"])
        self.assertEqual("OUT_OF_SCOPE", menu["policy"])
        self.assertEqual("vfx-combat", strike["family"])
        self.assertEqual("UNKNOWN", strike["policy"])

    def test_regeneration_keeps_existing_explicit_annotation(self):
        entries = coverage_manifest.inventory_entries(
            {"paths": [{
                "nativeClass": "com.megacrit.cardcrawl.vfx.combat.StrikeEffect",
                "nativeMethod": "render",
                "kind": "transient-effect",
            }]},
            [{
                "ownerId": "owner.custom",
                "nativeClass": "com.megacrit.cardcrawl.vfx.combat.StrikeEffect",
                "nativeMethod": "render", "family": "vfx-combat",
                "pathKind": "transient_effect", "surfaceId": "surface",
                "effectFamily": "abstract_game_effect", "hook": "Hook",
                "policy": "NATIVE_WITH_ART_OVERLAY",
            }],
        )
        self.assertEqual("NATIVE_WITH_ART_OVERLAY", entries[0]["policy"])

    def test_write_inventory_manifest_round_trip_carries_family(self):
        handle = tempfile.NamedTemporaryFile(mode="w", suffix=".yaml", delete=False)
        handle.close()
        self.paths = [handle.name]
        report = {"paths": [
            {
                "nativeClass": "com.megacrit.cardcrawl.screens.splash.SplashScreen",
                "nativeMethod": "render",
                "kind": "native-surface",
            },
            {
                "nativeClass": "com.esotericsoftware.spine.SkeletonMeshRenderer",
                "nativeMethod": "draw",
                "kind": "draw-owner",
            },
        ]}
        coverage_manifest.write_inventory_manifest(report, handle.name)
        data, entries = coverage_manifest.load_manifest(handle.name)
        families_written = sorted(entry["family"] for entry in entries)
        self.assertEqual(
            ["meta-outofrun-screens", "skeleton-runtime"],
            families_written,
        )
        policies = dict(
            ((entry["nativeClass"], entry["nativeMethod"]),
             coverage_manifest.effective_policy(entry))
            for entry in entries
        )
        self.assertEqual(
            "OUT_OF_SCOPE",
            policies[("com.megacrit.cardcrawl.screens.splash.SplashScreen", "render")],
        )
        self.assertEqual(
            "ART_DELEGATED",
            policies[("com.esotericsoftware.spine.SkeletonMeshRenderer", "draw")],
        )


if __name__ == "__main__":
    unittest.main()
