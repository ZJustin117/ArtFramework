package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.ui.buttons.ProceedButton;
import artframework.sts1.render.NativeRenderBridge;
import artframework.sts1.render.RenderDisposition;

/**
 * Gates native proceed button rendering. When the proceed surface is FULL + mounted,
 * ART suppresses the original ProceedButton.render and paints the button through the synced C2
 * item. Otherwise the native renderer continues unchanged.
 */
public final class ProceedButtonRenderPatches {
    private ProceedButtonRenderPatches() {}

    @SpirePatch(clz = ProceedButton.class, method = "render", paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeProceedButtonRender {
        public static SpireReturn<Void> Prefix(ProceedButton __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            RenderDisposition disposition = NativeRenderBridge.beginSurface(
                    "sts1.combat.proceed", "com.megacrit.cardcrawl.ui.buttons.ProceedButton", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return disposition.nativeContinuation
                    ? SpireReturn.Continue()
                    : SpireReturn.Return(null);
        }
    }
}
