package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.ui.panels.TopPanel;
import artframework.sts1.render.NativeRenderBridge;
import artframework.sts1.render.RenderDisposition;

/**
 * Gates native top-panel rendering. When the top-panel surface is FULL + mounted,
 * ART suppresses the original TopPanel.render and paints the HUD through the synced C2
 * item. Otherwise the native renderer continues unchanged.
 */
public final class TopPanelRenderPatches {
    private TopPanelRenderPatches() {}

    @SpirePatch(clz = TopPanel.class, method = "render", paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeTopPanelRender {
        public static SpireReturn<Void> Prefix(TopPanel __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            RenderDisposition disposition = NativeRenderBridge.beginSurface(
                    "sts1.top_panel", "com.megacrit.cardcrawl.ui.panels.TopPanel", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return disposition.nativeContinuation
                    ? SpireReturn.Continue()
                    : SpireReturn.Return(null);
        }
    }
}
