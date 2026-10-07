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
 *
 * <p>V03: the entry prefix also draws the delegated MAP band at the native map point, i.e.
 * exactly where native {@code DungeonMapScreen.render} was skipped in
 * {@code AbstractDungeon.render} (which runs the screen-specific map, then {@code topPanel.render}).
 * Because the native map family was suppressed, ART's map pixels must be drawn before the native
 * HUD body so the HUD lands on top instead of being covered by the post-native map overlay. The
 * draw is guarded inside {@code Sts1SurfaceRenderer.drawMapBandAtNativePoint} (delegated map, top
 * panel not ART-suppressed, once per projected frame) and is fail-open, so a failure can never block
 * the native HUD.
 */
public final class TopPanelRenderPatches {
    private TopPanelRenderPatches() {}

    @SpirePatch(clz = TopPanel.class, method = "render", paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeTopPanelRender {
        public static SpireReturn<Void> Prefix(TopPanel __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            RenderDisposition disposition = NativeRenderBridge.beginSurface(
                    "sts1.top_panel", "com.megacrit.cardcrawl.ui.panels.TopPanel", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            // V03: this Prefix entry IS the native map point — native AbstractDungeon.render runs the
            // screen-specific DungeonMapScreen.render (which ART suppressed) and only afterwards
            // TopPanel.render. Draw the delegated MAP band here so it lands in that slot; a native
            // HUD drawn below then paints over it, and natively-suppressed top-panel pixels are
            // redrawn by ART post-native on top. Idempotent per frame and fail-open (never throws):
            // the guard/once-check live inside drawMapBandAtNativePoint.
            artframework.sts1.render.Sts1SurfaceRenderer.drawMapBandAtNativePoint(sb);
            return disposition.nativeContinuation
                    ? SpireReturn.Continue()
                    : SpireReturn.Return(null);
        }
    }
}
