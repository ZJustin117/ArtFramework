package artframework.c2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory {@link EntityPresent}. No GL; safe for unit tests and pre-render wiring.
 */
public final class DefaultEntityPresent implements EntityPresent {

    private final Map<String, EntitySlot> slots = new LinkedHashMap<String, EntitySlot>();
    private final CopyOnWriteArrayList<EntityPresentListener> listeners =
            new CopyOnWriteArrayList<EntityPresentListener>();

    @Override
    public void attach(String slotId, String kind, String refId) {
        EntityKind k = EntityKind.parse(kind);
        if (slotId == null || slotId.isEmpty()) {
            throw new IllegalArgumentException("slotId required");
        }
        EntitySlot previous = slots.get(slotId);
        if (previous != null) {
            slots.remove(slotId);
            fireDetached(slotId);
        }
        EntitySlot slot = new EntitySlot(slotId, k, refId);
        slots.put(slotId, slot);
        for (EntityPresentListener l : listeners) {
            l.onAttached(slot);
        }
    }

    @Override
    public void sync(String slotId, Object snapshotDto) {
        EntitySlot slot = require(slotId);
        slot.setSnapshot(snapshotDto);
        for (EntityPresentListener l : listeners) {
            l.onSynced(slot);
        }
    }

    @Override
    public void layout(String slotId, float x, float y, float scale) {
        EntitySlot slot = require(slotId);
        slot.setLayout(x, y, scale);
        for (EntityPresentListener l : listeners) {
            l.onLaidOut(slot);
        }
    }

    @Override
    public void detach(String slotId) {
        if (slotId == null) {
            return;
        }
        if (slots.remove(slotId) != null) {
            fireDetached(slotId);
        }
    }

    @Override
    public boolean isAttached(String slotId) {
        return slots.containsKey(slotId);
    }

    @Override
    public EntitySlot get(String slotId) {
        return slots.get(slotId);
    }

    @Override
    public List<String> listSlotIds() {
        return Collections.unmodifiableList(new ArrayList<String>(slots.keySet()));
    }

    @Override
    public int size() {
        return slots.size();
    }

    @Override
    public void clear() {
        if (slots.isEmpty()) {
            return;
        }
        List<String> ids = new ArrayList<String>(slots.keySet());
        slots.clear();
        for (String id : ids) {
            fireDetached(id);
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

    void resetForTests() {
        slots.clear();
        listeners.clear();
    }

    private EntitySlot require(String slotId) {
        EntitySlot slot = slots.get(slotId);
        if (slot == null) {
            throw new IllegalArgumentException("not attached: " + slotId);
        }
        return slot;
    }

    private void fireDetached(String slotId) {
        for (EntityPresentListener l : listeners) {
            l.onDetached(slotId);
        }
    }
}
