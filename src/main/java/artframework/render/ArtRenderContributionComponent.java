package artframework.render;

import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shared ECS publication point for one render family's payload contribution.
 *
 * <p>A producer publishes its payload-bearing {@link RenderPlan.Entry} values here, keyed by a
 * stable {@code producerId}; the {@link ArtRenderFrameAggregationSystem} merges every contribution
 * into the single immutable {@link ArtRenderFrame}. The component is data only: it holds no host
 * object, {@code EntityId}, {@code PresentationWorld}, or {@code Texture}. The entry list is
 * defensively copied and unmodifiable, so a producer cannot mutate a published contribution.</p>
 *
 * <p>The producer id keeps each family on its own contribution entity: publishing or clearing one
 * family never disturbs another, and the aggregated frame stays a pure merge.</p>
 */
public final class ArtRenderContributionComponent {
    public final String producerId;
    public final List<RenderPlan.Entry> entries;

    public ArtRenderContributionComponent(String producerId, List<RenderPlan.Entry> entries) {
        if (producerId == null || producerId.isEmpty()) {
            throw new IllegalArgumentException("producer id required");
        }
        this.producerId = producerId;
        this.entries = entries == null || entries.isEmpty()
                ? Collections.<RenderPlan.Entry>emptyList()
                : Collections.unmodifiableList(new ArrayList<RenderPlan.Entry>(entries));
    }

    /**
     * Publishes (or replaces) one producer's payload contribution, reusing that producer's
     * existing contribution entity when present. A producer with no entries clears its
     * contribution instead of publishing an empty one.
     */
    public static void publish(PresentationWorld world, String producerId,
            List<RenderPlan.Entry> entries) {
        if (world == null) throw new IllegalArgumentException("world required");
        if (entries == null || entries.isEmpty()) {
            clear(world, producerId);
            return;
        }
        EntityId target = entityFor(world, producerId);
        if (target == null) target = world.createEntity();
        world.put(target, ArtRenderContributionComponent.class,
                new ArtRenderContributionComponent(producerId, entries));
    }

    /** Removes one producer's contribution so a cleared scene cannot render stale entries. */
    public static void clear(PresentationWorld world, String producerId) {
        if (world == null) return;
        while (true) {
            EntityId target = entityFor(world, producerId);
            if (target == null) return;
            world.destroyEntity(target);
        }
    }

    /** Reads one producer's current contribution, or {@code null} when it has none. */
    public static List<RenderPlan.Entry> contributionFor(PresentationWorld world, String producerId) {
        if (world == null) return null;
        EntityId target = entityFor(world, producerId);
        if (target == null) return null;
        ArtRenderContributionComponent component =
                world.get(target, ArtRenderContributionComponent.class);
        return component == null ? null : component.entries;
    }

    private static EntityId entityFor(PresentationWorld world, String producerId) {
        for (EntityId entity : world.query(ArtRenderContributionComponent.class)) {
            ArtRenderContributionComponent component =
                    world.get(entity, ArtRenderContributionComponent.class);
            if (component != null && component.producerId.equals(producerId)) return entity;
        }
        return null;
    }
}
