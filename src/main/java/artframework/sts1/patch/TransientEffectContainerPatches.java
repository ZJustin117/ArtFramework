package artframework.sts1.patch;

import artframework.sts1.render.VfxArtRenderer;
import artframework.sts1.render.NativeRenderBridge;
import artframework.sts1.render.RenderDisposition;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rewards.RewardItem;
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
 * <p><b>B06b (map effect path).</b> There is NO separate map-screen
 * {@code AbstractGameEffect.render(SpriteBatch)} call site: {@code javap} on the 1.0 jar finds
 * none in {@code MapRoomNode} or {@code DungeonMapScreen}, and the decompiled
 * {@code MapRoomNode.update()} adds {@code new MapCircleEffect(...)} directly to
 * {@code AbstractDungeon.topLevelEffects}. The map effect is therefore rendered by the
 * {@code topLevelEffects} loop instrumented by {@link ObserveContainerEffectRenders} (band
 * {@link artframework.sts1.render.EffectRenderBand#LINE_TOP_LEVEL_FRONT}) — already observed and
 * claimable. B06b adds NO patch and NO new suppression authority; it adds a regression test proving
 * the container seam covers the map path plus a D1 map-observation scenario.
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
        observeThenRender(effect, sb, -1);
    }

    /**
     * Entry generated at each instrumented call site. {@code nativeLine} is the native
     * {@code AbstractDungeon.render} LineNumberTable line of the replaced call site
     * ({@link artframework.sts1.render.EffectRenderBand}). It is used ONLY for the observation-only
     * native local render-order baseline (NRO-04 C01); the 2-arg overload supplies {@code -1}
     * (UNKNOWN) for callers that do not carry a line.
     */
    public static void observeThenRender(AbstractGameEffect effect, SpriteBatch sb, int nativeLine) {
        RenderDisposition disposition;
        try {
            disposition = NativeRenderBridge.beginEffectRender(effect, "render");
        } catch (Throwable error) {
            NativeRenderBridge.recordEffectObservationFailure();
            disposition = RenderDisposition.failOpen(-1L, "observation_error");
        }
        if (disposition.nativeContinuation) {
            renderNativeThenRecordBand(effect, sb, nativeLine);
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
                recordObservedBand(effect, nativeLine, true);
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
            // Isolate claim (or any other suppression): native pixels stay suppressed, so no render
            // happens and no band is recorded (nothing is drawn on this path).
            return;
        }
        renderNativeThenRecordBand(effect, sb, nativeLine);
    }

    /**
     * Runs the native render and then records its native band. The native render is attempted first
     * and the band is recorded even when the native render throws, because the observation is that
     * the traversal reached this native band — not that the draw succeeded. Both steps are guarded
     * so a failure can never interrupt the container render loop.
     */
    private static void renderNativeThenRecordBand(
            AbstractGameEffect effect, SpriteBatch sb, int nativeLine) {
        try {
            effect.render(sb);
        } catch (Throwable error) {
            NativeRenderBridge.recordEffectObservationFailure();
        }
        recordObservedBand(effect, nativeLine, false);
    }

    /** Observation-only band record; isolated so it can never interrupt the render path. */
    private static void recordObservedBand(AbstractGameEffect effect, int nativeLine, boolean claimed) {
        try {
            NativeRenderBridge.recordObservedEffectBand(effect, nativeLine, claimed);
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

    /**
     * Per-render-pass boundary for the band-ordinal observation (NRO-04 C02). This is a NEW,
     * observation-only native patch: a {@code Prefix} at {@code AbstractDungeon.render} ENTRY that
     * bumps {@link NativeRenderBridge#beginEffectRenderPass()} and does nothing else.
     *
     * <p>Design/why: {@code EffectRenderBand.rank} proves that within ONE native render traversal
     * the three call sites are strictly sequential (A line 2674 → B line 2697 → C line 2802), so
     * per-PASS monotonicity is the correct invariant. The earlier C02 revision used the ART
     * {@code lastFrameId()} as the boundary, but that id does not advance on the menu/transition
     * screens, so successive {@code AbstractDungeon.render} invocations were collapsed into one
     * "frame" and produced false {@code orderViolations} on D1. This Prefix gives the observation
     * an honest pass boundary instead.
     *
     * <p>It changes NO render behavior: it never reorders, suppresses, or draws pixels, never
     * throws (the bridge call is itself try/caught), and never touches identity/admission/
     * lifecycle. ModTheSpire supports multiple patch classes on one method, so this Prefix coexists
     * with the existing {@link ObserveContainerEffectRenders} {@code @SpireInstrumentPatch}
     * ExprEditor on the same {@code render(SpriteBatch)} method; that instrument is unchanged.
     */
    @SpirePatch(clz = AbstractDungeon.class, method = "render",
            paramtypez = {SpriteBatch.class})
    public static class ObserveEffectRenderPass {
        public static void Prefix(AbstractDungeon __instance, SpriteBatch sb) {
            NativeRenderBridge.beginEffectRenderPass();
        }
    }

    @SpirePatch(clz = AbstractDungeon.class, method = "render",
            paramtypez = {SpriteBatch.class})
    public static class ObserveContainerEffectRenders {
        /**
         * True iff the call is the native single-argument, void
         * {@code AbstractGameEffect.render(SpriteBatch)} that this instrument targets. Public so the
         * targeting predicate can be unit-tested directly (the test package differs) without
         * javassist fakes. This is the SAME descriptor the B05b reward-loop extension matches.
         *
         * <p>The three {@code AbstractDungeon.render} call sites this covers are the two
         * {@code effectList} loops and the {@code topLevelEffects} loop. The last one is the MAP
         * effect path: {@code MapRoomNode} adds {@code MapCircleEffect} to
         * {@code AbstractDungeon.topLevelEffects}, so map-screen effects are observed here with no
         * separate map-screen instrument (B06b).
         */
        public static boolean isNativeEffectRenderCall(
                String methodName, String className, String signature) {
            return "render".equals(methodName)
                    && "com.megacrit.cardcrawl.vfx.AbstractGameEffect".equals(className)
                    && "(Lcom/badlogic/gdx/graphics/g2d/SpriteBatch;)V".equals(signature);
        }

        /**
         * The replacement body emitted at each targeted call site: the observe-then-render helper
         * re-invokes the native {@code effect.render(sb)} under the bridge disposition. The
         * {@code nativeLine} classifies the native band ({@link
         * artframework.sts1.render.EffectRenderBand}); the {@code topLevelEffects} loop carries
         * {@link artframework.sts1.render.EffectRenderBand#LINE_TOP_LEVEL_FRONT}. Public so tests can
         * assert the helper call and its line argument without running the instrumentor.
         */
        public static String replacementBody(int line) {
            return "{ artframework.sts1.patch.TransientEffectContainerPatches"
                    + ".observeThenRender($0, $1, " + line + "); }";
        }

        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!isNativeEffectRenderCall(
                            call.getMethodName(), call.getClassName(), call.getSignature())) {
                        return;
                    }
                    // The helper re-invokes the native effect.render(sb) itself; no $proceed is used
                    // and no SpireReturn suppression path is introduced.
                    call.replace(replacementBody(call.getLineNumber()));
                }
            };
        }
    }

    /**
     * OBSERVATION-ONLY extension of the container effect seam (B05b) at a fourth traversal site:
     * {@code com.megacrit.cardcrawl.rewards.RewardItem#render(SpriteBatch)} iterates its own
     * private {@code effects} list calling {@code AbstractGameEffect.render(SpriteBatch)}. That
     * loop is reached via {@code CombatRewardScreen.render}, NOT through
     * {@code AbstractDungeon.render}, so the reward-screen effects (starting with
     * {@code RewardGlowEffect}) were previously never observed or claimable.
     *
     * <p>This patch reuses exactly the same {@link TransientEffectContainerPatches#observeThenRender}
     * helper as {@link ObserveContainerEffectRenders}: it replaces the single
     * {@code AbstractGameEffect.render:(SpriteBatch)V} call with a call to the observe-then-render
     * helper (NO {@code $_ =} and no {@code SpireReturn}), so native rendering follows the bridge
     * disposition and observation failures fail open and never interrupt the reward render loop. It
     * adds NO new suppression authority: the only native-absence branch is the pre-existing
     * isolate-only disposition already governed by the {@code AbstractDungeon} entry.
     *
     * <p>Double-observation is impossible by construction: {@code RewardItem.effects} is a private
     * list that is NOT {@code AbstractDungeon.effectList}/{@code topLevelEffects}, so no instance is
     * observed twice for one draw. The native line carried here is the reward-loop call-site line,
     * which does not match any known {@code EffectRenderBand} line, so it degrades to
     * {@link artframework.sts1.render.EffectRenderBand.Band#UNKNOWN} — an observation record only,
     * never order evidence. javap confirms exactly one such call site (offset 992) in the 1.0 jar.
     */
    @SpirePatch(clz = RewardItem.class, method = "render",
            paramtypez = {SpriteBatch.class})
    public static class ObserveRewardItemEffectRenders {
        /**
         * True iff the call is the native single-argument, void
         * {@code AbstractGameEffect.render(SpriteBatch)} that this instrument targets. Public so
         * the targeting predicate can be unit-tested directly without javassist fakes.
         */
        public static boolean isNativeEffectRenderCall(
                String methodName, String className, String signature) {
            return "render".equals(methodName)
                    && "com.megacrit.cardcrawl.vfx.AbstractGameEffect".equals(className)
                    && "(Lcom/badlogic/gdx/graphics/g2d/SpriteBatch;)V".equals(signature);
        }

        /**
         * The replacement body emitted at the targeted call site: the observe-then-render helper
         * re-invokes the native {@code effect.render(sb)} under the bridge disposition. Public so
         * tests can assert the helper call and its {@code nativeLine} argument without running the
         * instrumentor.
         */
        public static String replacementBody(int line) {
            return "{ artframework.sts1.patch.TransientEffectContainerPatches"
                    + ".observeThenRender($0, $1, " + line + "); }";
        }

        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!isNativeEffectRenderCall(
                            call.getMethodName(), call.getClassName(), call.getSignature())) {
                        return;
                    }
                    // The helper re-invokes the native effect.render(sb) itself; no $proceed is used
                    // and no SpireReturn suppression path is introduced.
                    call.replace(replacementBody(call.getLineNumber()));
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
