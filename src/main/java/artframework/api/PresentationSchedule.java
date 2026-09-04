package artframework.api;

import artframework.context.ContextFrame;
import artframework.context.AuthorityProjectionSystem;
import artframework.context.BusinessConfirmationSystem;
import artframework.context.FrameDiff;
import artframework.context.NativeIntentLifecycleSystem;
import artframework.context.PresentProjections;
import artframework.core.AnimationPlaybackSystem;
import artframework.core.EffectPulseSystem;
import artframework.core.HostBackendTickSystem;
import artframework.core.TransientSignalRuntime;
import artframework.core.TransientSignalPaths;
import artframework.core.SignalGroups;
import artframework.core.SignalGroup;
import artframework.core.PackSystemPhase;
import artframework.core.PackSystems;
import artframework.ecs.ArtEcs;
import artframework.ecs.EcsPipeline;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.presentation.ControlValueSystem;
import artframework.render.RenderClockSystem;
import artframework.render.RenderProjectionQueue;
import artframework.render.RenderProjectionSystem;
import artframework.skeleton.SkeletonHostTickSystem;
import artframework.sts1.render.NativeRenderBridge;
import artframework.sts1.render.TransientEffectProjectionSystem;
import artframework.sts1.skeleton.Sts1SkeletonBridge;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Single host-thread schedule for one ART presentation update.
 *
 * <p>Authority projection intentionally runs before simulation so animation and effects observe
 * the frame that will be rendered during this update.</p>
 */
public final class PresentationSchedule implements AutoCloseable {
    public enum Phase {
        SURFACE_INTENT_EXECUTION,
        AUTHORITY_PROJECTION_AND_CONFIRMATION,
        WORLD_NORMALIZATION,
        ANIMATION,
        EFFECTS,
        HOST_PRESENTATION,
        RENDER_PROJECTION,
        RENDER_CLOCK,
        HOST_BACKEND
    }

    private static final List<Phase> PHASES = Collections.unmodifiableList(Arrays.asList(
            Phase.SURFACE_INTENT_EXECUTION,
            Phase.AUTHORITY_PROJECTION_AND_CONFIRMATION,
            Phase.WORLD_NORMALIZATION,
            Phase.ANIMATION,
            Phase.EFFECTS,
            Phase.HOST_PRESENTATION,
            Phase.RENDER_PROJECTION,
            Phase.RENDER_CLOCK,
            Phase.HOST_BACKEND));
    /**
     * ART currently has one process-wide ECS/projection world.  Schedule instances therefore
     * serialize entry into world-mutating operations, while the reentrant lock permits signal
     * and host callbacks to call back into the same schedule synchronously.
     */
    /** All schedules execute against the process-wide ArtEcs/PresentProjections world. */
    private static final ReentrantLock SHARED_WORLD_GATE = new ReentrantLock(true);
    private final ControlValueSystem controls = new ControlValueSystem();
    private AuthorityProjectionSystem authority;
    private BusinessConfirmationSystem businessConfirmation;
    private NativeIntentLifecycleSystem nativeIntentLifecycle;
    private TransientSignalRuntime transientSignals;
    private SignalGroup transientSignalGroup;
    private final AnimationPlaybackSystem animation = new AnimationPlaybackSystem();
    private final EffectPulseSystem effects = new EffectPulseSystem();
    private final RenderClockSystem renderClock = new RenderClockSystem();
    private final RenderProjectionSystem renderProjection = new RenderProjectionSystem();
    private final HostBackendTickSystem hostBackend = new HostBackendTickSystem();
    private SkeletonHostTickSystem skeleton;
    private TransientEffectProjectionSystem transientEffectProjection;
    private HostPresentationSystem hostPresentationSystem;
    private final boolean frameworkAuthority;
    private long sequence;

    public PresentationSchedule() {
        this.frameworkAuthority = false;
        resetTransientSignals(false);
    }

    PresentationSchedule(boolean frameworkAuthority) {
        this.frameworkAuthority = frameworkAuthority;
        resetTransientSignals(frameworkAuthority);
    }

    public List<Phase> phases() {
        return PHASES;
    }

    public void setHostPresentationSystem(HostPresentationSystem system) {
        SHARED_WORLD_GATE.lock();
        try {
            hostPresentationSystem = system;
        } finally {
            SHARED_WORLD_GATE.unlock();
        }
    }

