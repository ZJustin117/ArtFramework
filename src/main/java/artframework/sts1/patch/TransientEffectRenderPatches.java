package artframework.sts1.patch;

import artframework.sts1.render.AuraArtRenderer;
import artframework.sts1.render.NativeRenderBridge;
import artframework.sts1.render.RenderDisposition;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** Instance-level effect observation. The native effect queue remains authoritative. */
public final class TransientEffectRenderPatches {
    private TransientEffectRenderPatches() {}

    @SpirePatch(clz = AbstractGameEffect.class, method = "render",
            paramtypez = {SpriteBatch.class, float.class, float.class})
    public static class ObserveEffectRenderAtPosition {
        public static SpireReturn<Void> Prefix(AbstractGameEffect __instance, SpriteBatch sb,
                float x, float y) {
            // NRO-04: effects are observed only; the native effect queue remains authoritative
            // except for a default-off per-instance aura claim, which suppresses only that instance
            // after a real ART draw and otherwise fails open to the native render below.
            RenderDisposition disposition;
            try {
                disposition = NativeRenderBridge.beginEffectRender(__instance, "render_at");
            } catch (Throwable error) {
                NativeRenderBridge.recordEffectObservationFailure();
                return SpireReturn.Continue();
            }
            if (disposition.nativeContinuation) return SpireReturn.Continue();
            boolean auraClaim = false;
            try {
                auraClaim = NativeRenderBridge.isAuraClaimInvocation(disposition.invocationId);
            } catch (Throwable error) {
                NativeRenderBridge.recordEffectObservationFailure();
            }
            if (!auraClaim) return SpireReturn.Return(null);
            boolean drew = false;
            try {
                drew = AuraArtRenderer.render(sb, __instance);
            } catch (Throwable error) {
                NativeRenderBridge.recordEffectObservationFailure();
            }
            if (drew) {
                try {
                    NativeRenderBridge.recordEffectDraw(disposition.invocationId, 1);
                } catch (Throwable error) {
                    NativeRenderBridge.recordEffectObservationFailure();
                }
                try {
                    AuraArtRenderer.recordDraw();
                } catch (Throwable error) {
                    NativeRenderBridge.recordEffectObservationFailure();
                }
                return SpireReturn.Return(null);
            }
            // Always consume the pending claim before failing open (never leave a delegated gap);
            // the consume itself must not throw out of the observation path.
            try {
                NativeRenderBridge.recordEffectFailure(disposition.invocationId);
            } catch (Throwable error) {
                NativeRenderBridge.recordEffectObservationFailure();
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = AbstractGameEffect.class, method = "update")
    public static class ObserveEffectUpdate {
        public static void Postfix(AbstractGameEffect __instance) {
            NativeRenderBridge.observeEffectUpdate(__instance);
        }
    }

}
