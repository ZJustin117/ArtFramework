package artframework.c2;

import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import artframework.presentation.HostBindingComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;
import artframework.render.RenderProjectionQueue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory {@link EntityPresent}. No GL; safe for unit tests and pre-render wiring.
 */
public final class DefaultEntityPresent implements EntityPresent {

    private static final String CONTEXT_SCOPE = "c2-entity-present";
    private final CopyOnWriteArrayList<EntityPresentListener> listeners =
            new CopyOnWriteArrayList<EntityPresentListener>();

    @Override
    public void attach(String slotId, String kind, String refId) {
        PresentationContext context = context();
        PresentationWorld world = context.world();
        EntityKind k = EntityKind.parse(kind);
        if (slotId == null || slotId.isEmpty()) {
            throw new IllegalArgumentException("slotId required");
        }
        EntityId previous = entityId(slotId);
        if (previous != null) {
            context.destroy(previous);
            fireDetached(slotId);
        }
        PresentationKey key = new PresentationKey("entity.slot", slotId);
        EntityId entity = context.entity(key);
        if (entity == null) {
            entity = context.create(key, slotId, "entity-slot", "entity-present");
        }
        world.put(entity, EntitySlotIdentityComponent.class,
                new EntitySlotIdentityComponent(slotId, k, refId));
        world.put(entity, EntitySlotSnapshotComponent.class,
                new EntitySlotSnapshotComponent(null));
        world.put(entity, EntitySlotTransformComponent.class,
                new EntitySlotTransformComponent(0f, 0f, 1f, false));
        world.put(entity, HostBindingComponent.class,
                new HostBindingComponent("STS1_ENTITY", slotId));
        fireAttached(view(entity));
    }

    @Override
    public void sync(String slotId, Object snapshotDto) {
        PresentationWorld world = world();
        EntityId entity = requireEntity(slotId);
        world.put(entity, EntitySlotSnapshotComponent.class,
                new EntitySlotSnapshotComponent(EntitySnapshot.normalize(snapshotDto)));
        for (EntityPresentListener l : listeners) {
            l.onSynced(view(entity));
        }
    }

    @Override
    public void layout(String slotId, float x, float y, float scale) {
        PresentationWorld world = world();
        EntityId entity = requireEntity(slotId);
        world.put(entity, EntitySlotTransformComponent.class,
                new EntitySlotTransformComponent(x, y, scale, true));
        for (EntityPresentListener l : listeners) {
            l.onLaidOut(view(entity));
        }
    }

    @Override
    public void present(String slotId, String kind, String refId, Object snapshotDto,
            float x, float y, float scale) {
        RenderProjectionQueue.begin();
        try {
            attach(slotId, kind, refId);
            sync(slotId, snapshotDto);
            layout(slotId, x, y, scale);
        } finally {
            RenderProjectionQueue.flush();
        }
    }

    @Override
    public void detach(String slotId) {
        if (slotId == null) {
            return;
        }
        RenderProjectionQueue.begin();
        try {
            EntityId entity = entityId(slotId);
            if (entity != null) {
                context().destroy(entity);
                fireDetached(slotId);
                RenderProjectionQueue.projectNow();
            }
        } finally {
            RenderProjectionQueue.flush();
        }
    }

    @Override
    public boolean isAttached(String slotId) {
        return entityId(slotId) != null;
    }

    @Override
    public EntitySlot get(String slotId) {
        EntityId entity = entityId(slotId);
        return entity != null ? view(entity) : null;
    }

    @Override
    public List<String> listSlotIds() {
        PresentationWorld world = world();
        List<String> result = new ArrayList<String>();
        for (EntityId entity : world.query(EntitySlotIdentityComponent.class)) {
            result.add(world.get(entity, EntitySlotIdentityComponent.class).slotId);
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public int size() {
        return world().query(EntitySlotIdentityComponent.class).size();
    }

    @Override
    public void clear() {
        RenderProjectionQueue.begin();
        try {
            boolean changed = false;
            for (String id : new ArrayList<String>(listSlotIds())) {
                EntityId entity = entityId(id);
                if (entity != null) {
                    context().destroy(entity);
                    fireDetached(id);
                    changed = true;
                }
            }
            if (changed) RenderProjectionQueue.projectNow();
        } finally {
            RenderProjectionQueue.flush();
        }
    }

    public void addListener(EntityPresentListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener required");
        }
        listeners.add(listener);
    }

    public void removeListener(EntityPresentListener listener) {
        listeners.remove(listener);
    }

    public int listenerCount() {
        return listeners.size();
    }

    /** Internal ECS world; the EntityPresent API remains the compatibility facade. */
    public PresentationWorld world() {
        return context().world();
    }

    public EntityId entityId(String slotId) {
        if (slotId == null) return null;
        return context().entity(new PresentationKey("entity.slot", slotId));
    }

    void resetForTests() {
        PresentationContext context = context();
        PresentationWorld world = context.world();
        for (EntityId entity : new ArrayList<EntityId>(world.query(EntitySlotIdentityComponent.class))) {
            context.destroy(entity);
        }
        listeners.clear();
    }

    private EntityId requireEntity(String slotId) {
        EntityId entity = entityId(slotId);
        if (entity == null) {
            throw new IllegalArgumentException("not attached: " + slotId);
        }
        return entity;
    }

    private EntitySlot view(EntityId entity) {
        PresentationWorld world = world();
        EntitySlotIdentityComponent identity = world.get(entity, EntitySlotIdentityComponent.class);
        EntitySlotSnapshotComponent snapshot = world.get(entity, EntitySlotSnapshotComponent.class);
        EntitySlotTransformComponent transform = world.get(entity, EntitySlotTransformComponent.class);
        return new EntitySlot(identity.slotId, identity.kind, identity.refId,
                snapshot != null ? snapshot.snapshot : null,
                transform != null ? transform.x : 0f,
                transform != null ? transform.y : 0f,
                transform != null ? transform.scale : 1f,
                transform != null && transform.laidOut);
    }

    private void fireAttached(EntitySlot slot) {
        for (EntityPresentListener l : listeners) {
            l.onAttached(slot);
        }
    }

    private void fireDetached(String slotId) {
        for (EntityPresentListener l : listeners) {
            l.onDetached(slotId);
        }
    }

    private PresentationContext context() {
        return PresentationRegistry.context(CONTEXT_SCOPE);
    }
}
