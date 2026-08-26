package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.component.Rect;
import artframework.sts1.PresentSafety;

/** Typed host-boundary bridge for STS1 native render invocations. */
public final class NativeRenderBridge {
    private static final NativeRenderLedger LEDGER = new NativeRenderLedger();
    private static final TransientEffectLedger EFFECT_LEDGER = new TransientEffectLedger();
    private static final TransientEffectRegistry EFFECT_REGISTRY = new TransientEffectRegistry();
    private static final TransientEffectLifecycleAdapter EFFECT_LIFECYCLE =
            new TransientEffectLifecycleAdapter(EFFECT_LEDGER, EFFECT_REGISTRY);
    private static long nextInvocationId;

    private NativeRenderBridge() {}

    public static RenderDisposition beginSurface(String ownerId, String nativeClass,
            String nativeMethod, String sourceIdentity) {
        SurfaceDrawPlan.Entry entry = Sts1RenderPipeline.plan().find(ownerId);
        long frameId = ArtFramework.projection().lastFrameId();
        String scene = ArtFramework.projection().scene();
        NativeRenderInvocation invocation = new NativeRenderInvocation(++nextInvocationId, frameId,
                scene, ownerId, nativeClass, nativeMethod, ownerId, sourceIdentity, Rect.ZERO);
        LEDGER.recordInvocation(invocation);
        RenderDisposition disposition;
        try {
            if (PresentSafety.isPanic()) {
                disposition = RenderDisposition.failOpen(invocation.invocationId, "panic");
            } else if (entry == null) {
                LEDGER.recordUnknownOwner();
                disposition = RenderDisposition.failOpen(invocation.invocationId, "unknown_owner");
            } else if (entry.mode == SurfaceDrawPlan.DrawMode.DRAW && entry.suppressNative) {
                String entityId = Sts1NativePresentationAdapter.present(invocation);
                disposition = RenderDisposition.delegate(invocation.invocationId,
                        entry.reason, entityId);
            } else if (entry.mode == SurfaceDrawPlan.DrawMode.OBSERVE) {
                disposition = RenderDisposition.capture(invocation.invocationId, entry.reason);
            } else {
                disposition = RenderDisposition.pass(invocation.invocationId, entry.reason);
            }
        } catch (Throwable error) {
            disposition = RenderDisposition.failOpen(invocation.invocationId,
                    "bridge_error:" + error.getClass().getSimpleName());
        }
        LEDGER.recordDisposition(disposition);
        return disposition;
    }

    public static void recordSurfaceDraw(String ownerId, int drawCount) {
        recordPresentationDraw("surface:" + ownerId, ownerId, drawCount);
    }

    private static void recordPresentationDraw(String entityId, String ownerId, int drawCount) {
        long frameId = ArtFramework.projection().lastFrameId();
        java.util.List<NativeRenderInvocation> invocations = LEDGER.invocations();
        for (int index = invocations.size() - 1; index >= 0; index--) {
            NativeRenderInvocation invocation = invocations.get(index);
            if (invocation.frameId != frameId || !ownerId.equals(invocation.ownerId)) continue;
            RenderDisposition d = LEDGER.disposition(invocation.invocationId);
            if (d != null && d.mode == RenderDisposition.Mode.DELEGATE_TO_ART
                    && LEDGER.isOpen(invocation.invocationId)
                    && LEDGER.evidence(invocation.invocationId) == null) {
                LEDGER.recordEvidence(new PresentationDrawEvidence(invocation.invocationId,
                        d.presentationEntityId, frameId, drawCount, "active"));
                return;
            }
        }
        LEDGER.recordOrphanArtOutput();
    }

    public static RenderDisposition beginSkeletonRender(
            com.esotericsoftware.spine.Skeleton skeleton) {
        String owner = artframework.sts1.skeleton.Sts1SkeletonBridge.nativeEntityKey(skeleton);
        if (owner == null) {
            return RenderDisposition.pass(nextInvocationId + 1L, "native_skeleton_unclaimed");
        }
        long frameId = ArtFramework.projection().lastFrameId();
        NativeRenderInvocation invocation = new NativeRenderInvocation(++nextInvocationId, frameId,
                ArtFramework.projection().scene(), owner,
                "com.esotericsoftware.spine.SkeletonMeshRenderer", "draw", "skeleton", owner, Rect.ZERO);
        LEDGER.recordInvocation(invocation);
        RenderDisposition disposition;
        if (PresentSafety.isPanic()) {
            disposition = RenderDisposition.failOpen(invocation.invocationId, "panic");
        } else if (artframework.sts1.skeleton.Sts1SkeletonBridge.canRenderClaimedNative(skeleton)) {
            disposition = RenderDisposition.delegate(invocation.invocationId,
                    "claimed_skeleton", "skeleton:" + owner);
        } else {
            disposition = RenderDisposition.failOpen(invocation.invocationId,
                    "skeleton_renderer_unavailable");
        }
        LEDGER.recordDisposition(disposition);
        return disposition;
    }

