package artframework.render;

import artframework.ecs.PresentationWorld;

import java.util.List;

/**
 * Shared ECS publication point for the current immutable {@link ArtRenderFrame}.
 *
 * <p>The aggregation system is the only writer; backend consumers use the single
 * {@link #read(PresentationWorld)} entry point. The component holds only the host-neutral,
 * immutable aggregate (no host object, {@code EntityId}, {@code PresentationWorld}, or
 * {@code Texture}) and is removed on an empty tick, so it carries no cross-tick state.</p>
 *
 * <p>Aggregation rejection diagnostics deliberately do <em>not</em> live here: a component that is
 * published only with real content and removed on an empty tick cannot accumulate a counter across
 * empty ticks. Those diagnostics live on the frame-independent
 * {@link ArtRenderFrameDiagnosticsComponent}; {@link #rejectedCount(PresentationWorld)} and
 * {@link #rejectionReason(PresentationWorld)} remain as a convenience facade that delegates to it.</p>
 */
public final class ArtRenderFrameComponent {
    public final ArtRenderFrame value;

    public ArtRenderFrameComponent(ArtRenderFrame value) {
        this.value = value == null ? ArtRenderFrame.empty() : value;
    }

    /**
     * Single read entry for backend consumers. Returns the published aggregate, or the safe empty
     * frame when nothing has been published (for example before the first aggregation tick or after
     * a clear). Never {@code null}. A published frame that represents a rejected aggregation is the
     * safe empty frame, so consumers never draw a stale prior aggregate.
     */
    public static ArtRenderFrame read(PresentationWorld world) {
        if (world == null) return ArtRenderFrame.empty();
        List<artframework.ecs.EntityId> entities = world.query(ArtRenderFrameComponent.class);
        if (entities.isEmpty()) return ArtRenderFrame.empty();
        ArtRenderFrameComponent component =
                world.get(entities.get(0), ArtRenderFrameComponent.class);
        return component == null ? ArtRenderFrame.empty() : component.value;
    }

    /**
     * Cumulative rejected-aggregation count. Delegates to the durable
     * {@link ArtRenderFrameDiagnosticsComponent} so it survives empty ticks (when this component is
     * removed); {@code 0} when nothing has been rejected.
     */
    public static long rejectedCount(PresentationWorld world) {
        return ArtRenderFrameDiagnosticsComponent.rejectedCount(world);
    }

    /** Last rejection reason, or {@code ""} when none/cleared. Never {@code null}. */
    public static String rejectionReason(PresentationWorld world) {
        return ArtRenderFrameDiagnosticsComponent.rejectionReason(world);
    }
}
