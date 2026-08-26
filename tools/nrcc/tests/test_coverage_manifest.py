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
                {"nativeClass": "native.Known", "nativeMethod": "render", "kind": "native-surface"},
                {"nativeClass": "native.New", "nativeMethod": "draw", "kind": "draw-owner"},
            ]},
            [{
                "ownerId": "owner.known", "nativeClass": "native.Known", "nativeMethod": "render",
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


if __name__ == "__main__":
    unittest.main()
