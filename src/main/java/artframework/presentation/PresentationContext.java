package artframework.presentation;

import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * One keyed presentation scope; all mutable node state belongs to the shared ART world.
 *
 * <p>Contexts are obtained from {@link PresentationRegistry#context(String)} so that every scope
 * observes the single {@link artframework.ecs.ArtEcs#world()} authority. A context never owns or
 * closes the world: {@link #close()} destroys only the entities this scope keyed.
 */
public final class PresentationContext implements AutoCloseable {
    private final String scope;
    private final PresentationWorld world;
    private final Map<PresentationKey, EntityId> entities = new LinkedHashMap<PresentationKey, EntityId>();

    PresentationContext(String scope, PresentationWorld world) {
        if (scope == null || scope.trim().isEmpty()) throw new IllegalArgumentException("scope required");
        if (world == null) throw new IllegalArgumentException("world required");
        this.scope = scope;
        this.world = world;
    }
    public String scope() { return scope; }
    public PresentationWorld world() { return world; }

    public EntityId create(PresentationKey key, String name, String type, String source) {
        if (entities.containsKey(key)) throw new IllegalArgumentException("duplicate presentation key: " + key);
        EntityId entity = world.createEntity();
        entities.put(key, entity);
        world.put(entity, NodeIdentityComponent.class, new NodeIdentityComponent(key, name, type, source));
        world.put(entity, NodeHierarchyComponent.class, new NodeHierarchyComponent(null, null));
        world.put(entity, NodeLifecycleComponent.class, new NodeLifecycleComponent(false, false));
        world.put(entity, NodePropertiesComponent.class, new NodePropertiesComponent(null));
        world.put(entity, EffectsComponent.class, new EffectsComponent());
        return entity;
    }

    public EntityId entity(PresentationKey key) {
        EntityId entity = entities.get(key);
        if (entity != null && !world.contains(entity)) {
            entities.remove(key);
            return null;
        }
        return entity;
    }

    /** Entity IDs owned by this named context, in creation order. */
    public List<EntityId> entities() {
        List<EntityId> result = new ArrayList<EntityId>();
        for (EntityId entity : entities.values()) {
            if (world.contains(entity)) result.add(entity);
        }
        return Collections.unmodifiableList(result);
    }
    public PresentationKey key(EntityId entity) {
        NodeIdentityComponent identity = world.get(entity, NodeIdentityComponent.class);
        return identity.key;
    }

    /** Identity key of an entity, or null when it carries no ART node identity. */
    private PresentationKey keyOrNull(EntityId entity) {
        NodeIdentityComponent identity = world.get(entity, NodeIdentityComponent.class);
        return identity != null ? identity.key : null;
    }

    boolean owns(EntityId entity) {
        if (entity == null || !world.contains(entity)) return false;
        PresentationKey key = keyOrNull(entity);
        return key != null && entity.equals(entities.get(key));
    }

    public boolean destroy(EntityId entity) {
        if (entity == null || !world.contains(entity)) return false;
        // Only entities this scope keyed are owned here: the world is shared, so a foreign or
        // unkeyed entity must be rejected rather than dereferenced or destroyed.
        PresentationKey key = keyOrNull(entity);
        if (!owns(entity)) return false;
        PresentationRuntime.clearEntitySignals(this, entity);
        entities.remove(key);
        return world.destroyEntity(entity);
    }

    /** Destroys only this scope's keyed entities; the shared world outlives every context. */
    @Override public void close() {
        PresentationRuntime.clearSignals(this);
        for (EntityId entity : new ArrayList<EntityId>(entities.values())) {
            if (world.contains(entity)) world.destroyEntity(entity);
        }
        entities.clear();
    }
}