    public static void recordSkeletonDraw(
            com.esotericsoftware.spine.Skeleton skeleton, int drawCount) {
        String owner = artframework.sts1.skeleton.Sts1SkeletonBridge.nativeEntityKey(skeleton);
        recordPresentationDraw("skeleton:" + owner, owner, drawCount);
    }

    public static void recordSkeletonFailure(com.esotericsoftware.spine.Skeleton skeleton) {
        String owner = artframework.sts1.skeleton.Sts1SkeletonBridge.nativeEntityKey(skeleton);
        if (owner == null) return;
        java.util.List<NativeRenderInvocation> invocations = LEDGER.invocations();
        for (int index = invocations.size() - 1; index >= 0; index--) {
            NativeRenderInvocation invocation = invocations.get(index);
            if (!owner.equals(invocation.ownerId)) continue;
            RenderDisposition disposition = LEDGER.disposition(invocation.invocationId);
            if (disposition != null && disposition.mode == RenderDisposition.Mode.DELEGATE_TO_ART
                    && LEDGER.isOpen(invocation.invocationId)) {
                LEDGER.recordDelegatedFallback(invocation.invocationId);
                Sts1NativePresentationAdapter.remove(owner);
                return;
            }
        }
        LEDGER.recordOrphanArtOutput();
    }

    /** Observe one effect instance without suppressing the native effect queue. */
    public static RenderDisposition beginEffectRender(
            com.megacrit.cardcrawl.vfx.AbstractGameEffect effect, String method) {
        TransientEffectIdentity identity = effectIdentity(effect);
        if (identity == null) {
            LEDGER.recordUnknownOwner();
            return RenderDisposition.failOpen(-1L, "effect_identity_unavailable");
        }
        long frameId = ArtFramework.projection().lastFrameId();
        EFFECT_LIFECYCLE.render(identity, frameId, method);
        NativeRenderInvocation invocation = new NativeRenderInvocation(++nextInvocationId, frameId,
                ArtFramework.projection().scene(), identity.instanceId,
                identity.nativeClass, method, "transient_effect", identity.instanceId, Rect.ZERO);
        LEDGER.recordInvocation(invocation);
        RenderDisposition disposition = RenderDisposition.capture(invocation.invocationId,
                "transient_effect_observe");
        LEDGER.recordDisposition(disposition);
        projectPendingEffects();
        return disposition;
    }

    public static void observeEffectUpdate(
            com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) {
        TransientEffectIdentity identity = effectIdentity(effect);
        if (identity == null) return;
        EFFECT_LIFECYCLE.create(identity);
        EFFECT_LIFECYCLE.update(identity, effect.isDone);
        projectPendingEffects();
    }

    public static void observeEffectDispose(
            com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) {
        TransientEffectIdentity identity = effectIdentity(effect);
        if (identity == null) return;
        EFFECT_LIFECYCLE.create(identity);
        EFFECT_LIFECYCLE.cancel(identity);
        projectPendingEffects();
    }

    /** Fail-open note for effect observation paths; never blocks native drawing. */
    public static void recordEffectObservationFailure() {
        EFFECT_LEDGER.recordFailOpen();
    }

    private static void projectPendingEffects() {
        try {
            ArtFramework.executeTransientEffectProjections();
        } catch (Throwable error) {
            EFFECT_LEDGER.recordFailOpen();
        }
    }

    public static NativeRenderLedger ledger() { return LEDGER; }

    public static java.util.Map<String, Object> probeSlice() {
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<String, Object>(LEDGER.probeSlice());
        out.put("transientEffects", EFFECT_LEDGER.probeSlice());
        out.put("transientEffectEntities", Integer.valueOf(EFFECT_REGISTRY.activeCount()));
        return out;
    }

    public static java.util.Map<String, Object> strictReport() {
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<String, Object>(LEDGER.strictReport());
        out.put("transientEffectUNKNOWN", Integer.valueOf(EFFECT_LEDGER.unknownLifecycleCount()));
        out.put("leakedTransientEntity", Integer.valueOf(
                EFFECT_LEDGER.leakedCount() + ((Number) out.get("leakedTransientEntity")).intValue()));
        return java.util.Collections.unmodifiableMap(out);
    }

    public static void clearTransientEffectsForRecovery() {
        EFFECT_LIFECYCLE.cleanupAll();
        LEDGER.closeForRecovery("recovery");
        Sts1NativePresentationAdapter.clear();
    }

    public static TransientEffectLedger effectLedger() { return EFFECT_LEDGER; }

    public static TransientEffectRegistry effectRegistry() { return EFFECT_REGISTRY; }

    public static void resetForTests() {
        nextInvocationId = 0L;
        LEDGER.clear();
        EFFECT_LEDGER.reset();
        EFFECT_REGISTRY.clear();
        Sts1NativePresentationAdapter.clear();
    }

    private static TransientEffectIdentity effectIdentity(
            com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) {
        if (effect == null) return null;
        String nativeClass = effect.getClass().getName();
        int hash = System.identityHashCode(effect);
        return new TransientEffectIdentity(nativeClass + "@" + Integer.toHexString(hash),
                nativeClass, hash, 0L);
    }
}
