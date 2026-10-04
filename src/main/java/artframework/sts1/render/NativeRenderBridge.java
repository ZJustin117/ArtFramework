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
    /** Pending per-instance {@code vfx-stance-aura} claims keyed by effect instanceId. */
    private static final Map<String, ArrayDeque<Long>> EFFECT_INVOCATIONS =
            new HashMap<String, ArrayDeque<Long>>();
    private static final Map<String, Long> NATIVE_CONTINUATION_FRAMES =
            new HashMap<String, Long>();
    /**
     * Stable per-live-object effect ids. {@code System.identityHashCode} is not unique across
     * time, so an id derived from it can be reused by a later live object; a monotonic sequence
     * assigned per object can never collide. Bounded with oldest-eviction as defense in depth.
     */
    private static final java.util.IdentityHashMap<
            com.megacrit.cardcrawl.vfx.AbstractGameEffect, EffectIdState> EFFECT_IDS =
            new java.util.IdentityHashMap<
                    com.megacrit.cardcrawl.vfx.AbstractGameEffect, EffectIdState>();
    /** Insertion order companion so the oldest effect ids can be evicted in O(log n). */
    private static final TreeMap<Long, com.megacrit.cardcrawl.vfx.AbstractGameEffect>
            EFFECT_ID_ORDER = new TreeMap<Long, com.megacrit.cardcrawl.vfx.AbstractGameEffect>();
    private static final Object EFFECT_IDS_LOCK = new Object();
    static final int EFFECT_IDS_CAPACITY = 8192;
    private static long nextEffectSeq;

    /**
     * Native local render-order baseline (NRO-04 C01): observation-only counters classifying each
     * observed transient-effect render by the native {@code AbstractDungeon.render} call-site band
     * it happened in. Two totals buckets (native vs claimed) plus a bounded per-class map. This is
     * pure observation: it NEVER changes identity/admission/lifecycle and never throws.
     */
    private static final Object EFFECT_BANDS_LOCK = new Object();
    /** Distinct effect classes tracked in {@link #claimedBandsByClass} before oldest-eviction. */
    static final int CLAIMED_BY_CLASS_CAPACITY = 32;
    /** Class-name bucket used when an observation has no (or a blank) class name. */
    static final String UNKNOWN_EFFECT_CLASS = "<unknown>";
    private static final int EFFECT_BAND_COUNT = EffectRenderBand.bands().size();
    private static final int[] NATIVE_EFFECT_BANDS = new int[EFFECT_BAND_COUNT];
    private static final int[] CLAIMED_EFFECT_BANDS = new int[EFFECT_BAND_COUNT];
    /**
     * Bounded per-class band attribution for the CLAIMED observations only (diagnostic; names the
     * class whose claimed draw landed in a native band). Oldest class name is evicted beyond
     * {@link #CLAIMED_BY_CLASS_CAPACITY}, counted by {@code claimedByClassOverflow}.
     */
    private static final Map<String, int[]> CLAIMED_BANDS_BY_CLASS =
            new LinkedHashMap<String, int[]>();
    private static int claimedByClassOverflow;

    /**
     * Per-RENDER-PASS within-pass band SEQUENCE evidence (NRO-04 C02). The pass boundary is an
     * explicit counter bumped at {@code AbstractDungeon.render} ENTRY via
     * {@link #beginEffectRenderPass()}, NOT the ART {@code lastFrameId()}: the projection frame id
     * does not advance on menu/transition screens, which collapsed successive native render passes
     * into one "frame" and produced false {@code orderViolations} on D1. Within one native
     * {@code AbstractDungeon.render} invocation the three call sites are strictly sequential
     * (A line 2674 → B line 2697 → C line 2802), so per-pass monotonicity is the correct invariant.
     *
     * <p>{@code lastOrderRank} is the rank of the last real band observed in
     * {@code lastOrderPassId}; when the pass changes the per-pass rank resets and
     * {@code passesObserved} increments. A subsequent observation whose rank is STRICTLY LOWER than
     * the last one seen in the same pass is an {@code orderViolation}.
     * {@link EffectRenderBand.Band#UNKNOWN} never participates: it neither resets nor advances this
     * state.
     */
    private static final long NO_ORDER_PASS = Long.MIN_VALUE;
    private static long effectRenderPassId = NO_ORDER_PASS;
    private static long lastOrderPassId = NO_ORDER_PASS;
    private static int lastOrderRank = -1;
    private static int orderViolations;
    private static int passesObserved;

    /**
     * Bounded within-pass ORDERED observation SEQUENCE evidence (NRO-04 C03). For the CURRENT pass
     * only, each real band keeps an append-only tail (capacity
     * {@link #EFFECT_SEQUENCE_CAPACITY}) of every observation in native traversal order. A repeated
     * observation of the SAME instance is NEVER coalesced by class name: every observation appends
     * its own entry, so two distinct instances of one class (or the same instance across frames)
     * appear as separate entries. When the pass changes
     * ({@link #beginEffectRenderPass()} or a new pass detected) every tail is cleared, because a
     * tail represents exactly one native {@code AbstractDungeon.render} traversal.
     *
     * <p>{@code EFFECT_BAND_INTERLEAVE} is the number of adjacent entries in the RECORDED (bounded)
     * tail whose {@code claimed} flag differs (a claimed&lt;-&gt;native adjacency). It is a witness
     * over the bounded tail, not an exhaustive count of every transition in an overflowing pass.
     * {@code EFFECT_BAND_SEQUENCE_TRUNCATED} records that at least one oldest entry was dropped for
     * the band during the current pass. {@link EffectRenderBand.Band#UNKNOWN} never creates a
     * sequence entry.
     */
    static final int EFFECT_SEQUENCE_CAPACITY = 16;
    private static final class EffectBandObservation {
        final boolean claimed;
        final String className;

        EffectBandObservation(boolean claimed, String className) {
            this.claimed = claimed;
            this.className = className;
        }
    }

    @SuppressWarnings("unchecked")
    private static final ArrayDeque<EffectBandObservation>[] EFFECT_BAND_SEQUENCES =
            new ArrayDeque[EFFECT_BAND_COUNT];
    private static final boolean[] EFFECT_BAND_SEQUENCE_TRUNCATED = new boolean[EFFECT_BAND_COUNT];
    private static final int[] EFFECT_BAND_INTERLEAVE = new int[EFFECT_BAND_COUNT];
    private static long sequencePassId = NO_ORDER_PASS;

    /** Identity plus its monotonic sequence, used to release the order entry in O(log n). */
    private static final class EffectIdState {
        final String id;
        final long seq;

        EffectIdState(String id, long seq) {
            this.id = id;
            this.seq = seq;
        }
    }

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
        // Default-off per-instance transient-effect claim (NRO-04), family-neutral; current members
        // are the vfx-stance-aura FQNs. Panic and background-only already returned above, so this
        // branch never overrides them. When the gate is off, the class is unsupported, or the
        // injected renderer is not ready, the disposition stays the existing isolate-delegate or
        // capture.
        final boolean vfxClaimProposed = VfxDelegationGate.isActive()
                && VfxClaimPolicy.supports(identity.nativeClass)
                && VfxArtRenderer.isReady(identity.nativeClass);
        RenderDisposition disposition = vfxClaimProposed
                ? RenderDisposition.delegate(invocation.invocationId, "aura_claim",
                        "effect:" + identity.instanceId)
                : (isolated && !exempt
                        ? RenderDisposition.delegate(invocation.invocationId,
                                "isolate:transient_effect", "effect:" + identity.instanceId)
                        : RenderDisposition.capture(invocation.invocationId,
                                "transient_effect_observe"));
        synchronized (BRIDGE_LOCK) {
            disposition = LEDGER.recordDispositionOrRecovery(disposition);
            if (beforeTokenPublicationForTests != null) beforeTokenPublicationForTests.run();
            // Correlate the token from the proposal (not the reason string): a recovery fail-open
            // changes the disposition, and its mode test below then skips token registration.
            if (vfxClaimProposed
                    && disposition.mode == RenderDisposition.Mode.DELEGATE_TO_ART
                    && LEDGER.isPendingDelegated(invocation.invocationId)) {
                // A claim keeps a host draw callback token so a successful ART draw or a
                // fail-open fallback can be correlated to this exact instance.
                registerEffectInvocation(identity.instanceId, invocation.invocationId);
            } else if (!vfxClaimProposed
                    && disposition.mode == RenderDisposition.Mode.DELEGATE_TO_ART
                    && LEDGER.isPendingDelegated(invocation.invocationId)
                    && LEDGER.completeDelegatedWithoutEvidence(invocation.invocationId)) {
                // Effect projection has no host draw callback. Complete the delegated lifecycle
                // without manufacturing pixel evidence; native continuation remains suppressed.
                LEDGER.recordNoPixelIsolation();
            }
        }
        projectPendingEffectsOncePerFrame();
        return disposition;
    }

    private static void registerEffectInvocation(String instanceId, long invocationId) {
        if (instanceId == null || instanceId.isEmpty()) return;
        synchronized (EFFECT_INVOCATIONS) {
            ArrayDeque<Long> ids = EFFECT_INVOCATIONS.get(instanceId);
            if (ids == null) {
                ids = new ArrayDeque<Long>();
                EFFECT_INVOCATIONS.put(instanceId, ids);
            }
            ids.addLast(Long.valueOf(invocationId));
        }
    }

    /** True iff the invocation id is a pending per-instance transient-effect claim. */
    public static boolean isVfxClaimInvocation(long invocationId) {
        synchronized (EFFECT_INVOCATIONS) {
            for (ArrayDeque<Long> ids : EFFECT_INVOCATIONS.values()) {
                if (ids.contains(Long.valueOf(invocationId))) return true;
            }
            return false;
        }
    }

    /** Records delegated claim pixel evidence then consumes the pending claim token. */
    public static void recordEffectDraw(long invocationId, int drawCount) {
        synchronized (BRIDGE_LOCK) {
            if (!isVfxClaimInvocation(invocationId)) return;
            if (LEDGER.recordDelegatedEvidence(invocationId, drawCount, "active")) {
                removeEffectInvocation(invocationId);
            } else {
                LEDGER.recordOrphanArtOutput();
            }
        }
    }

    /**
     * Fails a pending transient-effect claim open to native and always consumes its token. It must
     * not throw: a stale id with no token is a no-op. The declining class is attributed as
     * {@code "<unknown>"} because no effect instance is supplied; use the overload that takes the
     * effect to name the class.
     */
    public static void recordEffectFailure(long invocationId) {
        recordEffectFailure(invocationId, null);
    }

    /**
     * Fails a pending transient-effect claim open to native, always consuming its token, and
     * attributes the decline to the effect's class name for diagnostics. It must not throw: a stale
     * id with no token is a no-op.
     */
    public static void recordEffectFailure(long invocationId,
            com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) {
        synchronized (BRIDGE_LOCK) {
            try {
                if (!isVfxClaimInvocation(invocationId)) return;
                if (LEDGER.recordDelegatedFallbackIfPending(invocationId)) {
                    LEDGER.recordDeclinedClass(
                            effect == null ? null : effect.getClass().getName());
                    return;
                }
                if (LEDGER.isPendingDelegated(invocationId)) LEDGER.recordOrphanArtOutput();
            } finally {
                removeEffectInvocation(invocationId);
            }
        }
    }

    /**
     * Records a declined claim for the given instance, distinguishing a benign no-pixel decline from
     * a genuine renderer failure. It is benign only when the adapter can determine that THIS exact
     * instance would natively draw nothing
     * ({@link VfxArtRenderer#declinedWithoutPixels}: a null-image kind guarded on a present image,
     * or a wait-phase-guarded kind whose guard field blocks) and the ART renderer could not draw it
     * either ({@link VfxArtRenderer#canDraw}); then failing open loses nothing: the delegated
     * lifecycle is completed without pixel evidence and counted as no-pixel isolation, NOT as a
     * {@code dispositionMismatch} / {@code delegatedWithoutEvidence}. Every other declined claim —
     * including a guard-SATISFIED guard kind whose image snapshot failed — keeps the existing
     * fallback accounting, so a genuine renderer failure is never masked. The cheaper,
     * instance-scoping {@code declinedWithoutPixels} probe is evaluated first and short-circuits.
     * Always consumes the pending claim token and never throws.
     */
    public static void recordEffectDeclined(
            long invocationId, com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) {
        synchronized (BRIDGE_LOCK) {
            try {
                if (!isVfxClaimInvocation(invocationId)) return;
                if (VfxArtRenderer.declinedWithoutPixels(effect)
                        && !VfxArtRenderer.canDraw(effect)) {
                    if (LEDGER.completeDelegatedWithoutEvidence(invocationId)) {
                        LEDGER.recordNoPixelIsolation();
                    }
                    return;
                }
                if (LEDGER.recordDelegatedFallbackIfPending(invocationId)) {
                    // Genuine fallback: attribute the decline to the effect's class for diagnosis.
                    LEDGER.recordDeclinedClass(
                            effect == null ? null : effect.getClass().getName());
                    return;
                }
                if (LEDGER.isPendingDelegated(invocationId)) LEDGER.recordOrphanArtOutput();
            } finally {
                removeEffectInvocation(invocationId);
            }
        }
    }

    private static void removeEffectInvocation(long invocationId) {
        synchronized (EFFECT_INVOCATIONS) {
            List<String> emptyOwners = new ArrayList<String>();
            for (Map.Entry<String, ArrayDeque<Long>> entry : EFFECT_INVOCATIONS.entrySet()) {
                entry.getValue().remove(Long.valueOf(invocationId));
                if (entry.getValue().isEmpty()) emptyOwners.add(entry.getKey());
            }
            for (String owner : emptyOwners) EFFECT_INVOCATIONS.remove(owner);
        }
    }

    public static void observeEffectUpdate(
            com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) {
        if (PresentSafety.isPanic()) return;
        // A pooled effect is RECYCLED: the same object is re-`init` and rendered again after it
        // completes. With the NRM-13 sticky per-object instanceId, completing it would move its
        // record into `recent`; its next reuse would then re-admit under the same id and be
        // rejected as a terminal observation (rejectedTerminal/unknownLifecycle ~1 per recycled
        // render). Pooled objects therefore stay active (not completed) and are simply re-admitted
        // on reuse, bounded by the pool size (and the 4096 caps as defense-in-depth). This applies
        // to BOTH the class-level Postfix path and the container detached path below.
        if (effect instanceof com.badlogic.gdx.utils.Pool.Poolable) return;
        TransientEffectIdentity identity = effectIdentity(effect);
        if (identity == null) return;
        // Reached from the class-level AbstractGameEffect.update() Postfix in
        // TransientEffectRenderPatches for subclasses that call super.update(). This path COMPLETES
        // (and retains a terminal record): a super-calling effect that reports isDone is treated as
        // genuinely finished, so its projected entity is reclaimed once. The container AFTER-update
        // path for superless effects uses observeEffectUpdateDetached instead, because container
        // isDone is not a reliable end signal. updateIfActive performs the check and the update
        // under one lock, so a no-longer-active instance never surfaces "update after effect
        // termination" from an observation path.
        if (EFFECT_LEDGER.updateIfActive(identity, effect.isDone) && effect.isDone) {
            EFFECT_REGISTRY.cleanup(identity);
        }
        projectPendingEffectsOncePerFrame();
    }

    /**
     * Container AFTER-update completion observation for effects whose {@code update()} does NOT
     * call {@code super.update()} (so the class-level Postfix never fires).
     *
     * <p>Unlike {@link #observeEffectUpdate}, this path DETACHES: when the effect reports
     * {@code isDone} it drops the active record WITHOUT retaining a terminal record. Container
     * {@code isDone} is not a reliable end-of-life signal — the native {@code AbstractDungeon.render}
     * traversal may still render the object (not yet removed, or re-added), and under the NRM-13
     * sticky per-object ids a retained terminal record would make that next render a terminal
     * rejection ({@code rejectedTerminal}/{@code unknownLifecycle} once per render, freezing
     * {@code total}/{@code completed}). Detaching lets a later render of the same object re-admit as
     * a fresh active record; memory stays bounded because the active record is released. Any
     * observation failure is swallowed (counted as fail-open) so the native update path is never
     * interrupted.
     */
    public static void observeEffectUpdateDetached(
            com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) {
        if (PresentSafety.isPanic()) return;
        // Pooled effects are excluded for the same reason as observeEffectUpdate: a recycled object
        // must not be moved into a terminal state, so its record stays active and is re-admitted.
        if (effect instanceof com.badlogic.gdx.utils.Pool.Poolable) return;
        try {
            TransientEffectIdentity identity = effectIdentity(effect);
            if (identity == null) return;
            if (effect.isDone && EFFECT_LEDGER.detach(identity)) {
                EFFECT_REGISTRY.cleanup(identity);
            }
            projectPendingEffectsOncePerFrame();
        } catch (Throwable error) {
            recordEffectObservationFailure();
        }
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

    /**
     * Observation-only native local render-order baseline (NRO-04 C01).
     *
     * <p>Records WHICH native {@code AbstractDungeon.render} band an observed transient-effect
     * render happened in, as classified from the instrumented call-site line number by
     * {@link EffectRenderBand#classify(int)}. Because the container seam replaces the three native
     * call sites IN PLACE, band membership IS native order: a claimed draw recorded in band A/B/C
     * proves it landed in the correct native position rather than merely trusting call-site
     * replacement.
     *
     * <p>Bumps a per-bucket total for either the {@code native} (claimed=false) or {@code claimed}
     * (claimed=true) bucket, plus, for CLAIMED observations only, a bounded per-class map
     * ({@link #CLAIMED_BY_CLASS_CAPACITY} distinct class names, oldest-evicted, overflow counted).
     * A null/blank class name collapses to {@link #UNKNOWN_EFFECT_CLASS}.
     *
     * <p>Also records within-pass band SEQUENCE evidence (NRO-04 C02): both native and claimed
     * observations advance the SAME per-render-pass ordinal state (see
     * {@link #recordObservedEffectBand(String, int, boolean, long)}). This is ordering-sequence
     * evidence only, NOT pixel occlusion.
     *
     * <p>This is pure observation: it NEVER throws, never changes identity/admission/lifecycle, and
     * on any unexpected failure silently returns without touching the render path.
     */
    public static void recordObservedEffectBand(
            com.megacrit.cardcrawl.vfx.AbstractGameEffect effect, int lineNumber, boolean claimed) {
        recordObservedEffectBandForClass(
                effect == null ? null : effect.getClass().getName(), lineNumber, claimed);
    }

    /**
     * Advances the per-render-pass boundary (NRO-04 C02). Called at {@code AbstractDungeon.render}
     * ENTRY (see {@code TransientEffectContainerPatches.ObserveEffectRenderPass}); it only bumps a
     * monotonic counter so the band-ordinal observation has a correct pass boundary. It never
     * throws and never touches admission/identity/lifecycle or native pixels.
     */
    public static void beginEffectRenderPass() {
        try {
            synchronized (EFFECT_BANDS_LOCK) {
                // Skip the sentinel so passes start at 0 and stay distinguishable from "none yet".
                if (effectRenderPassId == NO_ORDER_PASS) {
                    effectRenderPassId = 0L;
                } else {
                    effectRenderPassId++;
                }
                // A new render pass invalidates the previous pass's bounded sequence tails.
                sequencePassId = effectRenderPassId;
                clearSequencesLocked();
            }
        } catch (Throwable ignored) {
            // Observation-only: never interrupt the native render path.
        }
    }

    /**
     * Class-name form of {@link #recordObservedEffectBand}; package-visible so the bounded-map
     * eviction is unit-testable without manufacturing one Java class per distinct name. Uses the
     * current render-pass boundary and delegates to the deterministic-pass-id overload.
     */
    static void recordObservedEffectBandForClass(
            String className, int lineNumber, boolean claimed) {
        recordObservedEffectBand(className, lineNumber, claimed, currentEffectRenderPassId());
    }

    private static long currentEffectRenderPassId() {
        synchronized (EFFECT_BANDS_LOCK) {
            return effectRenderPassId;
        }
    }

    /**
     * Deterministic-pass-id form of {@link #recordObservedEffectBandForClass} (NRO-04 C02):
     * package-visible so within-pass ordering evidence is unit-testable without a native render
     * pass. In addition to the per-bucket totals, this advances the per-pass band-ordinal state: a
     * real band whose rank is strictly lower than the last real band seen in the SAME pass is an
     * {@code orderViolations}; a new pass resets the per-pass rank and increments
     * {@code passesObserved}; {@link EffectRenderBand.Band#UNKNOWN} participates in neither.
     */
    static void recordObservedEffectBand(
            String className, int lineNumber, boolean claimed, long passId) {
        try {
            String key = className == null || className.trim().isEmpty()
                    ? UNKNOWN_EFFECT_CLASS : className;
            EffectRenderBand.Band band = EffectRenderBand.classify(lineNumber);
            int bandIndex = bandIndex(band);
            synchronized (EFFECT_BANDS_LOCK) {
                // A new pass (explicit id or the begin boundary) invalidates the current tails.
                if (passId != sequencePassId) {
                    sequencePassId = passId;
                    clearSequencesLocked();
                }
                if (claimed) {
                    CLAIMED_EFFECT_BANDS[bandIndex]++;
                    int[] perBand = CLAIMED_BANDS_BY_CLASS.get(key);
                    if (perBand == null
                            && CLAIMED_BANDS_BY_CLASS.size() >= CLAIMED_BY_CLASS_CAPACITY) {
                        java.util.Iterator<String> oldest =
                                CLAIMED_BANDS_BY_CLASS.keySet().iterator();
                        if (oldest.hasNext()) {
                            oldest.next();
                            oldest.remove();
                            claimedByClassOverflow++;
                        }
                    }
                    if (perBand == null) {
                        perBand = new int[EFFECT_BAND_COUNT];
                        CLAIMED_BANDS_BY_CLASS.put(key, perBand);
                    }
                    perBand[bandIndex]++;
                } else {
                    NATIVE_EFFECT_BANDS[bandIndex]++;
                }
                recordSequenceLocked(band, bandIndex, claimed, key);
                int rank = EffectRenderBand.rank(band);
                if (rank >= 0) {
                    if (passId != lastOrderPassId) {
                        lastOrderPassId = passId;
                        lastOrderRank = -1;
                        passesObserved++;
                    }
                    if (lastOrderRank >= 0 && rank < lastOrderRank) {
                        orderViolations++;
                    }
                    lastOrderRank = rank;
                }
            }
        } catch (Throwable ignored) {
            // Observation-only: never interrupt the native render path.
        }
    }

    private static int bandIndex(EffectRenderBand.Band band) {
        int index = EffectRenderBand.bands().indexOf(band);
        return index < 0 ? EFFECT_BAND_COUNT - 1 : index;
    }

    /**
     * Appends one observation to its band's bounded tail (NRO-04 C03). Every observation appends its
     * own entry, so no entry is ever coalesced by class name. An UNKNOWN band creates no entry.
     * Beyond {@link #EFFECT_SEQUENCE_CAPACITY} the oldest entry is dropped and the band is marked
     * truncated. {@code interleave} is recomputed over the recorded (bounded) tail.
     */
    private static void recordSequenceLocked(
            EffectRenderBand.Band band, int bandIndex, boolean claimed, String className) {
        if (EffectRenderBand.rank(band) < 0) return;
        ArrayDeque<EffectBandObservation> tail = EFFECT_BAND_SEQUENCES[bandIndex];
        if (tail == null) {
            tail = new ArrayDeque<EffectBandObservation>();
            EFFECT_BAND_SEQUENCES[bandIndex] = tail;
        }
        tail.addLast(new EffectBandObservation(claimed, className));
        while (tail.size() > EFFECT_SEQUENCE_CAPACITY) {
            tail.removeFirst();
            EFFECT_BAND_SEQUENCE_TRUNCATED[bandIndex] = true;
        }
        EFFECT_BAND_INTERLEAVE[bandIndex] = countInterleave(tail);
    }

    /** Adjacent entries whose {@code claimed} flag differs (a claimed&lt;-&gt;native adjacency). */
    private static int countInterleave(ArrayDeque<EffectBandObservation> tail) {
        int transitions = 0;
        Boolean previous = null;
        for (EffectBandObservation observation : tail) {
            if (previous != null && previous.booleanValue() != observation.claimed) {
                transitions++;
            }
            previous = Boolean.valueOf(observation.claimed);
        }
        return transitions;
    }

    /** Clears every per-band ordered tail/truncation/interleave for the current pass. */
    private static void clearSequencesLocked() {
        for (int i = 0; i < EFFECT_BAND_COUNT; i++) {
            if (EFFECT_BAND_SEQUENCES[i] != null) EFFECT_BAND_SEQUENCES[i].clear();
            EFFECT_BAND_SEQUENCE_TRUNCATED[i] = false;
            EFFECT_BAND_INTERLEAVE[i] = 0;
        }
    }

    private static Map<String, Integer> bandCounts(int[] counts) {
        List<EffectRenderBand.Band> bands = EffectRenderBand.bands();
        Map<String, Integer> m = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < bands.size(); i++) {
            m.put(EffectRenderBand.name(bands.get(i)), Integer.valueOf(counts[i]));
        }
        return m;
    }

    /**
     * Stable, bounded snapshot of the native/claimed effect-band counters and the claimed per-class
     * attribution ({@code claimedByClass} is a TreeMap so its key order is deterministic).
     */
    static Map<String, Object> effectBandsProbeSlice() {
        synchronized (EFFECT_BANDS_LOCK) {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("native", bandCounts(NATIVE_EFFECT_BANDS));
            out.put("claimed", bandCounts(CLAIMED_EFFECT_BANDS));
            Map<String, Map<String, Integer>> byClass = new TreeMap<String, Map<String, Integer>>();
            for (Map.Entry<String, int[]> entry : CLAIMED_BANDS_BY_CLASS.entrySet()) {
                byClass.put(entry.getKey(), bandCounts(entry.getValue()));
            }
            out.put("claimedByClass",
                    new LinkedHashMap<String, Map<String, Integer>>(byClass));
            out.put("claimedByClassCap", Integer.valueOf(CLAIMED_BY_CLASS_CAPACITY));
            out.put("claimedByClassOverflow", Integer.valueOf(claimedByClassOverflow));
            out.put("orderViolations", Integer.valueOf(orderViolations));
            out.put("passesObserved", Integer.valueOf(passesObserved));
            out.put("sequence", sequenceSnapshot());
            out.put("sequenceTruncated", sequenceTruncatedSnapshot());
            out.put("interleave", interleaveSnapshot());
            return out;
        }
    }

    /**
     * Stable, bounded map bandName -&gt; ordered list of {@code {claimed, class}} entries for the
     * CURRENT pass (empty list when the band has no recorded entry). Every observation appends its
     * own entry, so instances are never coalesced by class name.
     */
    private static Map<String, List<Map<String, Object>>> sequenceSnapshot() {
        Map<String, List<Map<String, Object>>> out =
                new LinkedHashMap<String, List<Map<String, Object>>>();
        List<EffectRenderBand.Band> bands = EffectRenderBand.bands();
        for (int i = 0; i < bands.size(); i++) {
            ArrayDeque<EffectBandObservation> tail = EFFECT_BAND_SEQUENCES[i];
            List<Map<String, Object>> entries = new ArrayList<Map<String, Object>>();
            if (tail != null) {
                for (EffectBandObservation observation : tail) {
                    Map<String, Object> entry = new LinkedHashMap<String, Object>();
                    entry.put("claimed", Boolean.valueOf(observation.claimed));
                    entry.put("class", observation.className);
                    entries.add(entry);
                }
            }
            out.put(EffectRenderBand.name(bands.get(i)), entries);
        }
        return out;
    }

    /** Stable map bandName -&gt; whether the current pass dropped an oldest entry for that band. */
    private static Map<String, Boolean> sequenceTruncatedSnapshot() {
        Map<String, Boolean> out = new LinkedHashMap<String, Boolean>();
        List<EffectRenderBand.Band> bands = EffectRenderBand.bands();
        for (int i = 0; i < bands.size(); i++) {
            out.put(EffectRenderBand.name(bands.get(i)),
                    Boolean.valueOf(EFFECT_BAND_SEQUENCE_TRUNCATED[i]));
        }
        return out;
    }

    /** Stable map bandName -&gt; adjacent claimed/native transitions in the recorded (bounded) tail. */
    private static Map<String, Integer> interleaveSnapshot() {
        Map<String, Integer> out = new LinkedHashMap<String, Integer>();
        List<EffectRenderBand.Band> bands = EffectRenderBand.bands();
        for (int i = 0; i < bands.size(); i++) {
            out.put(EffectRenderBand.name(bands.get(i)),
                    Integer.valueOf(EFFECT_BAND_INTERLEAVE[i]));
        }
        return out;
    }

    private static void clearEffectBandsForTests() {
        synchronized (EFFECT_BANDS_LOCK) {
            java.util.Arrays.fill(NATIVE_EFFECT_BANDS, 0);
            java.util.Arrays.fill(CLAIMED_EFFECT_BANDS, 0);
            CLAIMED_BANDS_BY_CLASS.clear();
            claimedByClassOverflow = 0;
            effectRenderPassId = NO_ORDER_PASS;
            lastOrderPassId = NO_ORDER_PASS;
            lastOrderRank = -1;
            orderViolations = 0;
            passesObserved = 0;
            sequencePassId = NO_ORDER_PASS;
            clearSequencesLocked();
        }
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
        out.put("transientEffectEntityCap", Integer.valueOf(EFFECT_REGISTRY.entityCapacity()));
        // Native local render-order baseline (NRO-04 C01): which native AbstractDungeon.render band
        // each observed effect render (native vs claimed) happened in.
        out.put("effectBands", effectBandsProbeSlice());
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
            synchronized (EFFECT_INVOCATIONS) { EFFECT_INVOCATIONS.clear(); }
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
        synchronized (EFFECT_INVOCATIONS) { EFFECT_INVOCATIONS.clear(); }
        clearEffectIds();
        clearEffectBandsForTests();
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
        // nativeIdentityHash stays diagnostic-only: it must never participate in identity
        // equality (TransientEffectLedger.sameIdentity compares instanceId/nativeClass/generation
        // only), because identityHashCode values are reused after an object dies and would then
        // let a dead terminal record reject a new live instance.
        int hash = System.identityHashCode(effect);
        return new TransientEffectIdentity(effectId(effect, nativeClass), nativeClass, hash, 0L);
    }

    /** Returns the stable, non-reusable id for one live effect, assigning it on first sight. */
    private static String effectId(com.megacrit.cardcrawl.vfx.AbstractGameEffect effect,
            String nativeClass) {
        synchronized (EFFECT_IDS_LOCK) {
            EffectIdState existing = EFFECT_IDS.get(effect);
            if (existing != null) return existing.id;
            long seq = ++nextEffectSeq;
            String id = nativeClass + "#" + Long.toHexString(seq);
            EFFECT_IDS.put(effect, new EffectIdState(id, seq));
            EFFECT_ID_ORDER.put(Long.valueOf(seq), effect);
            while (EFFECT_IDS.size() > EFFECT_IDS_CAPACITY) {
                Map.Entry<Long, com.megacrit.cardcrawl.vfx.AbstractGameEffect> oldest =
                        EFFECT_ID_ORDER.pollFirstEntry();
                if (oldest == null) break;
                EFFECT_IDS.remove(oldest.getValue());
            }
            return id;
        }
    }

    /**
     * Drops every tracked id. Only the test/reset path calls this: recovery deliberately keeps the
     * ids so that a post-recovery re-observation of a pre-recovery object still matches its
     * terminal ledger record instead of being re-admitted as a fresh instance.
     */
    private static void clearEffectIds() {
        synchronized (EFFECT_IDS_LOCK) {
            EFFECT_IDS.clear();
            EFFECT_ID_ORDER.clear();
        }
    }

    /** Package-visible for tests: number of live effects currently holding a tracked id. */
    static int trackedEffectIdCount() {
        synchronized (EFFECT_IDS_LOCK) {
            return EFFECT_IDS.size();
        }
    }

    /** Package-visible for tests: the stable non-reusable id assigned to one effect. */
    static String effectInstanceIdForTests(
            com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) {
        TransientEffectIdentity identity = effectIdentity(effect);
        return identity == null ? null : identity.instanceId;
    }
}
