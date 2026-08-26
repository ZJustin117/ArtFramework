package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import com.megacrit.cardcrawl.screens.select.HandCardSelectScreen;
import artframework.sts1.render.NativeRenderBridge;
import artframework.sts1.render.RenderDisposition;

/**
 * Gates native grid/hand select draw. When a select surface is FULL + mounted, ART suppresses the
 * original GridCardSelectScreen/HandCardSelectScreen.render and paints the selection through
 * synced C2 items. Otherwise the native renderer continues unchanged.
 */
public final class SelectRenderPatches {

    private SelectRenderPatches() {}

    @SpirePatch(
            clz = GridCardSelectScreen.class,
            method = "render",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeGridSelectRender {
        public static SpireReturn<Void> Prefix(
                GridCardSelectScreen __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            RenderDisposition disposition = NativeRenderBridge.beginSurface(
                    "sts1.select.grid", "com.megacrit.cardcrawl.screens.select.GridCardSelectScreen", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return disposition.nativeContinuation
                    ? SpireReturn.Continue()
                    : SpireReturn.Return(null);
        }
    }

    @SpirePatch(
            clz = HandCardSelectScreen.class,
            method = "render",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeHandSelectRender {
        public static SpireReturn<Void> Prefix(
                HandCardSelectScreen __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            RenderDisposition disposition = NativeRenderBridge.beginSurface(
                    "sts1.select.hand", "com.megacrit.cardcrawl.screens.select.HandCardSelectScreen", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return disposition.nativeContinuation
                    ? SpireReturn.Continue()
                    : SpireReturn.Return(null);
        }
    }
}
