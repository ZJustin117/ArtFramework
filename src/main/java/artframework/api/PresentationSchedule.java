package artframework.api;

import artframework.context.ContextFrame;
import artframework.core.AnimationPlayers;
import artframework.core.EffectPulse;
import artframework.core.HostBackends;
import artframework.ecs.ArtEcs;
import artframework.ecs.EcsPipeline;
import artframework.ecs.EcsTick;
import artframework.presentation.ControlValueSystem;
import artframework.render.RenderHosts;
import artframework.render.RenderProjectionQueue;
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
            Phase.AUTHORITY_PROJECTION_AND_CONFIRMATION,
            Phase.WORLD_NORMALIZATION,
            Phase.ANIMATION,
            Phase.EFFECTS,
            Phase.HOST_PRESENTATION,
            Phase.RENDER_PROJECTION,
            Phase.RENDER_CLOCK,
            Phase.HOST_BACKEND));
    private final ControlValueSystem controls = new ControlValueSystem();
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
        RenderProjectionQueue.begin();
        try {
            for (Phase phase : PHASES) {
                switch (phase) {
                    case AUTHORITY_PROJECTION_AND_CONFIRMATION:
                        if (authorityFrame != null) ArtFramework.publishFrame(authorityFrame);
                        break;
                    case WORLD_NORMALIZATION:
                        // World systems run once. All registered scopes share ArtEcs.world().
                        EcsPipeline.run(ArtEcs.world(), new EcsTick(deltaSeconds, sequence++),
                                java.util.Collections.<artframework.ecs.EcsSystem>singletonList(controls));
                        break;
                    case ANIMATION:
                        for (String windowId :
                                artframework.presentation.PresentationRuntime.openWindowIds()) {
                            AnimationPlayers.tick(windowId, deltaSeconds);
                        }
                        break;
                    case EFFECTS:
                        EffectPulse.tick(deltaSeconds);
                        break;
                    case HOST_PRESENTATION:
                        if (hostPresentationSystem != null) {
                            hostPresentationSystem.tick(deltaSeconds);
                        }
                        break;
                    case RENDER_PROJECTION:
                        RenderProjectionQueue.flush();
                        break;
                    case RENDER_CLOCK:
                        RenderHosts.get().tick(deltaSeconds);
                        break;
                    case HOST_BACKEND:
                        HostBackends.get().tick(deltaSeconds);
                        break;
                    default:
                        throw new IllegalStateException("unknown presentation phase: " + phase);
                }
            }
        } finally {
            RenderProjectionQueue.flush();
        }
    }

    public void resetForTests() {
        sequence = 0L;
        hostPresentationSystem = null;
        RenderProjectionQueue.resetForTests();
    }
}
