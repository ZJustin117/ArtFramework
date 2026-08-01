package artframework.ecs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Small, deterministic ECS store for ART-owned presentation state.
 *
 * <p>Signals and subscribers are deliberately outside this world. The world only owns
 * long-lived entities and their component data.</p>
 */
public final class PresentationWorld implements AutoCloseable {
    private final String scope;
    private final Map<EntityId, Map<Class<?>, Object>> entities =
            new LinkedHashMap<EntityId, Map<Class<?>, Object>>();
    private long nextEntity = 1L;
    private boolean open = true;

    public PresentationWorld(String scope) {
        if (scope == null || scope.trim().isEmpty()) {
            throw new IllegalArgumentException("scope required");
        }
        this.scope = scope;
    }

    public String scope() {
        return scope;
    }

    public boolean isOpen() {
        return open;
    }

    public EntityId createEntity() {
        requireOpen();
        EntityId id = new EntityId(scope, nextEntity++);
        entities.put(id, new LinkedHashMap<Class<?>, Object>());
        return id;
    }

    public boolean contains(EntityId id) {
        return id != null && entities.containsKey(id);
    }

    public List<EntityId> entities() {
        requireOpen();
        return Collections.unmodifiableList(new ArrayList<EntityId>(entities.keySet()));
    }

    public <T> void put(EntityId id, Class<T> type, T component) {
        requireEntity(id);
        if (type == null || component == null) {
            throw new IllegalArgumentException("component type and value required");
        }
        if (!type.isInstance(component)) {
            throw new IllegalArgumentException("component does not match type " + type.getName());
        }
        entities.get(id).put(type, component);
    }

    public <T> T get(EntityId id, Class<T> type) {
        requireEntity(id);
        if (type == null) {
            throw new IllegalArgumentException("component type required");
        }
        Object component = entities.get(id).get(type);
        return component == null ? null : type.cast(component);
    }

    public boolean has(EntityId id, Class<?> type) {
        requireEntity(id);
        return type != null && entities.get(id).containsKey(type);
    }

    public <T> T remove(EntityId id, Class<T> type) {
        requireEntity(id);
        if (type == null) {
            throw new IllegalArgumentException("component type required");
        }
        Object removed = entities.get(id).remove(type);
        return removed == null ? null : type.cast(removed);
    }

    public Set<Class<?>> componentTypes(EntityId id) {
        requireEntity(id);
        return Collections.unmodifiableSet(new LinkedHashSet<Class<?>>(entities.get(id).keySet()));
    }

    public boolean destroyEntity(EntityId id) {
        requireOpen();
        return id != null && entities.remove(id) != null;
    }

    public void clear() {
        requireOpen();
        entities.clear();
    }

    @Override
    public void close() {
        if (!open) return;
        entities.clear();
        open = false;
    }

    private void requireOpen() {
        if (!open) throw new IllegalStateException("presentation world is closed");
    }

    private void requireEntity(EntityId id) {
        requireOpen();
        if (id == null || !entities.containsKey(id)) {
            throw new IllegalArgumentException("unknown entity: " + id);
        }
    }
}
