package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.component.Rect;
import artframework.render.NativeRenderOwnership;
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
    private static final NativeFilterScope FILTER_SCOPE = new NativeFilterScope();
    private static final NativeRenderPolicy POLICY = new NativeRenderPolicy(new Runnable() {
        @Override public void run() { refreshPolicyProjection(); }
    });
    private static long nextInvocationId;
    private static final Object BRIDGE_LOCK = new Object();
    private static Runnable beforeTokenPublicationForTests;
    private static long lastProjectionFrameId = -1L;
    private static final Map<String, ArrayDeque<Long>> SURFACE_INVOCATIONS =
            new HashMap<String, ArrayDeque<Long>>();
    private static final Map<String, ArrayDeque<Long>> SKELETON_INVOCATIONS =
            new HashMap<String, ArrayDeque<Long>>();
    private static final Map<String, ArrayDeque<Long>> STANCE_INVOCATIONS =
            new HashMap<String, ArrayDeque<Long>>();
    private static final Map<String, Long> NATIVE_CONTINUATION_FRAMES =
            new HashMap<String, Long>();

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
        // Ownership actually written to the projected ECS native render input, so a final ledger
        // disposition that differs from the proposal (recovery fail-open won the race) can be
        // reconciled without letting the ECS input overstate pixel ownership.
        NativeRenderOwnership projectedOwnership = null;
        try {
            if (PresentSafety.isPanic()) {
                disposition = RenderDisposition.failOpen(invocation.invocationId, "panic");
            } else if (entry == null) {
                LEDGER.recordUnknownOwner();
                if (BackgroundOnlyGate.isActive()) {
                    BackgroundOnlyGate.recordUnsupported("surface:unknown_owner");
                }
                disposition = RenderDisposition.failOpen(invocation.invocationId, "unknown_owner");
            } else if (BackgroundOnlyGate.isActive()
                    && !BackgroundRenderGate.BACKGROUND_FAMILY.equals(invocation.surfaceFamily)) {
                BackgroundOnlyGate.recordBlocked("surface:" + invocation.surfaceFamily);
                disposition = RenderDisposition.blocked(invocation.invocationId,
                        "background_only:" + invocation.surfaceFamily);
            } else if (entry.mode == SurfaceDrawPlan.DrawMode.DRAW && entry.suppressNative) {
                if (FILTER_SCOPE.blocksDelegation(invocation.surfaceFamily)) {
                    // Family is filtered and not selected: narrow delegation back to native. The
                    // filter scope stays strictly downgrade-only and wins over isolate, so a
                    // filtered family never becomes a suppression target. Native pixels stay
                    // authoritative, so the ECS input is an overlay-only claim.
                    projectedOwnership = Sts1NativePresentationAdapter.ownershipFor(
                            RenderDisposition.Mode.PASS_THROUGH);
                    Sts1NativePresentationAdapter.present(invocation, projectedOwnership);
                    disposition = RenderDisposition.pass(invocation.invocationId,
                            "filter_scope:" + invocation.surfaceFamily);
                } else if (POLICY.isIsolateActive() && !POLICY.exempt(invocation)) {
                    POLICY.recordIsolated();
                    projectedOwnership = NativeRenderOwnership.DELEGATED_TO_ART;
                    String entityId = Sts1NativePresentationAdapter.present(
                            invocation, projectedOwnership);
                    disposition = RenderDisposition.delegate(invocation.invocationId,
                            "isolate:" + entry.reason, entityId);
                } else if (POLICY.isIsolateActive() && POLICY.exempt(invocation)) {
                    // Exemption keeps native continuation, but still projects the invocation so the
                    // ECS exemption view (NativeRenderExemptionComponent) reflects the matched
                    // target even before/without a delegated frame. No ledger token is published.
                    projectedOwnership = NativeRenderOwnership.NATIVE_WITH_ART_OVERLAY;
                    String entityId = Sts1NativePresentationAdapter.present(
                            invocation, projectedOwnership);
                    disposition = RenderDisposition.pass(invocation.invocationId,
                            "exempt:native_continuation");
                    POLICY.recordExempted(disposition.nativeContinuation);
                } else {
                    projectedOwnership = NativeRenderOwnership.DELEGATED_TO_ART;
                    disposition = normalSurfaceDecision(entry, invocation, projectedOwnership);
                }
            } else if (entry.mode == SurfaceDrawPlan.DrawMode.OBSERVE) {
                // OBSERVE only captures: native pixels stay authoritative and no ECS input is
                // written. This native render callback runs outside PresentationSchedule.advance,
                // so projecting here would force a synchronous full RenderPlan rebuild for every
                // observed surface callback in the default OFF/OBSERVE configurations.
                disposition = RenderDisposition.capture(invocation.invocationId, entry.reason);
            } else {
                // Known surface that keeps native pixels this frame (OFF/SKIP/DRAW-without-suppress):
                // pass through without writing ECS input, for the same out-of-schedule rebuild
                // reason. A surface delegated on an earlier frame keeps its (delegated) input until
                // the recovery/scene cleanup withdraws it; OFF/OBSERVE never fabricate a claim.
                disposition = RenderDisposition.pass(invocation.invocationId, entry.reason);
            }
        } catch (Throwable error) {
            if (BackgroundOnlyGate.isActive()) {
                BackgroundOnlyGate.recordUnsupported("surface:bridge_error");
            }
            disposition = RenderDisposition.failOpen(invocation.invocationId,
                    "bridge_error:" + error.getClass().getSimpleName());
        }
        synchronized (BRIDGE_LOCK) {
            disposition = LEDGER.recordDispositionOrRecovery(disposition);
            if (disposition.nativeContinuation) {
                NATIVE_CONTINUATION_FRAMES.put(ownerId, Long.valueOf(frameId));
            } else {
                NATIVE_CONTINUATION_FRAMES.remove(ownerId);
            }
            if (beforeTokenPublicationForTests != null) beforeTokenPublicationForTests.run();
            if (disposition.mode == RenderDisposition.Mode.DELEGATE_TO_ART
                    && LEDGER.isPendingDelegated(invocation.invocationId)) {
                ArrayDeque<Long> ids = SURFACE_INVOCATIONS.get(ownerId);
                if (ids == null) {
                    ids = new ArrayDeque<Long>();
                    SURFACE_INVOCATIONS.put(ownerId, ids);
                }
                ids.addLast(Long.valueOf(invocation.invocationId));
            } else {
                cancelPendingSurfaceInvocationsLocked(ownerId);
            }
        }
        NativeRenderOwnership finalOwnership =
                Sts1NativePresentationAdapter.ownershipFor(disposition.mode);
        if (projectedOwnership != null && finalOwnership != projectedOwnership) {
            // Reconcile only an input this frame actually projected. A recovery fail-open that won
            // the race downgrades the already-projected delegated claim. Non-projecting paths
            // (panic / unknown / blocked / OBSERVE / OFF pass) never write ECS input here, so a
            // surface that keeps native pixels cannot fabricate or revive a retained target.
            Sts1NativePresentationAdapter.projectInput(invocation, finalOwnership);
        }
        return disposition;
    }

    public static void recordSurfaceDraw(String ownerId, int drawCount) {
        synchronized (BRIDGE_LOCK) {
            List<Long> ids = drainSurfaceInvocations(ownerId);
            if (ids.isEmpty()) {
                LEDGER.recordOrphanArtOutput();
                return;
            }
            for (Long id : ids) recordSurfaceDrawEvidenceLocked(id.longValue(), drawCount);
        }
    }

    /** Renderer-side evidence hook; a native/off-transition callback is not ART output. */
    public static void recordSurfaceDrawIfPending(String ownerId, int drawCount) {
        synchronized (BRIDGE_LOCK) {
            List<Long> ids = drainSurfaceInvocations(ownerId);
            if (ids.isEmpty()) return;
            for (Long id : ids) recordSurfaceDrawEvidenceLocked(id.longValue(), drawCount);
        }
    }

    /**
     * Records surface evidence only when both the surface and invocation token correlate.
     * Rejected correlation is diagnostic input, not ART output, so it must not consume or
     * invalidate a pending invocation.
     */
    public static void recordSurfaceDraw(String ownerId, long invocationId, int drawCount) {
        synchronized (BRIDGE_LOCK) {
            NativeRenderInvocation invocation = LEDGER.invocation(invocationId);
            if (invocation == null) return;
            if (!sameSurface(ownerId, invocation.ownerId)) return;
            recordSurfaceDrawEvidenceLocked(invocationId, drawCount);
            removeSurfaceInvocation(invocation.ownerId, invocationId);
        }
    }

    /** Preferred API: evidence is correlated by the invocation token returned by beginSurface. */
    public static void recordSurfaceDraw(long invocationId, int drawCount) {
        synchronized (BRIDGE_LOCK) {
            NativeRenderInvocation invocation = LEDGER.invocation(invocationId);
            recordSurfaceDrawEvidenceLocked(invocationId, drawCount);
            if (invocation != null) removeSurfaceInvocation(invocation.ownerId, invocationId);
        }
    }

    private static void recordSurfaceDrawEvidence(long invocationId, int drawCount) {
        synchronized (BRIDGE_LOCK) { recordSurfaceDrawEvidenceLocked(invocationId, drawCount); }
    }

    private static void recordSurfaceDrawEvidenceLocked(long invocationId, int drawCount) {
        if (!LEDGER.recordDelegatedEvidence(invocationId, drawCount, "active")) {
            LEDGER.recordOrphanArtOutput();
        }
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
        synchronized (BRIDGE_LOCK) {
            cancelPendingSurfaceInvocationsLocked(ownerId);
        }
    }

    private static void cancelPendingSurfaceInvocationsLocked(String ownerId) {
        List<Long> ids;
        synchronized (SURFACE_INVOCATIONS) {
            ArrayDeque<Long> queued = SURFACE_INVOCATIONS.remove(ownerId);
            if (queued == null || queued.isEmpty()) return;
            ids = new ArrayList<Long>(queued);
        }
        for (Long id : ids) if (LEDGER.isOpen(id.longValue())) LEDGER.cancelForTransition(id.longValue());
    }

    private static boolean sameSurface(String left, String right) {
        String canonicalLeft = artframework.context.SurfaceIds.canonicalize(left);
        String canonicalRight = artframework.context.SurfaceIds.canonicalize(right);
        return canonicalLeft == null ? canonicalRight == null : canonicalLeft.equals(canonicalRight);
    }

    private static Long peekSkeletonInvocation(String ownerId) {
        if (ownerId == null) return null;
        synchronized (SKELETON_INVOCATIONS) {
            ArrayDeque<Long> ids = SKELETON_INVOCATIONS.get(ownerId);
            if (ids == null || ids.isEmpty()) return null;
            return ids.peekFirst();
        }
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

    /** Retire only the exact ID callback token; object callbacks retain FIFO compatibility. */
    private static void removeSkeletonInvocation(String ownerId, long invocationId) {
        if (ownerId == null) return;
        synchronized (SKELETON_INVOCATIONS) {
            ArrayDeque<Long> ids = SKELETON_INVOCATIONS.get(ownerId);
            if (ids == null) return;
            ids.remove(Long.valueOf(invocationId));
            if (ids.isEmpty()) SKELETON_INVOCATIONS.remove(ownerId);
        }
    }

    private static void removeSkeletonInvocation(long invocationId) {
        synchronized (SKELETON_INVOCATIONS) {
            List<String> emptyOwners = new ArrayList<String>();
            for (Map.Entry<String, ArrayDeque<Long>> entry : SKELETON_INVOCATIONS.entrySet()) {
                entry.getValue().remove(Long.valueOf(invocationId));
                if (entry.getValue().isEmpty()) emptyOwners.add(entry.getKey());
            }
            for (String owner : emptyOwners) SKELETON_INVOCATIONS.remove(owner);
        }
    }

    public static RenderDisposition beginSkeletonRender(
            com.esotericsoftware.spine.Skeleton skeleton) {
        String owner = artframework.sts1.skeleton.Sts1SkeletonBridge.nativeEntityKey(skeleton);
        if (owner == null) {
            if (BackgroundOnlyGate.isActive()) {
                BackgroundOnlyGate.recordUnsupported("skeleton:unclaimed");
                return RenderDisposition.failOpen(nextInvocationId + 1L, "native_skeleton_unclaimed");
            }
            return RenderDisposition.pass(nextInvocationId + 1L, "native_skeleton_unclaimed");
        }
        long frameId = ArtFramework.projection().lastFrameId();
        NativeRenderInvocation invocation = new NativeRenderInvocation(++nextInvocationId, frameId,
                ArtFramework.projection().scene(), owner,
                "com.esotericsoftware.spine.SkeletonMeshRenderer", "draw", "skeleton-runtime", owner, Rect.ZERO);
        LEDGER.recordInvocation(invocation);
        RenderDisposition disposition;
        if (PresentSafety.isPanic()) {
            disposition = RenderDisposition.failOpen(invocation.invocationId, "panic");
        } else if (BackgroundOnlyGate.isActive()) {
            if (artframework.sts1.skeleton.Sts1SkeletonBridge.canRenderClaimedNative(skeleton)) {
                BackgroundOnlyGate.recordBlocked("skeleton:" + owner);
                disposition = RenderDisposition.blocked(invocation.invocationId,
                        "background_only:skeleton");
            } else {
                BackgroundOnlyGate.recordUnsupported("skeleton:renderer_unavailable");
                disposition = RenderDisposition.failOpen(invocation.invocationId,
                        "skeleton_renderer_unavailable");
            }
        } else if (artframework.sts1.skeleton.Sts1SkeletonBridge.canRenderClaimedNative(skeleton)) {
            if (POLICY.isIsolateActive() && !POLICY.exempt(invocation)) {
                POLICY.recordIsolated();
            } else if (POLICY.isIsolateActive()) {
                POLICY.recordExempted(true);
            }
            disposition = POLICY.isIsolateActive() && POLICY.exempt(invocation)
                    ? RenderDisposition.pass(invocation.invocationId, "exempt:native_continuation")
                    : RenderDisposition.delegate(invocation.invocationId,
                            POLICY.isIsolateActive() ? "isolate:claimed_skeleton" : "claimed_skeleton",
                            "skeleton:" + owner);
        } else {
            disposition = RenderDisposition.failOpen(invocation.invocationId,
                    "skeleton_renderer_unavailable");
        }
        synchronized (BRIDGE_LOCK) {
            disposition = LEDGER.recordDispositionOrRecovery(disposition);
            if (beforeTokenPublicationForTests != null) beforeTokenPublicationForTests.run();
            if (disposition.mode == RenderDisposition.Mode.DELEGATE_TO_ART
                    && LEDGER.isPendingDelegated(invocation.invocationId)) {
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
        synchronized (BRIDGE_LOCK) {
            String owner = artframework.sts1.skeleton.Sts1SkeletonBridge.nativeEntityKey(skeleton);
            Long id = takeSkeletonInvocation(owner);
            if (id == null) {
                LEDGER.recordOrphanArtOutput();
                return;
            }
            recordSkeletonDraw(id.longValue(), drawCount);
        }
    }

    public static void recordSkeletonDraw(long invocationId, int drawCount) {
        synchronized (BRIDGE_LOCK) {
            if (LEDGER.recordDelegatedEvidence(invocationId, drawCount, "active")) {
                removeSkeletonInvocation(invocationId);
            } else {
                LEDGER.recordOrphanArtOutput();
            }
        }
    }

    public static void recordSkeletonFailure(com.esotericsoftware.spine.Skeleton skeleton) {
        synchronized (BRIDGE_LOCK) {
            String owner = artframework.sts1.skeleton.Sts1SkeletonBridge.nativeEntityKey(skeleton);
            Long id = peekSkeletonInvocation(owner);
            if (id == null || !LEDGER.recordDelegatedFallbackIfPending(id.longValue())) {
                if (id != null) removeSkeletonInvocation(owner, id.longValue());
                LEDGER.recordOrphanArtOutput();
                return;
            }
            removeSkeletonInvocation(owner, id.longValue());
            Sts1NativePresentationAdapter.remove(owner);
        }
    }

    public static void recordSkeletonFailure(long invocationId) {
        synchronized (BRIDGE_LOCK) {
            if (!LEDGER.recordDelegatedFallbackIfPending(invocationId)) {
                removeSkeletonInvocation(invocationId);
                LEDGER.recordOrphanArtOutput();
                return;
            }
            NativeRenderInvocation invocation = LEDGER.invocation(invocationId);
            if (invocation != null) {
                Sts1NativePresentationAdapter.remove(invocation.ownerId);
                removeSkeletonInvocation(invocation.ownerId, invocationId);
            } else {
                removeSkeletonInvocation(invocationId);
            }
        }
    }

    /**
     * Stance-family native render seam. While the delegation gate is off the native
     * {@code AbstractStance.render} pixels continue unchanged; once enabled and ART stance
     * drawing is ready the invocation is delegated to ART.
     */
    public static RenderDisposition beginStanceRender(
            com.megacrit.cardcrawl.stances.AbstractStance stance) {
        String owner = stanceOwner(stance);
        long frameId = ArtFramework.projection().lastFrameId();
        NativeRenderInvocation invocation = new NativeRenderInvocation(++nextInvocationId, frameId,
                ArtFramework.projection().scene(), owner,
                "com.megacrit.cardcrawl.stances.AbstractStance", "render",
                artframework.context.SurfaceIds.STANCE, owner, Rect.ZERO);
        LEDGER.recordInvocation(invocation);
        RenderDisposition disposition;
        try {
            if (PresentSafety.isPanic()) {
                disposition = RenderDisposition.failOpen(invocation.invocationId, "panic");
            } else if (BackgroundOnlyGate.isActive()) {
                disposition = RenderDisposition.failOpen(invocation.invocationId,
                        "background_only:stance");
            } else if (!StanceDelegationGate.isActive()) {
                disposition = RenderDisposition.pass(invocation.invocationId,
                        "native_continuation");
            } else if (!StanceArtRenderer.isReady(owner)) {
                disposition = RenderDisposition.failOpen(invocation.invocationId,
                        "stance_art_not_ready");
            } else {
                disposition = RenderDisposition.delegate(invocation.invocationId,
                        "stance_delegate", owner);
            }
        } catch (Throwable error) {
            disposition = RenderDisposition.failOpen(invocation.invocationId,
                    "bridge_error:" + error.getClass().getSimpleName());
        }
        synchronized (BRIDGE_LOCK) {
            disposition = LEDGER.recordDispositionOrRecovery(disposition);
            if (beforeTokenPublicationForTests != null) beforeTokenPublicationForTests.run();
            if (disposition.mode == RenderDisposition.Mode.DELEGATE_TO_ART
                    && LEDGER.isPendingDelegated(invocation.invocationId)) {
                ArrayDeque<Long> ids = STANCE_INVOCATIONS.get(owner);
                if (ids == null) {
                    ids = new ArrayDeque<Long>();
                    STANCE_INVOCATIONS.put(owner, ids);
                }
                ids.addLast(Long.valueOf(invocation.invocationId));
            }
        }
        return disposition;
    }

    /** Canonical ART stance owner key ({@code "stance:" + ID}) shared by bridge and patch. */
    public static String stanceOwner(com.megacrit.cardcrawl.stances.AbstractStance stance) {
        if (stance == null) return "stance:unknown";
        String id = stance.ID;
        String suffix = (id != null && !id.isEmpty()) ? id : stance.getClass().getSimpleName();
        return "stance:" + suffix;
    }

    static Long takeStanceInvocation(String owner) {
        if (owner == null) return null;
        synchronized (STANCE_INVOCATIONS) {
            ArrayDeque<Long> ids = STANCE_INVOCATIONS.get(owner);
            if (ids == null || ids.isEmpty()) return null;
            Long id = ids.removeFirst();
            if (ids.isEmpty()) STANCE_INVOCATIONS.remove(owner);
            return id;
        }
    }

    public static void recordStanceDraw(long invocationId, int drawCount) {
        synchronized (BRIDGE_LOCK) {
            if (LEDGER.recordDelegatedEvidence(invocationId, drawCount, "active")) {
                removeStanceInvocation(invocationId);
            } else {
                LEDGER.recordOrphanArtOutput();
            }
        }
    }

    public static void recordStanceFailure(long invocationId) {
        synchronized (BRIDGE_LOCK) {
            if (!LEDGER.recordDelegatedFallbackIfPending(invocationId)) {
                removeStanceInvocation(invocationId);
                LEDGER.recordOrphanArtOutput();
                return;
            }
            NativeRenderInvocation invocation = LEDGER.invocation(invocationId);
            if (invocation != null) {
                Sts1NativePresentationAdapter.remove(invocation.ownerId);
                removeStanceInvocation(invocation.ownerId, invocationId);
            } else {
                removeStanceInvocation(invocationId);
            }
        }
    }

    private static void removeStanceInvocation(String ownerId, long invocationId) {
        if (ownerId == null) return;
        synchronized (STANCE_INVOCATIONS) {
            ArrayDeque<Long> ids = STANCE_INVOCATIONS.get(ownerId);
            if (ids == null) return;
            ids.remove(Long.valueOf(invocationId));
            if (ids.isEmpty()) STANCE_INVOCATIONS.remove(ownerId);
        }
    }

    private static void removeStanceInvocation(long invocationId) {
        synchronized (STANCE_INVOCATIONS) {
            List<String> emptyOwners = new ArrayList<String>();
            for (Map.Entry<String, ArrayDeque<Long>> entry : STANCE_INVOCATIONS.entrySet()) {
                entry.getValue().remove(Long.valueOf(invocationId));
                if (entry.getValue().isEmpty()) emptyOwners.add(entry.getKey());
            }
            for (String owner : emptyOwners) STANCE_INVOCATIONS.remove(owner);
        }
    }

    /** Observe one effect instance without suppressing the native effect queue. */
    public static RenderDisposition beginEffectRender(
            com.megacrit.cardcrawl.vfx.AbstractGameEffect effect, String method) {
        if (PresentSafety.isPanic()) {
            return RenderDisposition.failOpen(-1L, "panic");
        }
        TransientEffectIdentity identity = effectIdentity(effect);
        if (identity == null) {
            LEDGER.recordUnknownOwner();
            if (BackgroundOnlyGate.isActive()) {
                BackgroundOnlyGate.recordUnsupported("effect:identity_unavailable");
            }
            return RenderDisposition.failOpen(-1L, "effect_identity_unavailable");
        }
        long frameId = ArtFramework.projection().lastFrameId();
        EFFECT_LIFECYCLE.render(identity, frameId, method);
        NativeRenderInvocation invocation = new NativeRenderInvocation(++nextInvocationId, frameId,
                ArtFramework.projection().scene(), identity.instanceId,
                identity.nativeClass, method, "vfx-misc-root", identity.instanceId, Rect.ZERO);
        LEDGER.recordInvocation(invocation);
        if (BackgroundOnlyGate.isActive()) {
            BackgroundOnlyGate.recordBlocked("effect:" + identity.nativeClass);
            return LEDGER.recordDispositionOrRecovery(RenderDisposition.blocked(
                    invocation.invocationId, "background_only:effect"));
        }
        boolean isolated = POLICY.isIsolateActive();
        boolean exempt = POLICY.exempt(invocation);
        if (isolated && !exempt) {
            POLICY.recordIsolated();
        } else if (isolated) {
            POLICY.recordExempted(true);
        }
        RenderDisposition disposition = isolated && !exempt
                ? RenderDisposition.delegate(invocation.invocationId, "isolate:transient_effect",
                        "effect:" + identity.instanceId)
                : RenderDisposition.capture(invocation.invocationId, "transient_effect_observe");
        disposition = LEDGER.recordDispositionOrRecovery(disposition);
        if (disposition.mode == RenderDisposition.Mode.DELEGATE_TO_ART
                && LEDGER.completeDelegatedWithoutEvidence(invocation.invocationId)) {
            // Effect projection has no host draw callback. Complete the delegated lifecycle
            // without manufacturing pixel evidence; native continuation remains suppressed.
            LEDGER.recordNoPixelIsolation();
        }
        projectPendingEffectsOncePerFrame();
        return disposition;
    }

    public static void observeEffectUpdate(
            com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) {
        if (PresentSafety.isPanic()) return;
        TransientEffectIdentity identity = effectIdentity(effect);
        if (identity == null) return;
        EFFECT_LIFECYCLE.update(identity, effect.isDone);
        projectPendingEffectsOncePerFrame();
    }

    public static void observeEffectDispose(
            com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) {
        if (PresentSafety.isPanic()) return;
        TransientEffectIdentity identity = effectIdentity(effect);
        if (identity == null) return;
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

    /** Whether the current projected frame retained native pixel authority for a surface. */
    public static boolean nativeContinuationForSurface(String ownerId) {
        if (ownerId == null) return false;
        long frameId = ArtFramework.projection().lastFrameId();
        synchronized (BRIDGE_LOCK) {
            Long continuationFrame = NATIVE_CONTINUATION_FRAMES.get(ownerId);
            return continuationFrame != null && continuationFrame.longValue() == frameId;
        }
    }

    public static java.util.Map<String, Object> probeSlice() {
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<String, Object>(LEDGER.probeSlice());
        Map<String, Integer> pendingByOwner = pendingSurfaceInvocationsByOwner();
        int pendingCount = 0;
        for (Integer count : pendingByOwner.values()) pendingCount += count.intValue();
        out.put("pendingSurfaceInvocationCount", Integer.valueOf(pendingCount));
        out.put("pendingSurfaceInvocationsByOwner", pendingByOwner);
        out.put("pendingSkeletonInvocationCount", Integer.valueOf(pendingSkeletonInvocationCount()));
        out.put("transientEffects", EFFECT_LEDGER.probeSlice());
        out.put("transientEffectEntities", Integer.valueOf(EFFECT_REGISTRY.activeCount()));
        out.put("filterScopes", FILTER_SCOPE.probeSlice());
        out.put("isolate", POLICY.probeSlice());
        out.put("backgroundOnly", BackgroundOnlyGate.probeSlice());
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

    private static int pendingSkeletonInvocationCount() {
        synchronized (SKELETON_INVOCATIONS) {
            int count = 0;
            for (ArrayDeque<Long> ids : SKELETON_INVOCATIONS.values()) count += ids.size();
            return count;
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
        FILTER_SCOPE.clear();
        POLICY.setIsolate(false);
        POLICY.clear();
        synchronized (BRIDGE_LOCK) {
            LEDGER.closeForRecovery("recovery");
            synchronized (SURFACE_INVOCATIONS) { SURFACE_INVOCATIONS.clear(); }
            synchronized (SKELETON_INVOCATIONS) { SKELETON_INVOCATIONS.clear(); }
            synchronized (STANCE_INVOCATIONS) { STANCE_INVOCATIONS.clear(); }
        }
        Sts1NativePresentationAdapter.clearTransientEffects();
        // Recovery/panic ends every delegation (isolate cleared, ledger closed, tokens dropped),
        // so no surface is actively delegating here. Withdraw surface-owned native inputs too so a
        // stale retained target/ownership claim cannot survive into the next scene/frame. Effect
        // inputs are handled by the line above and are never touched here.
        Sts1NativePresentationAdapter.clearSurfaceInputs();
    }

    public static TransientEffectLedger effectLedger() { return EFFECT_LEDGER; }

    public static TransientEffectRegistry effectRegistry() { return EFFECT_REGISTRY; }

    /** Package-local access for tests and native-boundary policy wiring. */
    static NativeFilterScope filterScope() { return FILTER_SCOPE; }

    public static NativeRenderPolicy policy() { return POLICY; }

    private static RenderDisposition normalSurfaceDecision(SurfaceDrawPlan.Entry entry,
            NativeRenderInvocation invocation, NativeRenderOwnership ownership) {
        String entityId = Sts1NativePresentationAdapter.present(invocation, ownership);
        return RenderDisposition.delegate(invocation.invocationId, entry.reason, entityId);
    }

    static void filterFamily(String family) {
        String key = family == null ? "" : family.trim();
        if (key.isEmpty()) return;
        FILTER_SCOPE.filter(key);
        FILTER_SCOPE.activate();
    }

    static void unfilterFamily(String family) {
        FILTER_SCOPE.unfilter(family);
    }

    static void clearFilterScopes() {
        FILTER_SCOPE.clear();
    }

    static java.util.Map<String, Object> filterScopeProbeSlice() {
        return FILTER_SCOPE.probeSlice();
    }

    public static void resetForTests() {
        nextInvocationId = 0L;
        lastProjectionFrameId = -1L;
        LEDGER.clear();
        EFFECT_LEDGER.reset();
        EFFECT_REGISTRY.clear();
        FILTER_SCOPE.clear();
        POLICY.reset();
        synchronized (SURFACE_INVOCATIONS) { SURFACE_INVOCATIONS.clear(); }
        synchronized (SKELETON_INVOCATIONS) { SKELETON_INVOCATIONS.clear(); }
        synchronized (STANCE_INVOCATIONS) { STANCE_INVOCATIONS.clear(); }
        synchronized (BRIDGE_LOCK) { NATIVE_CONTINUATION_FRAMES.clear(); }
        beforeTokenPublicationForTests = null;
        Sts1NativePresentationAdapter.clear();
    }

    /** Refreshes projected exemption evidence after a policy revision. */
    public static int refreshPolicyProjection() {
        return Sts1NativePresentationAdapter.refreshPolicyProjection();
    }

    public static void setBeforeTokenPublicationForTests(Runnable hook) {
        beforeTokenPublicationForTests = hook;
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
