package artframework.api;

import artframework.context.ContextFrame;
import artframework.context.AuthorityFrameComponent;
import artframework.context.AuthorityProjectionSystem;
import artframework.context.BusinessConfirmationSystem;
import artframework.context.FrameDiff;
import artframework.context.NativeIntentLifecycleSystem;
import artframework.context.PresentProjections;
import artframework.context.SurfaceIntentExecutionSystem;
import artframework.context.SurfaceLifecycleSystem;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;
import artframework.core.AnimationPlaybackSystem;
import artframework.core.EffectPulseSystem;
import artframework.core.HostBackendTickSystem;
import artframework.ecs.ArtEcs;
import artframework.ecs.EcsPipeline;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.presentation.ControlValueSystem;
import artframework.render.RenderClockSystem;
import artframework.render.RenderProjectionQueue;
import artframework.render.RenderProjectionSystem;
import artframework.skeleton.SkeletonHostTickSystem;
import artframework.sts1.skeleton.Sts1SkeletonBridge;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Single host-thread schedule for one ART presentation update.
 *
 * <p>Authority projection intentionally runs before simulation so animation and effects observe
 * the frame that will be rendered during this update.</p>
 */
public final class PresentationSchedule {
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
    private final ControlValueSystem controls = new ControlValueSystem();
    private final AuthorityProjectionSystem authority = new AuthorityProjectionSystem();
    private final BusinessConfirmationSystem businessConfirmation =
            new BusinessConfirmationSystem();
    private final NativeIntentLifecycleSystem nativeIntentLifecycle =
            new NativeIntentLifecycleSystem();
    private final SurfaceIntentExecutionSystem surfaceIntentExecution =
            new SurfaceIntentExecutionSystem();
    private final SurfaceLifecycleSystem surfaceLifecycle = new SurfaceLifecycleSystem();
    private final AnimationPlaybackSystem animation = new AnimationPlaybackSystem();
    private final EffectPulseSystem effects = new EffectPulseSystem();
    private final RenderClockSystem renderClock = new RenderClockSystem();
    private final RenderProjectionSystem renderProjection = new RenderProjectionSystem();
    private final HostBackendTickSystem hostBackend = new HostBackendTickSystem();
    private SkeletonHostTickSystem skeleton;
    private HostPresentationSystem hostPresentationSystem;
    private long sequence;

    public List<Phase> phases() {
        return PHASES;
    }

    public void setHostPresentationSystem(HostPresentationSystem system) {
        hostPresentationSystem = system;
    }

    public void advance(float deltaSeconds, ContextFrame authorityFrame) {
        if (deltaSeconds < 0f) {
            throw new IllegalArgumentException("deltaSeconds must be non-negative");
        }
        EcsTick tick = new EcsTick(deltaSeconds, sequence++);
        RenderProjectionQueue.begin();
        try {
            for (Phase phase : PHASES) {
                switch (phase) {
                    case SURFACE_INTENT_EXECUTION:
                        run(tick, surfaceIntentExecution);
                        break;
                    case AUTHORITY_PROJECTION_AND_CONFIRMATION:
                        EntityId authorityEntity = authorityFrame != null
                                ? queueAuthorityFrame(authorityFrame) : null;
                        runAuthorityProjection(tick, authorityEntity);
                        run(tick, businessConfirmation);
                        run(tick, nativeIntentLifecycle);
                        break;
                    case WORLD_NORMALIZATION:
                        // World systems run once. All registered scopes share ArtEcs.world().
                        run(tick, controls);
                        break;
                    case ANIMATION:
                        run(tick, animation);
                        break;
                    case EFFECTS:
                        run(tick, effects);
                        break;
                    case HOST_PRESENTATION:
                        if (hostPresentationSystem != null) {
                            hostPresentationSystem.tick(deltaSeconds);
                        }
                        run(tick, skeletonSystem());
                        break;
                    case RENDER_PROJECTION:
                        run(tick, renderProjection);
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
    }

    /** Runs the schedule-owned command system for synchronous compatibility APIs. */
    public void executeSurfaceIntents() {
        run(new EcsTick(0f, sequence), surfaceIntentExecution);
    }

    /** Runs the schedule-owned lifecycle command system for synchronous surface facades. */
    public void executeSurfaceLifecycle() {
        run(new EcsTick(0f, sequence), surfaceLifecycle);
    }

    /** Processes native lifecycle events at their synchronous host-hook boundary. */
    public void processNativeIntentLifecycle() {
        run(new EcsTick(0f, sequence), nativeIntentLifecycle);
    }

    /** Ingests one compatibility frame through the schedule-owned authority systems. */
    public FrameDiff publishFrame(ContextFrame frame) {
        if (frame == null) return FrameDiff.skipped("frame required");
        EcsTick tick = new EcsTick(0f, sequence);
        EntityId authorityEntity = queueAuthorityFrame(frame);
        runAuthorityProjection(tick, authorityEntity);
        run(tick, businessConfirmation);
        run(tick, nativeIntentLifecycle);
        return PresentProjections.last();
    }

    private static void run(EcsTick tick, artframework.ecs.EcsSystem system) {
        EcsPipeline.run(ArtEcs.world(), tick,
                java.util.Collections.singletonList(system));
    }

    private void runAuthorityProjection(EcsTick tick, EntityId authorityEntity) {
        try {
            run(tick, authority);
        } finally {
            destroyAuthorityEntity(authorityEntity);
        }
    }

    private SkeletonHostTickSystem skeletonSystem() {
        if (skeleton == null) {
            skeleton = new SkeletonHostTickSystem(Sts1SkeletonBridge.presentationSystem());
        }
        return skeleton;
    }

    private static EntityId queueAuthorityFrame(ContextFrame frame) {
        PresentationContext context = PresentationRegistry.context("authority-input");
        PresentationKey key = new PresentationKey("authority.frame", "pending");
        artframework.ecs.EntityId entity = context.entity(key);
        if (entity == null) entity = context.create(key, "pending", "authority-frame", "context");
        context.world().put(entity, AuthorityFrameComponent.class, new AuthorityFrameComponent(frame));
        return entity;
    }

    private static void destroyAuthorityEntity(EntityId entity) {
        if (entity == null) return;
        PresentationRegistry.context("authority-input").destroy(entity);
    }

    public void resetForTests() {
        sequence = 0L;
        hostPresentationSystem = null;
        RenderProjectionQueue.resetForTests();
    }
}
