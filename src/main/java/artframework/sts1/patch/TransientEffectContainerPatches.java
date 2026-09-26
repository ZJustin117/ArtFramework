package artframework.sts1.patch;

import artframework.sts1.render.AuraArtRenderer;
import artframework.sts1.render.NativeRenderBridge;
import artframework.sts1.render.RenderDisposition;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * Observes container-driven effect renders at the three AbstractDungeon traversal sites
 * (effectList behind/regular, topLevelEffects). The single-arg
 * {@code AbstractGameEffect#render(SpriteBatch)} is abstract — ModTheSpire cannot attach a
 * Prefix to a bodyless method — so each call site is replaced with an observe-then-render
 * helper. Native rendering follows the bridge disposition; observation failures fail open and
 * never interrupt drawing.
 */
public final class TransientEffectContainerPatches {

    private TransientEffectContainerPatches() {}

    /** Package-visible entry generated at each instrumented call site. */
    public static void observeThenRender(AbstractGameEffect effect, SpriteBatch sb) {
        RenderDisposition disposition;
        try {
            disposition = NativeRenderBridge.beginEffectRender(effect, "render");
        } catch (Throwable error) {
            NativeRenderBridge.recordEffectObservationFailure();
            disposition = RenderDisposition.failOpen(-1L, "observation_error");
        }
        if (disposition.nativeContinuation) {
            try {
                effect.render(sb);
            } catch (Throwable error) {
                NativeRenderBridge.recordEffectObservationFailure();
            }
            return;
        }
        // Non-continuing disposition: an aura claim draws here; an isolate claim stays suppressed.
        boolean auraClaim = false;
        try {
            auraClaim = NativeRenderBridge.isAuraClaimInvocation(disposition.invocationId);
        } catch (Throwable error) {
            NativeRenderBridge.recordEffectObservationFailure();
        }
        if (auraClaim) {
            boolean drew = false;
            try {
                drew = AuraArtRenderer.render(sb, effect);
            } catch (Throwable error) {
                NativeRenderBridge.recordEffectObservationFailure();
            }
            if (drew) {
                try {
                    NativeRenderBridge.recordEffectDraw(disposition.invocationId, 1);
                } catch (Throwable error) {
                    NativeRenderBridge.recordEffectObservationFailure();
                }
                return;
            }
            // Always consume the pending claim before failing open (never leave a delegated gap);
            // the consume itself must not throw out of the observation path.
            try {
                NativeRenderBridge.recordEffectFailure(disposition.invocationId);
            } catch (Throwable error) {
                NativeRenderBridge.recordEffectObservationFailure();
            }
            // fall through to the native render
        } else {
            // Isolate claim (or any other suppression): native pixels stay suppressed.
            return;
        }
        try {
            effect.render(sb);
        } catch (Throwable error) {
            NativeRenderBridge.recordEffectObservationFailure();
        }
    }

    @SpirePatch(clz = AbstractDungeon.class, method = "render",
            paramtypez = {SpriteBatch.class})
    public static class ObserveContainerEffectRenders {
        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!"render".equals(call.getMethodName())
                            || !"com.megacrit.cardcrawl.vfx.AbstractGameEffect"
                                    .equals(call.getClassName())
                            || !"(Lcom/badlogic/gdx/graphics/g2d/SpriteBatch;)V"
                                    .equals(call.getSignature())) {
                        return;
                    }
                    call.replace("{"
                            + "artframework.sts1.patch.TransientEffectContainerPatches"
                            + ".observeThenRender($0, $1);"
                            + "}");
                }
            };
        }
    }
}
