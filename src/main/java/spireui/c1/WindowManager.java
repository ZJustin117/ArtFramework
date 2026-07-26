package spireui.c1;

import spireui.c1.layout.LayoutNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure C1 open-window bookkeeping (layout roots). Scene2d Stage attach is later.
 */
public final class WindowManager {

    private static final Map<String, LayoutNode> ROOTS = new LinkedHashMap<String, LayoutNode>();

    private WindowManager() {}

    public static void put(String id, LayoutNode root) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id required");
        }
        if (root == null) {
            throw new IllegalArgumentException("root required");
        }
        ROOTS.put(id, root);
    }

    public static LayoutNode get(String id) {
        return ROOTS.get(id);
    }

    public static void remove(String id) {
        ROOTS.remove(id);
    }

    public static boolean contains(String id) {
        return ROOTS.containsKey(id);
    }

    public static Map<String, LayoutNode> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<String, LayoutNode>(ROOTS));
    }

    public static void resetForTests() {
        ROOTS.clear();
    }
}
