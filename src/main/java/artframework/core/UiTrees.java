package artframework.core;

import artframework.component.UiNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Process-global mounted {@link UiTree}s for synthetic windows.
 */
public final class UiTrees {

    private static final Map<String, UiTree> TREES = new LinkedHashMap<String, UiTree>();

    private UiTrees() {}

    public static UiTree open(String windowId, UiNode expandedRoot) {
        return open(windowId, expandedRoot, null);
    }

    public static UiTree open(String windowId, UiNode expandedRoot, TreeLifecycle lifecycle) {
        close(windowId);
        UiTree tree = UiTree.mount(windowId, expandedRoot, lifecycle);
        TREES.put(windowId, tree);
        return tree;
    }

    public static UiTree get(String windowId) {
        if (windowId == null) {
            return null;
        }
        return TREES.get(windowId);
    }

    public static boolean isOpen(String windowId) {
        return windowId != null && TREES.containsKey(windowId);
    }

    public static void close(String windowId) {
        if (windowId == null) {
            return;
        }
        UiTree t = TREES.remove(windowId);
        if (t != null) {
            t.unmount();
        }
    }

    public static List<String> listOpenIds() {
        return Collections.unmodifiableList(new ArrayList<String>(TREES.keySet()));
    }

    public static void resetForTests() {
        List<String> ids = new ArrayList<String>(TREES.keySet());
        for (String id : ids) {
            close(id);
        }
        TREES.clear();
    }
}
