import os
import sys
import unittest


ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "nrcc"))

import families


# Inline fixture of real STS1 native render paths (class, method).  Every
# family must keep at least one representative here so a rule regression or a
# new family without coverage fails fast; the fixture never reads the
# gitignored debug-artifacts scan output.
FIXTURE_PATHS = (
    # overlay-targeting placeholder: method rule wins over any class rule.
    ("com.megacrit.cardcrawl.monsters.AbstractMonster", "renderTargetingUi"),
    ("com.esotericsoftware.spine.SkeletonMeshRenderer", "draw"),
    ("com.megacrit.cardcrawl.vfx.combat.StrikeEffect", "render"),
    ("com.megacrit.cardcrawl.vfx.combat.ClashEffect", "draw"),
    ("com.megacrit.cardcrawl.vfx.scene.TorchParticleMEffect", "render"),
    ("com.megacrit.cardcrawl.vfx.campfire.CampfireSmithEffect", "render"),
    ("com.megacrit.cardcrawl.vfx.cardManip.ExhaustCardEffect", "render"),
    ("com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect", "render"),
    ("com.megacrit.cardcrawl.vfx.stance.WrathParticleEffect", "render"),
    ("com.megacrit.cardcrawl.vfx.stance.StanceAuraEffect", "render"),
    ("com.megacrit.cardcrawl.vfx.AbstractGameEffect", "render"),
    ("com.megacrit.cardcrawl.vfx.SpeechBubble", "render"),
    ("com.megacrit.cardcrawl.vfx.shader.ShaderEffect", "render"),
    ("com.megacrit.cardcrawl.vfx.deprecated.ShinyChestEffect", "render"),
    ("com.megacrit.cardcrawl.cards.AbstractCard", "render"),
    ("com.megacrit.cardcrawl.cards.CardGroup", "renderHand"),
    ("com.megacrit.cardcrawl.cards.Soul", "render"),
    ("com.megacrit.cardcrawl.cards.SoulGroup", "render"),
    ("com.megacrit.cardcrawl.characters.AbstractPlayer", "renderHand"),
    ("com.megacrit.cardcrawl.characters.AnimatedNpc", "render"),
    ("com.megacrit.cardcrawl.core.CardCrawlGame", "render"),
    ("com.megacrit.cardcrawl.core.OverlayMenu", "render"),
    ("com.megacrit.cardcrawl.core.GameCursor", "draw"),
    ("com.megacrit.cardcrawl.dungeons.AbstractDungeon", "render"),
    ("com.megacrit.cardcrawl.monsters.AbstractMonster", "renderIntent"),
    ("com.megacrit.cardcrawl.monsters.exordium.TheGuardian", "draw"),
    ("com.megacrit.cardcrawl.orbs.AbstractOrb", "render"),
    ("com.megacrit.cardcrawl.orbs.Frost", "render"),
    ("com.megacrit.cardcrawl.relics.AbstractRelic", "render"),
    ("com.megacrit.cardcrawl.blights.AbstractBlight", "render"),
    ("com.megacrit.cardcrawl.potions.AbstractPotion", "render"),
    ("com.megacrit.cardcrawl.stances.AbstractStance", "render"),
    ("com.megacrit.cardcrawl.stances.NeutralStance", "render"),
    ("com.megacrit.cardcrawl.map.MapRoomNode", "render"),
    ("com.megacrit.cardcrawl.map.MapEdge", "draw"),
    ("com.megacrit.cardcrawl.map.Legend", "render"),
    ("com.megacrit.cardcrawl.rooms.AbstractRoom", "render"),
    ("com.megacrit.cardcrawl.rooms.CampfireUI", "render"),
    ("com.megacrit.cardcrawl.rooms.VictoryRoom", "render"),
    ("com.megacrit.cardcrawl.events.GenericEventDialog", "render"),
    ("com.megacrit.cardcrawl.events.RoomEventDialog", "render"),
    ("com.megacrit.cardcrawl.events.AbstractImageEvent", "render"),
    ("com.megacrit.cardcrawl.events.shrines.GremlinWheelGame", "render"),
    ("com.megacrit.cardcrawl.shop.ShopScreen", "render"),
    ("com.megacrit.cardcrawl.shop.Merchant", "render"),
    ("com.megacrit.cardcrawl.rewards.RewardItem", "render"),
    ("com.megacrit.cardcrawl.rewards.chests.AbstractChest", "render"),
    ("com.megacrit.cardcrawl.ui.panels.TopPanel", "render"),
    ("com.megacrit.cardcrawl.ui.panels.EnergyPanel", "render"),
    ("com.megacrit.cardcrawl.ui.panels.DrawPilePanel", "render"),
    ("com.megacrit.cardcrawl.ui.panels.DiscardPilePanel", "render"),
    ("com.megacrit.cardcrawl.ui.panels.PotionPopUp", "render"),
    ("com.megacrit.cardcrawl.ui.buttons.EndTurnButton", "render"),
    ("com.megacrit.cardcrawl.ui.buttons.ProceedButton", "render"),
    ("com.megacrit.cardcrawl.ui.campfire.AbstractCampfireOption", "render"),
    ("com.megacrit.cardcrawl.helpers.Hitbox", "render"),
    ("com.megacrit.cardcrawl.helpers.DrawMaster", "draw"),
    ("com.megacrit.cardcrawl.helpers.TipHelper", "renderTip"),
    ("com.megacrit.cardcrawl.helpers.Label", "render"),
    ("com.megacrit.cardcrawl.helpers.Sprite", "draw"),
    ("com.megacrit.cardcrawl.helpers.AbstractDrawable", "render"),
    ("com.megacrit.cardcrawl.ui.DialogWord", "render"),
    ("com.megacrit.cardcrawl.ui.SpeechWord", "render"),
    ("com.megacrit.cardcrawl.ui.FtueTip", "render"),
    ("com.megacrit.cardcrawl.ui.MultiPageFtue", "render"),
    ("com.megacrit.cardcrawl.screens.DungeonMapScreen", "render"),
    ("com.megacrit.cardcrawl.screens.CombatRewardScreen", "render"),
    ("com.megacrit.cardcrawl.screens.select.GridCardSelectScreen", "render"),
    ("com.megacrit.cardcrawl.screens.select.HandCardSelectScreen", "render"),
    ("com.megacrit.cardcrawl.screens.SingleCardViewPopup", "render"),
    ("com.megacrit.cardcrawl.screens.MasterDeckViewScreen", "render"),
    ("com.megacrit.cardcrawl.screens.DeathScreen", "render"),
    ("com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen", "render"),
    ("com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen", "render"),
    ("com.megacrit.cardcrawl.screens.options.SettingsScreen", "render"),
    ("com.megacrit.cardcrawl.screens.compendium.CardLibraryScreen", "render"),
    ("com.megacrit.cardcrawl.screens.stats.StatsScreen", "render"),
    ("com.megacrit.cardcrawl.screens.runHistory.RunHistoryScreen", "render"),
    ("com.megacrit.cardcrawl.screens.leaderboards.LeaderboardScreen", "render"),
    ("com.megacrit.cardcrawl.screens.splash.SplashScreen", "render"),
    ("com.megacrit.cardcrawl.screens.mainMenu.PatchNotesScreen", "render"),
    ("com.megacrit.cardcrawl.screens.mainMenu.SaveSlot", "render"),
    ("com.megacrit.cardcrawl.screens.DoorUnlockScreen", "render"),
    ("com.megacrit.cardcrawl.credits.CreditsScreen", "render"),
    ("com.megacrit.cardcrawl.cutscenes.NeowNarrationScreen", "render"),
    ("com.megacrit.cardcrawl.daily.DailyScreen", "render"),
    ("com.megacrit.cardcrawl.neow.NeowUnlockScreen", "render"),
    ("com.megacrit.cardcrawl.scenes.TitleBackground", "render"),
    ("com.megacrit.cardcrawl.unlock.UnlockCharacterScreen", "render"),
)


