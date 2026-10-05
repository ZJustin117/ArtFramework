package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.rooms.CampfireUI;
import com.megacrit.cardcrawl.screens.CombatRewardScreen;
import com.megacrit.cardcrawl.shop.ShopScreen;
import artframework.sts1.render.NativeRenderBridge;
import artframework.sts1.render.RenderDisposition;

/**
 * Gates native room UI draw. When a reward/rest/shop surface is FULL + mounted + matching
 * scene, ART suppresses the original renderer and paints the room through synced C2 items.
 * Otherwise the native renderer continues unchanged.
 *
 * <p>NRO-04 D08: the treasure surface is NOT gated here. {@code TreasureRoom.render} calls
 * {@code chest.render} then {@code super.render}; suppressing the room would also remove
 * {@code AbstractRoom.render} (the player sprite). Treasure now suppresses only
 * {@code AbstractChest.render} ({@link TreasureChestRenderPatches}).
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
            RenderDisposition disposition = NativeRenderBridge.beginSurface(
                    "sts1.reward.combat", "com.megacrit.cardcrawl.screens.CombatRewardScreen", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return disposition.nativeContinuation
                    ? SpireReturn.Continue()
                    : SpireReturn.Return(null);
        }
    }

    @SpirePatch(
            clz = CampfireUI.class,
            method = "render",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeRestRender {
        public static SpireReturn<Void> Prefix(
                CampfireUI __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            RenderDisposition disposition = NativeRenderBridge.beginSurface(
                    "sts1.rest", "com.megacrit.cardcrawl.rooms.CampfireUI", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return disposition.nativeContinuation
                    ? SpireReturn.Continue()
                    : SpireReturn.Return(null);
        }
    }

    @SpirePatch(
            clz = ShopScreen.class,
            method = "render",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeShopRender {
        public static SpireReturn<Void> Prefix(
                ShopScreen __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            RenderDisposition disposition = NativeRenderBridge.beginSurface(
                    "sts1.shop", "com.megacrit.cardcrawl.shop.ShopScreen", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return disposition.nativeContinuation
                    ? SpireReturn.Continue()
                    : SpireReturn.Return(null);
        }
    }

    // NRO-04 D08 regression fix: the treasure room is NOT suppressed here. Suppressing
    // TreasureRoom.render also suppressed AbstractRoom.render, which draws the player sprite via
    // AbstractDungeon.player.render. ART owns only AbstractChest.render now (see
    // TreasureChestRenderPatches), so the room continues and the player is preserved.
}
