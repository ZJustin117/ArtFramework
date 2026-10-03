package artframework.sts1.patch;

import artframework.sts1.render.VfxArtRenderer;
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
 *
 * <p>Effect completion is observed by BOTH paths now:
 * <ul>
 *   <li>the class-level {@code AbstractGameEffect#update()} Postfix in
 *       {@link TransientEffectRenderPatches} (fires only for subclasses that call
 *       {@code super.update()}); and</li>
 *   <li>the container-level {@code AbstractDungeon.update()} instrument below
 *       ({@link ObserveContainerEffectUpdates} → {@link #observeAfterUpdate}), which observes the
 *       effects whose {@code update()} does NOT call {@code super.update()}.</li>
 * </ul>
 * The container instrument uses {@code $proceed($$)} and therefore runs the native
 * {@code effect.update()} exactly as before; it only OBSERVES afterwards. It does NOT re-apply
 * update and does NOT touch identity/admission. Crucially, the container path DETACHES rather
 * than completes: when a superless effect reports {@code isDone}, {@link #observeAfterUpdate}
 * drops its active record WITHOUT retaining a terminal record, so a still-rendered/re-added object
 * is re-admitted on its next render instead of being rejected. Container {@code isDone} is not a
 * reliable end signal, and a retained terminal record under the NRM-13 sticky ids reproduces the
 * exact D1 failure that removed the earlier instrument (and its retained-terminal revision):
 * {@code rejectedTerminal}/{@code unknownLifecycle} grew ~1 per observed render, freezing
 * {@code rendered}/{@code total} and permanently failing strict acceptance. The detached path does
 * not change identity/admission. Pooled ({@code com.badlogic.gdx.utils.Pool.Poolable}) effects are
 * additionally EXCLUDED from observation entirely (neither complete nor detach): pool reuse
 * re-`init`s the SAME object and renders it again, so it must simply stay active and be re-admitted
 * on reuse. The class-level {@code super.update()} Postfix path still COMPLETES and retains for
 * super-callers. Native memory remains bounded by the active drop plus the ledger/registry
 * capacities as defense-in-depth.
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
        // Non-continuing disposition: a transient-effect claim draws here; an isolate claim stays suppressed.
        boolean vfxClaim = false;
        try {
            vfxClaim = NativeRenderBridge.isVfxClaimInvocation(disposition.invocationId);
        } catch (Throwable error) {
            NativeRenderBridge.recordEffectObservationFailure();
        }
        if (vfxClaim) {
            boolean drew = false;
            try {
                drew = VfxArtRenderer.render(sb, effect);
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
                    VfxArtRenderer.recordDraw();
                } catch (Throwable error) {
                    NativeRenderBridge.recordEffectObservationFailure();
                }
                return;
            }
            // Always consume the pending claim before failing open (never leave a delegated gap);
            // the consume itself must not throw out of the observation path. A decline of an
            // instance the native effect would not have drawn either is a benign no-pixel decline.
            try {
                NativeRenderBridge.recordEffectDeclined(disposition.invocationId, effect);
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

    /**
     * Completion-observation entry appended by the container {@code AbstractDungeon.update()}
     * instrument ({@link ObserveContainerEffectUpdates}).
     *
     * <p>Ordering guarantee: the instrument emits {@code $proceed($$)} FIRST and this helper
     * SECOND, so it observes the effect AFTER the native {@code update()} ran and reads the
     * POST-update {@code isDone} value. Effects whose {@code update()} overrides without calling
     * {@code super.update()} never reach the class-level Postfix, so without this path their
     * records stay active until the active cap.
     *
     * <p>This helper DETACHES via {@link NativeRenderBridge#observeEffectUpdateDetached}: when the
     * effect reports {@code isDone} it drops the active record WITHOUT retaining a terminal record.
     * Container {@code isDone} is not a reliable end signal — {@code AbstractDungeon.render} may
     * still render the object (not yet removed, or re-added), and under the NRM-13 sticky per-object
     * ids a retained terminal record would reject that next render. Detaching lets a later render of
     * the same object re-admit as a fresh active record. Contrast with the class-level
     * {@code super.update()} Postfix path ({@link NativeRenderBridge#observeEffectUpdate}), which
     * still COMPLETES and retains; and with the removed {@code observeThenUpdate} attempt, which
     * re-applied update and rejected live effects as terminal.
     *
     * <p>It does NOT re-apply or duplicate the native update and does NOT change
     * identity/admission. Any observation failure is swallowed (and counted as fail-open) so the
     * native update path is never interrupted.
     */
    public static void observeAfterUpdate(AbstractGameEffect effect) {
        try {
            NativeRenderBridge.observeEffectUpdateDetached(effect);
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

    @SpirePatch(clz = AbstractDungeon.class, method = "update")
    public static class ObserveContainerEffectUpdates {
        /**
         * True iff the call is the native single-argument, void {@code AbstractGameEffect.update()}
         * that the container AFTER-update instrument targets. Public so the targeting predicate can
         * be unit-tested directly (the test package differs) without javassist fakes.
         */
        public static boolean isNativeEffectUpdateCall(
                String methodName, String className, String signature) {
            return "update".equals(methodName)
                    && "com.megacrit.cardcrawl.vfx.AbstractGameEffect".equals(className)
                    && "()V".equals(signature);
        }

        /**
         * The replacement body emitted at each targeted call site: run the native update exactly as
         * before via {@code $proceed($$)}, then OBSERVE the post-update {@code isDone}. Public so
         * tests can assert the native update is preserved and update is never re-applied.
         */
        public static String replacementBody() {
            return "{ $proceed($$); "
                    + "artframework.sts1.patch.TransientEffectContainerPatches"
                    + ".observeAfterUpdate($0); }";
        }

        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!isNativeEffectUpdateCall(
                            call.getMethodName(), call.getClassName(), call.getSignature())) {
                        return;
                    }
                    // $proceed($$) runs the native effect.update() exactly as before; the appended
                    // call only observes the POST-update isDone value (never re-applies update).
                    call.replace(replacementBody());
                }
            };
        }
    }
}
