package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.rooms.CampfireUI;
import com.megacrit.cardcrawl.screens.CombatRewardScreen;
import com.megacrit.cardcrawl.shop.ShopScreen;
import artframework.sts1.render.RestDrawPath;
import artframework.sts1.render.RewardDrawPath;
import artframework.sts1.render.ShopDrawPath;
import artframework.sts1.render.TreasureDrawPath;

/**
 * Skip native room UI draw while ART room full-present is FULL_READY (milestone 26).
 * Treasure uses room-level chest render when suppress is active.
 */
public final class RoomRenderPatches {

    private RoomRenderPatches() {}

    @SpirePatch(
            clz = CombatRewardScreen.class,
            method = "render",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class SuppressNativeRewardRender {
        public static SpireReturn<Void> Prefix(
                CombatRewardScreen __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            return RewardDrawPath.shouldSuppressNativeReward()
                    ? SpireReturn.Return(null)
                    : SpireReturn.Continue();
        }
    }

    @SpirePatch(
            clz = CampfireUI.class,
            method = "render",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class SuppressNativeRestRender {
        public static SpireReturn<Void> Prefix(
                CampfireUI __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            return RestDrawPath.shouldSuppressNativeRest()
                    ? SpireReturn.Return(null)
                    : SpireReturn.Continue();
        }
    }

    @SpirePatch(
            clz = ShopScreen.class,
            method = "render",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class SuppressNativeShopRender {
        public static SpireReturn<Void> Prefix(
                ShopScreen __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            return ShopDrawPath.shouldSuppressNativeShop()
                    ? SpireReturn.Return(null)
                    : SpireReturn.Continue();
        }
    }

    @SpirePatch(
            clz = com.megacrit.cardcrawl.rooms.TreasureRoom.class,
            method = "render",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class SuppressNativeTreasureRender {
        public static SpireReturn<Void> Prefix(
                com.megacrit.cardcrawl.rooms.TreasureRoom __instance,
                com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            return TreasureDrawPath.shouldSuppressNativeTreasure()
                    ? SpireReturn.Return(null)
                    : SpireReturn.Continue();
        }
    }
}
