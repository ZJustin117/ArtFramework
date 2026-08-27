package artframework.sts1.patch;

import artframework.sts1.render.NativeRenderBridge;
import artframework.sts1.render.RenderDisposition;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** Instance-level effect observation. The native effect queue remains authoritative. */
public final class TransientEffectRenderPatches {
    private TransientEffectRenderPatches() {}

    @SpirePatch(clz = AbstractGameEffect.class, method = "render",
            paramtypez = {SpriteBatch.class, float.class, float.class})
    public static class ObserveEffectRenderAtPosition {
        public static void Prefix(AbstractGameEffect __instance, SpriteBatch sb,
                float x, float y) {
            // NRO-04: effects are observed only; the native effect queue remains authoritative.
            RenderDisposition disposition = NativeRenderBridge.beginEffectRender(__instance, "render_at");
            if (!disposition.nativeContinuation) {
                NativeRenderBridge.recordEffectObservationFailure();
            }
        }
    }

    @SpirePatch(clz = AbstractGameEffect.class, method = "update")
    public static class ObserveEffectUpdate {
        public static void Postfix(AbstractGameEffect __instance) {
            NativeRenderBridge.observeEffectUpdate(__instance);
        }
    }

}
