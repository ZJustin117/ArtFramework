package artframework.presentation;

import artframework.component.UiNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Process-global mounted C1 NodeTrees. */
public final class NodeTrees {
    private static final Map<String, NodeTree> TREES = new LinkedHashMap<String, NodeTree>();
    private NodeTrees() {}

    public static NodeTree open(String windowId, UiNode root, NodeTreeLifecycle lifecycle) {
        close(windowId);
        NodeTree tree = NodeTree.mount("tree:" + windowId, root, lifecycle);
        TREES.put(windowId, tree);
        return tree;
    }

    public static NodeTree get(String windowId) { return windowId == null ? null : TREES.get(windowId); }
    public static boolean isOpen(String windowId) { return windowId != null && TREES.containsKey(windowId); }
    public static List<String> listOpenIds() { return Collections.unmodifiableList(new ArrayList<String>(TREES.keySet())); }
    public static List<NodeTree> listOpen() { return Collections.unmodifiableList(new ArrayList<NodeTree>(TREES.values())); }

    public static void close(String windowId) {
        NodeTree tree = TREES.remove(windowId);
        if (tree != null) tree.close();
    }

    public static void resetForTests() {
        for (String id : new ArrayList<String>(TREES.keySet())) close(id);
    }
}
