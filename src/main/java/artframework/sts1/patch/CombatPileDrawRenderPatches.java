package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.ui.panels.DrawPilePanel;
import artframework.sts1.render.NativeRenderBridge;
import artframework.sts1.render.RenderDisposition;

/**
 * Gates native draw-pile panel rendering. When the draw-pile surface is FULL + mounted + combat
 * scene, ART suppresses the original {@code DrawPilePanel.render} and paints the draw-zone chrome
 * through the synced C2 item. Otherwise the native renderer continues unchanged.
 */
public final class CombatPileDrawRenderPatches {
    private CombatPileDrawRenderPatches() {}

    @SpirePatch(clz = DrawPilePanel.class, method = "render", paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativePileDrawRender {
        public static SpireReturn<Void> Prefix(DrawPilePanel __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            RenderDisposition disposition = NativeRenderBridge.beginSurface(
                    "sts1.combat.pile_draw", "com.megacrit.cardcrawl.ui.panels.DrawPilePanel", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return disposition.nativeContinuation
                    ? SpireReturn.Continue()
                    : SpireReturn.Return(null);
        }
    }
}
