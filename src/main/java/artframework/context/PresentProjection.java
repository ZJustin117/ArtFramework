package artframework.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ART-owned hard-sync projection of card entities from context frames.
 */
public final class PresentProjection {

    private final Map<String, CardEntity> byInstance = new LinkedHashMap<String, CardEntity>();
    private long lastFrameId = -1L;
    private long sceneEpoch = -1L;
    private String scene = "";
    private boolean available;
    private boolean stale;
    private String dragInstanceId;
    private ContextFrame lastFrame = ContextFrame.unavailable(0L);

    public FrameDiff applyFrame(ContextFrame frame) {
        if (frame == null) {
            return FrameDiff.skipped("frame required");
        }
        if (!frame.available) {
            available = false;
            stale = lastFrameId >= 0;
            lastFrame = frame;
            return FrameDiff.skipped("frame unavailable");
        }
        if (sceneEpoch >= 0L && frame.sceneEpoch != sceneEpoch) {
            // Epoch is Backend authority. A new scene must never inherit card/drag state.
            byInstance.clear();
            dragInstanceId = null;
            lastFrameId = -1L;
        }
        if (frame.frameId < lastFrameId) {
            return FrameDiff.skipped("stale frameId " + frame.frameId + " < " + lastFrameId);
        }
        if (frame.frameId == lastFrameId && lastFrameId >= 0) {
            // same-frame reapply: last wins (replace projection from frame)
        }

        List<String> added = new ArrayList<String>();
        List<String> updated = new ArrayList<String>();
        List<String> seen = new ArrayList<String>();

        for (CardView view : frame.cards) {
            String id = view.ref.instanceId;
            seen.add(id);
            CardEntity entity = byInstance.get(id);
            if (entity == null) {
                entity = new CardEntity(id);
                byInstance.put(id, entity);
                entity.apply(view);
                added.add(id);
            } else {
                entity.apply(view);
                updated.add(id);
            }
        }

        List<String> removed = new ArrayList<String>();
        List<String> toRemove = new ArrayList<String>();
        for (String id : byInstance.keySet()) {
            if (!seen.contains(id)) {
                toRemove.add(id);
            }
        }
        for (String id : toRemove) {
            byInstance.remove(id);
            removed.add(id);
            if (id.equals(dragInstanceId)) {
                dragInstanceId = null;
            }
        }

        lastFrameId = frame.frameId;
        sceneEpoch = frame.sceneEpoch;
        scene = frame.scene;
        available = true;
        stale = false;
        lastFrame = frame;
        return new FrameDiff(added, removed, updated, true, "");
    }

    public long lastFrameId() {
        return lastFrameId;
    }

    public String scene() {
        return scene;
    }

    public long sceneEpoch() {
        return sceneEpoch;
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isStale() {
        return stale;
    }

    public ContextFrame lastFrame() {
        return lastFrame;
    }

    public CardEntity get(String instanceId) {
        return byInstance.get(instanceId);
    }

    public List<CardEntity> list() {
        return Collections.unmodifiableList(new ArrayList<CardEntity>(byInstance.values()));
    }

    public List<CardEntity> listZone(CardZone zone) {
        List<CardEntity> out = new ArrayList<CardEntity>();
        for (CardEntity e : byInstance.values()) {
            if (e.zone == zone) {
                out.add(e);
            }
        }
        return Collections.unmodifiableList(out);
    }

    public int size() {
        return byInstance.size();
    }

    public String dragInstanceId() {
        return dragInstanceId;
    }

    public void setDragInstanceId(String instanceId) {
        this.dragInstanceId = instanceId;
    }

    public void clearDrag() {
        this.dragInstanceId = null;
    }

    public void reset() {
        byInstance.clear();
        lastFrameId = -1L;
        scene = "";
        sceneEpoch = -1L;
        available = false;
        stale = false;
        dragInstanceId = null;
        lastFrame = ContextFrame.unavailable(0L);
    }

    public Map<String, Object> probeSlice() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("frameId", Long.valueOf(lastFrameId));
        m.put("sceneEpoch", Long.valueOf(sceneEpoch));
        m.put("scene", scene);
        m.put("available", Boolean.valueOf(available));
        m.put("stale", Boolean.valueOf(stale));
        m.put("cardCount", Integer.valueOf(byInstance.size()));
        m.put("handCount", Integer.valueOf(listZone(CardZone.HAND).size()));
        m.put("dragInstanceId", dragInstanceId != null ? dragInstanceId : "");
        return m;
    }
}
