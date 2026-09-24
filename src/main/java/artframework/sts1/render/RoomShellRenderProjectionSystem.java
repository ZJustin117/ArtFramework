package artframework.sts1.render;

import artframework.context.RoomShellView;
import artframework.core.PackSystemPhase;
import artframework.core.PackSystems;
import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import artframework.render.ArtRenderContributionComponent;
import artframework.render.RenderPlan;

import java.util.List;

/**
 * Stateless ECS render-projection for the ART room-shell overlay chrome.
 *
 * <p>It samples the host-neutral room-shell observation <em>once per tick</em> and maps that single
 * sample into the two halves of the shared render-frame contract:</p>
 * <ul>
 *   <li>the payload-bearing {@link RenderPlan.Entry} values are published as the {@code room-shell}
 *       {@link ArtRenderContributionComponent contribution}, which the
 *       {@link artframework.render.ArtRenderFrameAggregationSystem} merges into the single immutable
 *       {@link artframework.render.ArtRenderFrame}; and</li>
 *   <li>the non-payload metadata (host-side {@code title} and anchor, plus ownership) is published
 *       as the minimal {@link RoomShellRenderFrame} so the label still draws byte-identically.</li>
 * </ul>
 *
 * <p>Because both halves derive from one {@link Observation} value, metadata and entries can never
 * disagree within a tick (for example a title from a visible shell paired with pixel entries from a
 * later, already-hidden sample).</p>
 *
 * <p>It never resolves a texture and never draws. The {@link Sts1RoomShellDrawPath} consumer reads
 * room-shell pixel entries from the aggregate frame and the label metadata from
 * {@link RoomShellRenderFrameComponent}; it never re-reads the projection at draw time.</p>
 */
public final class RoomShellRenderProjectionSystem implements EcsSystem {
    public static final String SYSTEM_ID = "art.room-shell.render-projection";
    /** Producer id for this family's contribution to the shared aggregate frame. */
    public static final String PRODUCER_ID = "room-shell";

    /**
     * Installs this system into the existing schedule-owned render projection phase. Idempotent
     * (checked against the live registry each call) so a pack reset cannot leave a stale
     * "installed" flag that silently skips re-registration.
     */
    public static synchronized void install() {
        for (EcsSystem system : PackSystems.systemsFor(PackSystemPhase.RENDER_PROJECTION)) {
            if (system instanceof RoomShellRenderProjectionSystem) return;
        }
        PackSystems.enable(PackSystemPhase.RENDER_PROJECTION, SYSTEM_ID,
                new RoomShellRenderProjectionSystem());
    }

    public static synchronized void uninstall() {
        PackSystems.disable(PackSystemPhase.RENDER_PROJECTION, SYSTEM_ID);
    }

    @Override public void run(PresentationWorld world, EcsTick tick) {
        if (world == null || tick == null) throw new IllegalArgumentException("world and tick required");
        // Sample the host observation once; both the metadata frame and the payload contribution are
        // derived from this same value so they can never disagree within a tick.
        Observation observation = sample();
        RoomShellRenderFrame frame = project(observation);
        publishContribution(world, projectEntries(observation));
        if (frame.isEmpty()) {
            removeFrame(world);
        } else {
            publish(world, frame);
        }
    }

    /**
     * One tick's immutable room-shell observation: the host-neutral view and whether the delegated
     * event surface is currently available. Reading this once and passing it to the pure mappers is
     * what keeps metadata and payload entries on the same observation.
     */
    static final class Observation {
        final RoomShellView view;
        final boolean eventSurfaceAvailable;

        Observation(RoomShellView view, boolean eventSurfaceAvailable) {
            this.view = view;
            this.eventSurfaceAvailable = eventSurfaceAvailable;
        }
    }

    /**
     * Test seam so a focused test can pin one observation (or count samples) without touching the
     * host projection source. Production reads the live observation through {@link #sample()}.
     */
    interface ObservationSource {
        Observation sample();
    }

    private static volatile ObservationSource observationSourceForTests;

    static void setObservationSourceForTests(ObservationSource source) {
        observationSourceForTests = source;
    }

    static void resetForTests() {
        observationSourceForTests = null;
    }

    /** Reads the current room-shell observation exactly once. Failure is fail-open to empty. */
    static Observation sample() {
        ObservationSource override = observationSourceForTests;
        if (override != null) {
            try {
                Observation sampled = override.sample();
                return sampled != null ? sampled : new Observation(null, false);
            } catch (Throwable ignored) {
                return new Observation(null, false);
            }
        }
        try {
            return new Observation(artframework.sts1.backend.Sts1RoomShellProjection.current(),
                    eventSurfaceAvailable());
        } catch (Throwable ignored) {
            return new Observation(null, false);
        }
    }

    /**
     * Purely maps one observation to the immutable metadata frame. The event dialog keeps the
     * delegated event surface, so an {@code event} shell is suppressed while that surface is
     * available, exactly as the legacy draw-time filter did.
     */
    static RoomShellRenderFrame project(Observation observation) {
        if (observation == null) return RoomShellRenderFrame.empty();
        return RoomShellRenderFrame.of(observation.view, observation.eventSurfaceAvailable);
    }

    /**
     * Purely maps one observation to the payload entries for the shared aggregate frame. Uses the
     * same filter as {@link #project(Observation)}.
     */
    static List<RenderPlan.Entry> projectEntries(Observation observation) {
        if (observation == null) return java.util.Collections.emptyList();
        return RoomShellRenderFrame.payloadEntries(observation.view, observation.eventSurfaceAvailable);
    }

    private static boolean eventSurfaceAvailable() {
        try {
            return artframework.api.ArtFramework.projection().event().available;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Publishes this family's payload contribution into the shared aggregate pipeline. */
    static void publishContribution(PresentationWorld world, List<RenderPlan.Entry> entries) {
        ArtRenderContributionComponent.publish(world, PRODUCER_ID, entries);
    }

    /** Publishes the metadata frame onto a single shared frame entity, reusing any existing one. */
    static EntityId publish(PresentationWorld world, RoomShellRenderFrame frame) {
        if (world == null) throw new IllegalArgumentException("world required");
        EntityId target = frameEntity(world);
        if (target == null) target = world.createEntity();
        world.put(target, RoomShellRenderFrameComponent.class, new RoomShellRenderFrameComponent(frame));
        return target;
    }

    /** Removes the projected metadata frame so a cleared/recovered scene cannot render stale chrome. */
    static void removeFrame(PresentationWorld world) {
        if (world == null) return;
        while (true) {
            EntityId target = frameEntity(world);
            if (target == null) return;
            world.destroyEntity(target);
        }
    }

    static RoomShellRenderFrame frameFor(PresentationWorld world) {
        if (world == null) return null;
        EntityId target = frameEntity(world);
        if (target == null) return null;
        RoomShellRenderFrameComponent component = world.get(target, RoomShellRenderFrameComponent.class);
        return component == null ? null : component.value;
    }

    private static EntityId frameEntity(PresentationWorld world) {
        java.util.List<EntityId> entities = world.query(RoomShellRenderFrameComponent.class);
        return entities.isEmpty() ? null : entities.get(0);
    }
}
