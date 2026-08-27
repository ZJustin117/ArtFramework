package artframework.sts1.patch;

import artframework.sts1.render.NativeRenderBridge;
import artframework.sts1.render.RenderDisposition;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;

/**
 * Observe-only hooks for the card/potion targeting arrow. Both private
 * renderTargetingUi(SpriteBatch) variants are captured at their choke points and always
 * continue to native rendering. The surface is {@code sts1.combat.targeting}; OBSERVE level
 * produces CAPTURE_AND_PASS dispositions, OFF level passes through, and every failure path
 * returns Continue() to keep native pixels authoritative.
 */
public final class CombatTargetingRenderPatches {
    private CombatTargetingRenderPatches() {}

    @SpirePatch(
            clz = com.megacrit.cardcrawl.characters.AbstractPlayer.class,
            method = "renderTargetingUi",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObservePlayerTargeting {
        public static SpireReturn<Void> Prefix(
                com.megacrit.cardcrawl.characters.AbstractPlayer __instance,
                com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            return handleTargeting();
        }
    }

    @SpirePatch(
            clz = com.megacrit.cardcrawl.ui.panels.PotionPopUp.class,
            method = "renderTargetingUi",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObservePotionTargeting {
        public static SpireReturn<Void> Prefix(
                com.megacrit.cardcrawl.ui.panels.PotionPopUp __instance,
                com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            return handleTargeting();
        }
    }

    private static SpireReturn<Void> handleTargeting() {
        try {
            RenderDisposition disposition = NativeRenderBridge.beginSurface(
                    "sts1.combat.targeting",
                    "com.megacrit.cardcrawl.overlay.targeting",
                    "renderTargetingUi",
                    "");
            // Zero suppression: native pixels always continue.
            return disposition.nativeContinuation ? SpireReturn.Continue() : SpireReturn.Continue();
        } catch (Throwable ignored) {
            // Fail open: never block the native targeting render.
            return SpireReturn.Continue();
        }
    }
}
