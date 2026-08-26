import os
import sys
import unittest


ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "nrcc"))

import scan_sts_render


class StaticScanTest(unittest.TestCase):
    def test_resolves_simple_imported_patch_target(self):
        imports = scan_sts_render.parse_imports(
            "import com.megacrit.cardcrawl.screens.DungeonMapScreen;\n"
        )
        self.assertEqual(
            "com.megacrit.cardcrawl.screens.DungeonMapScreen",
            scan_sts_render.resolve_target_class("DungeonMapScreen.class", imports),
        )

    def test_strips_qualified_class_literal(self):
        self.assertEqual(
            "com.megacrit.cardcrawl.screens.DungeonMapScreen",
            scan_sts_render.resolve_target_class(
                "com.megacrit.cardcrawl.screens.DungeonMapScreen.class", {}
            ),
        )

    def test_render_patch_filter_excludes_input_patch(self):
        self.assertTrue(
            scan_sts_render.is_render_patch(
                {"targetMethod": "render", "source": "CombatHandRenderPatches.java"}
            )
        )
        self.assertFalse(
            scan_sts_render.is_render_patch(
                {"targetMethod": "updateInput", "source": "CombatHandInputPatches.java"}
            )
        )

    def test_render_intent_is_a_render_patch(self):
        self.assertTrue(
            scan_sts_render.is_render_patch(
                {"targetMethod": "renderIntent", "source": "CombatIntentRenderPatches.java"}
            )
        )

    def test_classification_marks_effects(self):
        self.assertEqual(
            "transient-effect",
            scan_sts_render.classify(
                "com.megacrit.cardcrawl.effects.AbstractGameEffect", "render"
            ),
        )

    def test_static_scan_uses_hooked_vocabulary(self):
        self.assertEqual("nrcc.static-scan.v2", scan_sts_render.STATIC_SCAN_SCHEMA)
        self.assertEqual(
            "relic-power",
            scan_sts_render.classify(
                "com.megacrit.cardcrawl.relics.AbstractRelic", "render"
            ),
        )

    def test_candidate_classes_include_explicit_non_sts_patch_targets(self):
        self.assertEqual(
            ["com.esotericsoftware.spine.SkeletonMeshRenderer"],
            scan_sts_render.candidate_classes(
                [], ["com.esotericsoftware.spine.SkeletonMeshRenderer"]
            ),
        )

if __name__ == "__main__":
    unittest.main()
