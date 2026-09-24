package artframework.render;

import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

import java.util.List;

/**
 * Host-neutral, frame-independent diagnostics for the shared frame aggregation pass.
 *
 * <p>The {@link ArtRenderFrameComponent} is published only when there is real content and is
 * removed on an empty tick, so it cannot carry a cumulative counter across empty ticks. This
 * component is the durable home for aggregation rejection diagnostics: the aggregation system
 * creates one diagnostics entity per world and updates it on every rejected run, while a
 * successful (non-empty) run clears only the reason. The cumulative count therefore survives
 * reject -> empty-tick -> reject sequences.</p>
 *
 * <p>The component holds plain data only: no host object, {@code EntityId},
 * {@code PresentationWorld}, {@code Texture}, and no draw frame. The single mutable draw state
 * remains {@link ArtRenderFrameComponent}; this is a diagnostic side channel, not a second frame.</p>
 */
public final class ArtRenderFrameDiagnosticsComponent {
    /**
     * Cumulative number of aggregation runs whose merged contributions were rejected (for example a
     * duplicate stable key) and therefore could not be published as a real frame.
     */
    public final long rejectedCount;
    /** Last rejection reason, or {@code ""} once a later run aggregated successfully. */
    public final String rejectionReason;

    public ArtRenderFrameDiagnosticsComponent(long rejectedCount, String rejectionReason) {
        this.rejectedCount = rejectedCount < 0L ? 0L : rejectedCount;
        this.rejectionReason = rejectionReason == null ? "" : rejectionReason;
    }

    /**
     * Creates or updates the single shared diagnostics entry. Idempotent per world: the first call
     * creates the entity, later calls reuse it so the counter accumulates.
     */
    public static void record(PresentationWorld world, long rejectedCount, String rejectionReason) {
        if (world == null) throw new IllegalArgumentException("world required");
        EntityId target = entityFor(world);
        if (target == null) target = world.createEntity();
        world.put(target, ArtRenderFrameDiagnosticsComponent.class,
                new ArtRenderFrameDiagnosticsComponent(rejectedCount, rejectionReason));
    }

    /** Cumulative rejected-aggregation count, or {@code 0} when no diagnostics are recorded. */
    public static long rejectedCount(PresentationWorld world) {
        ArtRenderFrameDiagnosticsComponent component = published(world);
        return component == null ? 0L : component.rejectedCount;
    }

    /** Last rejection reason, or {@code ""} when none/cleared. Never {@code null}. */
    public static String rejectionReason(PresentationWorld world) {
        ArtRenderFrameDiagnosticsComponent component = published(world);
        return component == null ? "" : component.rejectionReason;
    }

    private static ArtRenderFrameDiagnosticsComponent published(PresentationWorld world) {
        if (world == null) return null;
        List<EntityId> entities = world.query(ArtRenderFrameDiagnosticsComponent.class);
        if (entities.isEmpty()) return null;
        return world.get(entities.get(0), ArtRenderFrameDiagnosticsComponent.class);
    }

    private static EntityId entityFor(PresentationWorld world) {
        List<EntityId> entities = world.query(ArtRenderFrameDiagnosticsComponent.class);
        return entities.isEmpty() ? null : entities.get(0);
    }
}
