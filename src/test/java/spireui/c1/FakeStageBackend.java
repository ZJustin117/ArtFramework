package spireui.c1;

import spireui.c1.layout.LayoutNode;
import spireui.component.UiNode;

import java.util.LinkedHashMap;
import java.util.Map;

/** In-memory {@link StageBackend} for pure unit tests (no LibGDX GL). */
public final class FakeStageBackend implements StageBackend {

    private boolean ready = true;
    private final Map<String, LayoutNode> attached = new LinkedHashMap<String, LayoutNode>();
    private final Map<String, UiNode> attachedComposition = new LinkedHashMap<String, UiNode>();

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
        attachedComposition.remove(id);
        attached.put(id, root);
    }

    @Override
    public void attachComposition(String id, UiNode root) {
        if (!ready) {
            return;
        }
        if (id == null || root == null) {
            throw new IllegalArgumentException("id and root required");
        }
        attached.remove(id);
        attachedComposition.put(id, root);
    }

    @Override
    public void detach(String id) {
        attached.remove(id);
        attachedComposition.remove(id);
    }

    @Override
    public boolean isAttached(String id) {
        return attached.containsKey(id) || attachedComposition.containsKey(id);
    }

    @Override
    public int attachedCount() {
        return attached.size() + attachedComposition.size();
    }

    public LayoutNode getAttached(String id) {
        return attached.get(id);
    }

    public UiNode getAttachedComposition(String id) {
        return attachedComposition.get(id);
    }

    public void clear() {
        attached.clear();
        attachedComposition.clear();
    }
}
