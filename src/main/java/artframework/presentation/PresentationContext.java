package artframework.presentation;

import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** One keyed presentation scope; all mutable node state belongs to its world. */
public final class PresentationContext implements AutoCloseable {
    private final String scope;
    private final PresentationWorld world;
    private final boolean ownsWorld;
    private final Map<PresentationKey, EntityId> entities = new LinkedHashMap<PresentationKey, EntityId>();

    public PresentationContext(String scope) {
        this(scope, new PresentationWorld(scope), true);
    }

    PresentationContext(String scope, PresentationWorld world) {
        this(scope, world, false);
    }

    private PresentationContext(String scope, PresentationWorld world, boolean ownsWorld) {
        if (scope == null || scope.trim().isEmpty()) throw new IllegalArgumentException("scope required");
        if (world == null) throw new IllegalArgumentException("world required");
        this.scope = scope;
        this.world = world;
        this.ownsWorld = ownsWorld;
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

    public boolean destroy(EntityId entity) {
        if (entity == null || !world.contains(entity)) return false;
        entities.remove(key(entity));
        return world.destroyEntity(entity);
    }

    @Override public void close() {
        for (EntityId entity : new ArrayList<EntityId>(entities.values())) {
            if (world.contains(entity)) world.destroyEntity(entity);
        }
        entities.clear();
        if (ownsWorld) world.close();
    }
}
