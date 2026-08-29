package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.component.Rect;
import artframework.sts1.PresentSafety;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Typed host-boundary bridge for STS1 native render invocations. */
public final class NativeRenderBridge {
    private static final NativeRenderLedger LEDGER = new NativeRenderLedger();
    private static final TransientEffectLedger EFFECT_LEDGER = new TransientEffectLedger();
    private static final TransientEffectRegistry EFFECT_REGISTRY = new TransientEffectRegistry();
    private static final TransientEffectLifecycleAdapter EFFECT_LIFECYCLE =
            new TransientEffectLifecycleAdapter(EFFECT_LEDGER, EFFECT_REGISTRY);
    private static long nextInvocationId;
    private static long lastProjectionFrameId = -1L;
    private static final Map<String, ArrayDeque<Long>> SURFACE_INVOCATIONS =
            new HashMap<String, ArrayDeque<Long>>();
    private static final Map<String, ArrayDeque<Long>> SKELETON_INVOCATIONS =
            new HashMap<String, ArrayDeque<Long>>();

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
        if (disposition.mode == RenderDisposition.Mode.DELEGATE_TO_ART) {
            synchronized (SURFACE_INVOCATIONS) {
                ArrayDeque<Long> ids = SURFACE_INVOCATIONS.get(ownerId);
                if (ids == null) {
                    ids = new ArrayDeque<Long>();
                    SURFACE_INVOCATIONS.put(ownerId, ids);
                }
                ids.addLast(Long.valueOf(invocation.invocationId));
            }
        } else {
            cancelPendingSurfaceInvocations(ownerId);
        }
        return disposition;
    }

    public static void recordSurfaceDraw(String ownerId, int drawCount) {
        List<Long> ids = drainSurfaceInvocations(ownerId);
        if (ids.isEmpty()) {
            LEDGER.recordOrphanArtOutput();
            return;
        }
        for (Long id : ids) {
            recordSurfaceDrawEvidence(id.longValue(), drawCount);
        }
    }

    /** Renderer-side evidence hook; a native/off-transition callback is not ART output. */
    public static void recordSurfaceDrawIfPending(String ownerId, int drawCount) {
        List<Long> ids = drainSurfaceInvocations(ownerId);
        if (ids.isEmpty()) return;
        for (Long id : ids) recordSurfaceDrawEvidence(id.longValue(), drawCount);
    }

    /**
     * Records surface evidence only when both the surface and invocation token correlate.
     * Rejected correlation is diagnostic input, not ART output, so it must not consume or
     * invalidate a pending invocation.
     */
    public static void recordSurfaceDraw(String ownerId, long invocationId, int drawCount) {
        NativeRenderInvocation invocation = LEDGER.invocation(invocationId);
        if (invocation == null || !sameSurface(ownerId, invocation.ownerId)) {
            return;
        }
        recordSurfaceDrawEvidence(invocationId, drawCount);
        removeSurfaceInvocation(invocation.ownerId, invocationId);
    }

    /** Preferred API: evidence is correlated by the invocation token returned by beginSurface. */
    public static void recordSurfaceDraw(long invocationId, int drawCount) {
        NativeRenderInvocation invocation = LEDGER.invocation(invocationId);
        recordSurfaceDrawEvidence(invocationId, drawCount);
        if (invocation != null) removeSurfaceInvocation(invocation.ownerId, invocationId);
    }

    private static void recordSurfaceDrawEvidence(long invocationId, int drawCount) {
        RenderDisposition disposition = LEDGER.disposition(invocationId);
        if (disposition == null || disposition.mode != RenderDisposition.Mode.DELEGATE_TO_ART) {
            LEDGER.recordOrphanArtOutput();
            return;
        }
        NativeRenderInvocation invocation = LEDGER.invocation(invocationId);
        LEDGER.recordEvidence(invocationId, disposition.presentationEntityId,
                invocation.frameId, drawCount, "active");
    }

    private static List<Long> drainSurfaceInvocations(String ownerId) {
        synchronized (SURFACE_INVOCATIONS) {
            ArrayDeque<Long> ids = SURFACE_INVOCATIONS.remove(ownerId);
            if (ids == null || ids.isEmpty()) return Collections.emptyList();
            return new ArrayList<Long>(ids);
        }
    }

    private static void removeSurfaceInvocation(String ownerId, long invocationId) {
        synchronized (SURFACE_INVOCATIONS) {
            ArrayDeque<Long> ids = SURFACE_INVOCATIONS.get(ownerId);
            if (ids == null) return;
            ids.remove(Long.valueOf(invocationId));
            if (ids.isEmpty()) SURFACE_INVOCATIONS.remove(ownerId);
        }
    }

    private static void cancelPendingSurfaceInvocations(String ownerId) {
        List<Long> ids;
        synchronized (SURFACE_INVOCATIONS) {
            ArrayDeque<Long> queued = SURFACE_INVOCATIONS.remove(ownerId);
            if (queued == null || queued.isEmpty()) return;
            ids = new ArrayList<Long>(queued);
        }
        for (Long id : ids) {
            if (LEDGER.isOpen(id.longValue())) {
                LEDGER.cancelForTransition(id.longValue());
            }
        }
    }

    private static boolean sameSurface(String left, String right) {
        String canonicalLeft = artframework.context.SurfaceIds.canonicalize(left);
        String canonicalRight = artframework.context.SurfaceIds.canonicalize(right);
        return canonicalLeft == null ? canonicalRight == null : canonicalLeft.equals(canonicalRight);
    }

    private static Long takeSkeletonInvocation(String ownerId) {
        if (ownerId == null) return null;
        synchronized (SKELETON_INVOCATIONS) {
            ArrayDeque<Long> ids = SKELETON_INVOCATIONS.get(ownerId);
            if (ids == null || ids.isEmpty()) return null;
            Long id = ids.removeFirst();
            if (ids.isEmpty()) SKELETON_INVOCATIONS.remove(ownerId);
            return id;
        }
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
        if (disposition.mode == RenderDisposition.Mode.DELEGATE_TO_ART) {
            synchronized (SKELETON_INVOCATIONS) {
                ArrayDeque<Long> ids = SKELETON_INVOCATIONS.get(owner);
                if (ids == null) {
                    ids = new ArrayDeque<Long>();
                    SKELETON_INVOCATIONS.put(owner, ids);
                }
                ids.addLast(Long.valueOf(invocation.invocationId));
            }
        }
        return disposition;
    }

    public static void recordSkeletonDraw(
            com.esotericsoftware.spine.Skeleton skeleton, int drawCount) {
        String owner = artframework.sts1.skeleton.Sts1SkeletonBridge.nativeEntityKey(skeleton);
        Long id = takeSkeletonInvocation(owner);
        if (id == null) {
            LEDGER.recordOrphanArtOutput();
            return;
        }
        recordSkeletonDraw(id.longValue(), drawCount);
    }

    public static void recordSkeletonDraw(long invocationId, int drawCount) {
        RenderDisposition disposition = LEDGER.disposition(invocationId);
        if (disposition == null || disposition.mode != RenderDisposition.Mode.DELEGATE_TO_ART) {
            LEDGER.recordOrphanArtOutput();
            return;
        }
        NativeRenderInvocation invocation = LEDGER.invocation(invocationId);
        LEDGER.recordEvidence(invocationId, disposition.presentationEntityId,
                invocation.frameId, drawCount, "active");
    }

    public static void recordSkeletonFailure(com.esotericsoftware.spine.Skeleton skeleton) {
        String owner = artframework.sts1.skeleton.Sts1SkeletonBridge.nativeEntityKey(skeleton);
        Long id = takeSkeletonInvocation(owner);
        if (id == null) {
            LEDGER.recordOrphanArtOutput();
            return;
        }
        recordSkeletonFailure(id.longValue());
    }

    public static void recordSkeletonFailure(long invocationId) {
        NativeRenderInvocation invocation = LEDGER.invocation(invocationId);
        RenderDisposition disposition = LEDGER.disposition(invocationId);
        if (invocation == null || disposition == null
                || disposition.mode != RenderDisposition.Mode.DELEGATE_TO_ART) {
            LEDGER.recordOrphanArtOutput();
            return;
        }
        LEDGER.recordDelegatedFallback(invocationId);
        Sts1NativePresentationAdapter.remove(invocation.ownerId);
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
        projectPendingEffectsOncePerFrame();
        return disposition;
    }

    public static void observeEffectUpdate(
            com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) {
        TransientEffectIdentity identity = effectIdentity(effect);
        if (identity == null) return;
        EFFECT_LIFECYCLE.create(identity);
        EFFECT_LIFECYCLE.update(identity, effect.isDone);
        projectPendingEffectsOncePerFrame();
    }

    public static void observeEffectDispose(
            com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) {
        TransientEffectIdentity identity = effectIdentity(effect);
        if (identity == null) return;
        EFFECT_LIFECYCLE.create(identity);
        EFFECT_LIFECYCLE.cancel(identity);
        projectPendingEffectsOncePerFrame();
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

    private static void projectPendingEffectsOncePerFrame() {
        long frameId = ArtFramework.projection().lastFrameId();
        if (frameId == lastProjectionFrameId) {
            return;
        }
        lastProjectionFrameId = frameId;
        projectPendingEffects();
    }

    public static NativeRenderLedger ledger() { return LEDGER; }

    public static java.util.Map<String, Object> probeSlice() {
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<String, Object>(LEDGER.probeSlice());
        Map<String, Integer> pendingByOwner = pendingSurfaceInvocationsByOwner();
        int pendingCount = 0;
        for (Integer count : pendingByOwner.values()) pendingCount += count.intValue();
        out.put("pendingSurfaceInvocationCount", Integer.valueOf(pendingCount));
        out.put("pendingSurfaceInvocationsByOwner", pendingByOwner);
        out.put("transientEffects", EFFECT_LEDGER.probeSlice());
        out.put("transientEffectEntities", Integer.valueOf(EFFECT_REGISTRY.activeCount()));
        return out;
    }

    private static Map<String, Integer> pendingSurfaceInvocationsByOwner() {
        synchronized (SURFACE_INVOCATIONS) {
            Map<String, Integer> snapshot = new TreeMap<String, Integer>();
            for (Map.Entry<String, ArrayDeque<Long>> entry : SURFACE_INVOCATIONS.entrySet()) {
                snapshot.put(entry.getKey(), Integer.valueOf(entry.getValue().size()));
            }
            return Collections.unmodifiableMap(new LinkedHashMap<String, Integer>(snapshot));
        }
    }

    public static java.util.Map<String, Object> strictReport() {
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<String, Object>(LEDGER.strictReport());
        out.put("transientEffectUNKNOWN", Integer.valueOf(EFFECT_LEDGER.unknownLifecycleCount()));
        out.put("leakedTransientEntity", Integer.valueOf(
                EFFECT_LEDGER.leakedCount() + ((Number) out.get("leakedTransientEntity")).intValue()));
        boolean accepted = Boolean.TRUE.equals(out.get("accepted"))
                && ((Number) out.get("transientEffectUNKNOWN")).intValue() == 0
                && ((Number) out.get("leakedTransientEntity")).intValue() == 0;
        out.put("accepted", Boolean.valueOf(accepted));
        return java.util.Collections.unmodifiableMap(out);
    }

    public static boolean isStrictlyAccepted() {
        return Boolean.TRUE.equals(strictReport().get("accepted"));
    }

    public static void clearTransientEffectsForRecovery() {
        EFFECT_LIFECYCLE.cleanupForRecovery();
        LEDGER.closeForRecovery("recovery");
        synchronized (SURFACE_INVOCATIONS) { SURFACE_INVOCATIONS.clear(); }
        synchronized (SKELETON_INVOCATIONS) { SKELETON_INVOCATIONS.clear(); }
        Sts1NativePresentationAdapter.clear();
    }

    public static TransientEffectLedger effectLedger() { return EFFECT_LEDGER; }

    public static TransientEffectRegistry effectRegistry() { return EFFECT_REGISTRY; }

    public static void resetForTests() {
        nextInvocationId = 0L;
        lastProjectionFrameId = -1L;
        LEDGER.clear();
        EFFECT_LEDGER.reset();
        EFFECT_REGISTRY.clear();
        synchronized (SURFACE_INVOCATIONS) { SURFACE_INVOCATIONS.clear(); }
        synchronized (SKELETON_INVOCATIONS) { SKELETON_INVOCATIONS.clear(); }
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
