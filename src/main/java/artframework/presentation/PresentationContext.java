package artframework.presentation;

import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import java.util.LinkedHashMap;
import java.util.Map;

/** One keyed presentation scope; all mutable node state belongs to its world. */
public final class PresentationContext implements AutoCloseable {
    private final PresentationWorld world;
    private final Map<PresentationKey, EntityId> entities = new LinkedHashMap<PresentationKey, EntityId>();

    public PresentationContext(String scope) { world = new PresentationWorld(scope); }
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
    public PresentationKey key(EntityId entity) {
        NodeIdentityComponent identity = world.get(entity, NodeIdentityComponent.class);
        return identity.key;
    }

    public boolean destroy(EntityId entity) {
        if (entity == null || !world.contains(entity)) return false;
        entities.remove(key(entity));
        return world.destroyEntity(entity);
    }

    @Override public void close() { entities.clear(); world.close(); }
}
