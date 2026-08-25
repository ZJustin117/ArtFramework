package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import com.megacrit.cardcrawl.screens.select.HandCardSelectScreen;
import artframework.sts1.render.NativeRenderBridge;

/** Observe native grid/hand select draw; original renderers stay the visual authority (NRO-03). */
public final class SelectRenderPatches {

    private SelectRenderPatches() {}

    @SpirePatch(
            clz = GridCardSelectScreen.class,
            method = "render",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeGridSelectRender {
        public static SpireReturn<Void> Prefix(
                GridCardSelectScreen __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            NativeRenderBridge.beginSurface(
                    "sts1.select.grid", "com.megacrit.cardcrawl.screens.select.GridCardSelectScreen", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(
            clz = HandCardSelectScreen.class,
            method = "render",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeHandSelectRender {
        public static SpireReturn<Void> Prefix(
                HandCardSelectScreen __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            NativeRenderBridge.beginSurface(
                    "sts1.select.hand", "com.megacrit.cardcrawl.screens.select.HandCardSelectScreen", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return SpireReturn.Continue();
        }
    }
}
