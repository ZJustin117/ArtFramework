package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.rooms.CampfireUI;
import com.megacrit.cardcrawl.screens.CombatRewardScreen;
import com.megacrit.cardcrawl.shop.ShopScreen;
import artframework.sts1.render.NativeRenderBridge;

/**
 * Observe native room UI draw; original renderers stay the visual authority (NRO-03).
 */
public final class RoomRenderPatches {

    private RoomRenderPatches() {}

    @SpirePatch(
            clz = CombatRewardScreen.class,
            method = "render",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeRewardRender {
        public static SpireReturn<Void> Prefix(
                CombatRewardScreen __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            NativeRenderBridge.beginSurface(
                    "sts1.reward.combat", "com.megacrit.cardcrawl.screens.CombatRewardScreen", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(
            clz = CampfireUI.class,
            method = "render",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeRestRender {
        public static SpireReturn<Void> Prefix(
                CampfireUI __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            NativeRenderBridge.beginSurface(
                    "sts1.rest", "com.megacrit.cardcrawl.rooms.CampfireUI", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(
            clz = ShopScreen.class,
            method = "render",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeShopRender {
        public static SpireReturn<Void> Prefix(
                ShopScreen __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            NativeRenderBridge.beginSurface(
                    "sts1.shop", "com.megacrit.cardcrawl.shop.ShopScreen", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(
            clz = com.megacrit.cardcrawl.rooms.TreasureRoom.class,
            method = "render",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeTreasureRender {
        public static SpireReturn<Void> Prefix(
                com.megacrit.cardcrawl.rooms.TreasureRoom __instance,
                com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            NativeRenderBridge.beginSurface(
                    "sts1.treasure", "com.megacrit.cardcrawl.rooms.TreasureRoom", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return SpireReturn.Continue();
        }
    }
}
