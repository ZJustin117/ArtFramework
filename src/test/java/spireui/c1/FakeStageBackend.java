package spireui.c1;

import spireui.c1.layout.LayoutNode;

import java.util.LinkedHashMap;
import java.util.Map;

/** In-memory {@link StageBackend} for pure unit tests (no LibGDX GL). */
public final class FakeStageBackend implements StageBackend {

    private boolean ready = true;
    private final Map<String, LayoutNode> attached = new LinkedHashMap<String, LayoutNode>();

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void attach(String id, LayoutNode root) {
        if (!ready) {
            return;
        }
        if (id == null || root == null) {
            throw new IllegalArgumentException("id and root required");
        }
        attached.put(id, root);
    }

    @Override
    public void detach(String id) {
        attached.remove(id);
    }

    @Override
    public boolean isAttached(String id) {
        return attached.containsKey(id);
    }

    @Override
    public int attachedCount() {
        return attached.size();
    }

    public LayoutNode getAttached(String id) {
        return attached.get(id);
    }

    public void clear() {
        attached.clear();
    }
}