    public void advance(float deltaSeconds, ContextFrame authorityFrame) {
        SHARED_WORLD_GATE.lock();
        try {
            if (deltaSeconds < 0f) {
                throw new IllegalArgumentException("deltaSeconds must be non-negative");
            }
            EcsTick tick = new EcsTick(deltaSeconds, sequence++);
            RenderProjectionQueue.begin();
            try {
                for (Phase phase : PHASES) {
                    switch (phase) {
                        case SURFACE_INTENT_EXECUTION:
                            break;
                        case AUTHORITY_PROJECTION_AND_CONFIRMATION:
                            if (authorityFrame != null) dispatchAuthorityFrame(authorityFrame);
                            run(tick, businessConfirmation);
                            run(tick, nativeIntentLifecycle);
                            break;
                        case WORLD_NORMALIZATION:
                            // World systems run once. All registered scopes share ArtEcs.world().
                            run(tick, controls);
                            runAll(tick, PackSystemPhase.WORLD_NORMALIZATION);
                            break;
                        case ANIMATION:
                            run(tick, animation);
                            runAll(tick, PackSystemPhase.ANIMATION);
                            break;
                        case EFFECTS:
                            run(tick, effects);
                            runAll(tick, PackSystemPhase.EFFECTS);
                            break;
                        case HOST_PRESENTATION:
                            if (hostPresentationSystem != null) {
                                hostPresentationSystem.tick(deltaSeconds);
                            }
                            run(tick, skeletonSystem());
                            run(tick, transientEffectProjectionSystem());
                            runAll(tick, PackSystemPhase.HOST_PRESENTATION);
                            break;
                        case RENDER_PROJECTION:
                            run(tick, renderProjection);
                            runAll(tick, PackSystemPhase.RENDER_PROJECTION);
                            break;
                        case RENDER_CLOCK:
                            run(tick, renderClock);
                            break;
                        case HOST_BACKEND:
                            run(tick, hostBackend);
                            break;
                        default:
                            throw new IllegalStateException("unknown presentation phase: " + phase);
                    }
                }
            } finally {
                RenderProjectionQueue.flush();
            }
        } finally {
            SHARED_WORLD_GATE.unlock();
        }
    }

    /** Dispatches one immutable surface intent command to the schedule-owned transient runtime. */
    public artframework.core.SignalDispatchResult dispatchSurfaceIntent(
            String name, String surfaceId, Object... args) {
        SHARED_WORLD_GATE.lock();
        try {
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("surfaceId", surfaceId);
            payload.put("name", name);
            payload.put("args", artframework.context.SurfaceIntentExecutionSystem.normalizeArgs(args));
            payload.put("__producerSurfaceId", surfaceId);
            String previousProducer =
                    artframework.context.SurfaceIntentExecutionSystem.beginProducer(surfaceId);
            try {
                return transientSignals.dispatch(TransientSignalPaths.SURFACE_INTENT,
                        "c2-surfaces", payload);
            } finally {
                artframework.context.SurfaceIntentExecutionSystem.endProducer(previousProducer);
            }
        } finally {
            SHARED_WORLD_GATE.unlock();
        }
    }

    public void dispatchSurfaceLifecycle(String surfaceId, boolean mounted) {
        SHARED_WORLD_GATE.lock();
        try {
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("surfaceId", surfaceId);
            payload.put("mounted", Boolean.valueOf(mounted));
            transientSignals.dispatch(TransientSignalPaths.SURFACE_LIFECYCLE,
                    "c2-surfaces", payload);
        } finally {
            SHARED_WORLD_GATE.unlock();
        }
    }

    public artframework.core.SignalDispatchResult dispatchNativeIntentLifecycle(String surfaceId,
            String name, artframework.context.NativeIntentLifecycleComponent.State state,
            String message) {
        SHARED_WORLD_GATE.lock();
        try {
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("surfaceId", surfaceId);
            payload.put("name", name != null ? name : "");
            payload.put("state", state != null ? state.name() : null);
            payload.put("message", message != null ? message : "");
            return transientSignals.dispatch(TransientSignalPaths.NATIVE_INTENT_LIFECYCLE,
                    "sts1-input", payload);
        } finally {
            SHARED_WORLD_GATE.unlock();
        }
    }

    /** Processes native lifecycle events at their synchronous host-hook boundary. */
    public void processNativeIntentLifecycle() {
        SHARED_WORLD_GATE.lock();
        try {
            run(new EcsTick(0f, sequence), nativeIntentLifecycle);
        } finally {
            SHARED_WORLD_GATE.unlock();
        }
    }

    /**
     * Drains pending transient-effect projections through the schedule-owned system so
     * synchronous host render hooks keep same-frame entity visibility.
     */
    public void executeTransientEffectProjections() {
        SHARED_WORLD_GATE.lock();
        try {
            run(new EcsTick(0f, sequence), transientEffectProjectionSystem());
        } finally {
            SHARED_WORLD_GATE.unlock();
        }
    }

