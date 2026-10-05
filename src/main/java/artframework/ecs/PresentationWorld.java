package artframework.ecs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Small, deterministic ECS store for ART-owned presentation state.
 *
 * <p>Signals and subscribers are deliberately outside this world. The world only owns
 * long-lived entities and their component data.</p>
 */
public final class PresentationWorld implements AutoCloseable {
    private static final AtomicLong NEXT_ENTITY = new AtomicLong(1L);
    private final String scope;
    private final boolean closeable;
    private final Map<EntityId, Map<Class<?>, Object>> entities =
            new LinkedHashMap<EntityId, Map<Class<?>, Object>>();
    /**
     * Creation-ordered entity index for lock-free readers. A concurrent skip list has a weakly
     * consistent iterator that never throws {@link java.util.ConcurrentModificationException}, so
     * the post-native render hook can snapshot the world while the game thread mutates it. The key
     * is {@code EntityId.value()}, which increases monotonically with creation, so natural key
     * order is creation order. Readers never block; writes are O(log n).
     */
    private final ConcurrentSkipListMap<Long, EntityId> order =
            new ConcurrentSkipListMap<Long, EntityId>();
    private boolean open = true;

    public PresentationWorld(String scope) {
        this(scope, true);
    }

    PresentationWorld(String scope, boolean closeable) {
        if (scope == null || scope.trim().isEmpty()) {
            throw new IllegalArgumentException("scope required");
        }
        this.scope = scope;
        this.closeable = closeable;
    }

    public String scope() {
        return scope;
    }

    public boolean isOpen() {
        return open;
    }

    public EntityId createEntity() {
        requireOpen();
        EntityId id = new EntityId(NEXT_ENTITY.getAndIncrement());
        entities.put(id, new LinkedHashMap<Class<?>, Object>());
        order.put(Long.valueOf(id.value()), id);
        return id;
    }

    public boolean contains(EntityId id) {
        return id != null && entities.containsKey(id);
    }

    public List<EntityId> entities() {
        requireOpen();
        List<EntityId> result = new ArrayList<EntityId>(order.size());
        // CME safety comes from iterating the weakly-consistent skip-list index below, not from this
        // guard. The containsKey re-check is only a cheap defensive membership guard.
        for (EntityId id : order.values()) {
            if (entities.containsKey(id)) result.add(id);
        }
        return Collections.unmodifiableList(result);
    }

    public <T> void put(EntityId id, Class<T> type, T component) {
        requireEntity(id);
        if (type == null || component == null) {
            throw new IllegalArgumentException("component type and value required");
        }
        if (!type.isInstance(component)) {
            throw new IllegalArgumentException("component does not match type " + type.getName());
        }
        requireComponents(id).put(type, component);
    }

    public <T> T get(EntityId id, Class<T> type) {
        requireEntity(id);
        if (type == null) {
            throw new IllegalArgumentException("component type required");
        }
        Object component = requireComponents(id).get(type);
        return component == null ? null : type.cast(component);
    }

    /**
     * Lock-free, non-throwing read for the concurrent render path: returns the component or
     * {@code null} when the entity was destroyed (or never existed) or carries no such component.
     * Unlike {@link #get}, a concurrent {@code destroyEntity} between a query and this call cannot
     * throw, so render-hook readers can skip vanished entities.
     */
    public <T> T getIfPresent(EntityId id, Class<T> type) {
        if (id == null || type == null) return null;
        Map<Class<?>, Object> components = entities.get(id);
        if (components == null) return null;
        Object component = components.get(type);
        return component == null ? null : type.cast(component);
    }

    public boolean has(EntityId id, Class<?> type) {
        requireEntity(id);
        return type != null && requireComponents(id).containsKey(type);
    }

    public <T> T remove(EntityId id, Class<T> type) {
        requireEntity(id);
        if (type == null) {
            throw new IllegalArgumentException("component type required");
        }
        Object removed = requireComponents(id).remove(type);
        return removed == null ? null : type.cast(removed);
    }

    public Set<Class<?>> componentTypes(EntityId id) {
        requireEntity(id);
        return Collections.unmodifiableSet(new LinkedHashSet<Class<?>>(requireComponents(id).keySet()));
    }

    /**
     * Returns entities containing every requested component type in creation order.
     * Component values remain data owned by the world; systems must not retain them across ticks.
     */
    public List<EntityId> query(Class<?>... requiredTypes) {
        requireOpen();
        if (requiredTypes == null || requiredTypes.length == 0) {
            return entities();
        }
        List<EntityId> result = new ArrayList<EntityId>();
        // Iterate the concurrent skip-list index: its iterator is weakly consistent and never throws
        // ConcurrentModificationException, so a concurrent createEntity/destroyEntity/clear on the
        // game thread cannot break the render thread. The live entity map is read only via get.
        for (EntityId id : order.values()) {
            Map<Class<?>, Object> components = entities.get(id);
            // Defensive membership re-check, not the CME mechanism: CME safety comes from this
            // weakly-consistent skip-list iteration. If the id is no longer live, skip it.
            if (components == null) continue;
            boolean matches = true;
            for (Class<?> type : requiredTypes) {
                if (type == null || !components.containsKey(type)) {
                    matches = false;
                    break;
                }
            }
            if (matches) result.add(id);
        }
        return Collections.unmodifiableList(result);
    }

    public boolean destroyEntity(EntityId id) {
        requireOpen();
        if (id == null || entities.remove(id) == null) return false;
        order.remove(Long.valueOf(id.value()));
        return true;
    }

    public void clear() {
        requireOpen();
        entities.clear();
        order.clear();
    }

    @Override
    public void close() {
        if (!closeable) {
            throw new IllegalStateException("presentation world is registry-owned");
        }
        if (!open) return;
        entities.clear();
        order.clear();
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

    /**
     * Resolves an entity's component map or throws the documented unknown-entity exception. Used by
     * the mutating/authoritative accessors: unlike a raw {@code entities.get(id)} they fail with the
     * contract exception rather than NPE if the entity vanished between the {@code requireEntity}
     * check and the read.
     */
    private Map<Class<?>, Object> requireComponents(EntityId id) {
        Map<Class<?>, Object> components = id == null ? null : entities.get(id);
        if (components == null) throw new IllegalArgumentException("unknown entity: " + id);
        return components;
    }
}
