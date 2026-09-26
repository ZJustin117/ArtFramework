package artframework.sts1.patch;

import artframework.sts1.render.NativeRenderBridge;
import artframework.sts1.render.RenderDisposition;
import artframework.sts1.render.StanceArtRenderer;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.stances.AbstractStance;

/**
 * Stance-family native render interception.
 *
 * <p>Default off and fail-open: while the stance delegation gate is closed (the default),
 * or the ART stance renderer is not ready, the native {@code AbstractStance.render} pixels
 * continue unchanged. Only a {@code DELEGATE_TO_ART} disposition (gate open and ART ready)
 * suppresses the native stance pixels; the ART-side drawing is supplied by S2b-2.
 */
public final class StanceRenderPatches {
    private StanceRenderPatches() {}

    @SpirePatch(
            clz = AbstractStance.class,
            method = "render",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class SuppressDelegatedNativeStance {
        public static SpireReturn<Void> Prefix(AbstractStance __instance,
                com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            RenderDisposition d = NativeRenderBridge.beginStanceRender(__instance);
            if (d.mode == RenderDisposition.Mode.DELEGATE_TO_ART) {
                // ART owns the stance pixels this frame. Draw them here and consume the token so it
                // cannot accumulate; a failed draw fails open to the native render below.
                String owner = NativeRenderBridge.stanceOwner(__instance);
                if (StanceArtRenderer.render(sb, owner)) {
                    NativeRenderBridge.recordStanceDraw(d.invocationId, 1);
                    return SpireReturn.Return(null);
                }
                NativeRenderBridge.recordStanceFailure(d.invocationId);
                return SpireReturn.Continue();
            }
            // PASS_THROUGH / FAIL_OPEN (and any other mode): native stance rendering continues.
            return SpireReturn.Continue();
        }
    }
}