    /** Advances pulse envelopes through the schedule-owned effect system only. */
    public void executeEffectPulses(float deltaSeconds) {
        if (deltaSeconds < 0f) {
            throw new IllegalArgumentException("deltaSeconds must be non-negative");
        }
        SHARED_WORLD_GATE.lock();
        try {
            RenderProjectionQueue.begin();
            try {
                run(new EcsTick(deltaSeconds, sequence), effects);
            } finally {
                RenderProjectionQueue.flush();
            }
        } finally {
            SHARED_WORLD_GATE.unlock();
        }
    }

    /** Ingests one compatibility frame through the schedule-owned authority systems. */
    public FrameDiff publishFrame(ContextFrame frame) {
        SHARED_WORLD_GATE.lock();
        try {
        if (frame == null) return FrameDiff.skipped("frame required");
        EcsTick tick = new EcsTick(0f, sequence);
        dispatchAuthorityFrame(frame);
        run(tick, businessConfirmation);
        run(tick, nativeIntentLifecycle);
        return PresentProjections.last();
        } finally {
            SHARED_WORLD_GATE.unlock();
        }
    }

    private static void run(EcsTick tick, artframework.ecs.EcsSystem system) {
        EcsPipeline.run(ArtEcs.world(), tick,
                java.util.Collections.singletonList(system));
    }

    private static void runAll(EcsTick tick, PackSystemPhase phase) {
        EcsPipeline.run(ArtEcs.world(), tick, PackSystems.systemsFor(phase));
    }

    private void dispatchAuthorityFrame(ContextFrame frame) {
        ContextFrame before = PresentProjections.get().lastFrame();
        artframework.core.SignalDispatchResult result = transientSignals.dispatch(
                TransientSignalPaths.AUTHORITY_FRAME, "presentation-schedule", frame);
        if (result.isStopped() || result.signal == null
                || !(result.signal.payload instanceof ContextFrame)) return;
        ContextFrame after = (ContextFrame) result.signal.payload;
        // Unavailable frames are valid authority admissions: they advance projection metadata and
        // must fail pending business confirmations. Stale frames leave the prior snapshot intact.
        if (!Boolean.TRUE.equals(result.signal.metadata.get("projectionApplied"))
                && PresentProjections.get().lastFrame() != after) return;
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("before", before);
        payload.put("after", after);
        transientSignals.dispatch(TransientSignalPaths.AUTHORITY_BUSINESS_CONFIRMATION,
                "presentation-schedule", payload);
    }

    private SkeletonHostTickSystem skeletonSystem() {
        if (skeleton == null) {
            skeleton = new SkeletonHostTickSystem(Sts1SkeletonBridge.presentationSystem());
        }
        return skeleton;
    }

    private TransientEffectProjectionSystem transientEffectProjectionSystem() {
        if (transientEffectProjection == null) {
            transientEffectProjection =
                    new TransientEffectProjectionSystem(NativeRenderBridge.effectRegistry());
        }
        return transientEffectProjection;
    }

    public void resetForTests() {
        SHARED_WORLD_GATE.lock();
        try {
            if (transientSignals != null) transientSignals.close();
            resetTransientSignals(frameworkAuthority);
            sequence = 0L;
            hostPresentationSystem = null;
            RenderProjectionQueue.resetForTests();
            PackSystems.resetForTests();
        } finally {
            SHARED_WORLD_GATE.unlock();
        }
    }

    /** Releases this schedule's isolated or framework-owned transient subscriptions. */
    @Override public void close() {
        SHARED_WORLD_GATE.lock();
        try {
            if (transientSignals != null) transientSignals.close();
        } finally {
            SHARED_WORLD_GATE.unlock();
        }
    }

    private void resetTransientSignals(boolean frameworkAuthority) {
        transientSignalGroup = frameworkAuthority
                ? SignalGroups.transientRuntimeRawGroup()
                : SignalGroups.isolatedTransientRuntimeGroup();
        transientSignals = frameworkAuthority
                ? new TransientSignalRuntime()
                : new TransientSignalRuntime(transientSignalGroup, false);
        new artframework.context.SurfaceLifecycleSystem(transientSignals);
        new artframework.context.SurfaceIntentExecutionSystem(transientSignals);
        authority = new artframework.context.AuthorityProjectionSystem(transientSignals);
        businessConfirmation = new artframework.context.BusinessConfirmationSystem(transientSignals);
        nativeIntentLifecycle = new artframework.context.NativeIntentLifecycleSystem(transientSignals);
    }

    SignalGroup transientSignalGroupForTests() {
        return transientSignalGroup;
    }

    TransientSignalRuntime transientSignalRuntimeForTests() {
        return transientSignals;
    }

    void setTransientDispatchAdmissionHookForTests(Runnable hook) {
        transientSignals.setDispatchAdmissionHookForTests(hook);
    }
}
