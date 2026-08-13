package artframework.c1;

import artframework.c1.layout.LayoutNode;
import artframework.c1.layout.LayoutNodeBridge;
import artframework.component.WidgetSession;
import artframework.component.WidgetSessions;


/**
 * Legacy layout adapter over the immutable C1 declaration cache. It owns no window state.
 */
public final class WindowManager {

    private WindowManager() {}

    public static void put(String id, LayoutNode root) {
        // Kept for source compatibility. WidgetSessions/NodeTree own the declaration lifecycle.
    }

    public static LayoutNode get(String id) {
        WidgetSession session = WidgetSessions.get(id);
        return session != null ? LayoutNodeBridge.toLegacyOrNull(session.root()) : null;
    }

    public static void remove(String id) {
        // WidgetSessions/NodeTree own the declaration lifecycle.
    }

    public static boolean contains(String id) {
        return WidgetSessions.isOpen(id);
    }

    public static void resetForTests() {
        // WidgetSessions owns test cleanup.
    }
}