class FamiliesTest(unittest.TestCase):
    def test_fixture_covers_every_family(self):
        seen = set(
            families.family_for(native_class, native_method)
            for native_class, native_method in FIXTURE_PATHS
        )
        self.assertEqual(set(families.FAMILY_IDS), seen)

    def test_family_ids_are_unique_and_complete(self):
        self.assertEqual(25, len(families.FAMILY_IDS))
        self.assertEqual(len(set(families.FAMILY_IDS)), len(families.FAMILY_IDS))

    def test_first_match_wins_on_overlapping_rules(self):
        cases = {
            # vfx.stance beats the broader vfx. root rule.
            "com.megacrit.cardcrawl.vfx.stance.StanceAuraEffect": "vfx-stance-aura",
            # screens subpackages beat the generic screens. in-run rule.
            "com.megacrit.cardcrawl.screens.mainMenu.SaveSlot": "meta-outofrun-screens",
            # exact DoorUnlockScreen override beats the screens. prefix.
            "com.megacrit.cardcrawl.screens.DoorUnlockScreen": "meta-outofrun-screens",
            # campfire option is a control button, not a hud panel.
            "com.megacrit.cardcrawl.ui.campfire.AbstractCampfireOption": "buttons-controls",
            # map package stays out of the screens DungeonMapScreen family.
            "com.megacrit.cardcrawl.map.DungeonMap": "map-graph",
            # Neow room/event stay structural; only its screens are meta.
            "com.megacrit.cardcrawl.neow.NeowRoom": "room-shells",
            "com.megacrit.cardcrawl.neow.NeowEvent": "event-dialogs",
            "com.megacrit.cardcrawl.neow.NeowUnlockScreen": "meta-outofrun-screens",
            # deprecated/shader vfx fall through to the misc root group.
            "com.megacrit.cardcrawl.vfx.deprecated.ShinyChestEffect": "vfx-misc-root",
            "com.megacrit.cardcrawl.vfx.shader.ShaderEffect": "vfx-misc-root",
        }
        for native_class, expected in cases.items():
            self.assertEqual(
                expected,
                families.family_for(native_class, "render"),
                native_class,
            )

    def test_targeting_method_rule_beats_class_rules(self):
        self.assertEqual(
            "overlay-targeting",
            families.family_for("com.megacrit.cardcrawl.monsters.AbstractMonster", "renderTargetingUi"),
        )
        self.assertEqual(
            "overlay-targeting",
            families.family_for("com.megacrit.cardcrawl.core.OverlayMenu", "renderTargetingUi"),
        )

    def test_unmatched_class_fails_loudly_with_class_name(self):
        with self.assertRaises(ValueError) as ctx:
            families.family_for("com.megacrit.cardcrawl.future.UnknownWidget", "render")
        self.assertIn("com.megacrit.cardcrawl.future.UnknownWidget", str(ctx.exception))

    def test_family_default_policies(self):
        self.assertEqual(
            {"meta-outofrun-screens": "OUT_OF_SCOPE"},
            families.FAMILY_DEFAULT_POLICY,
        )


if __name__ == "__main__":
    unittest.main()
