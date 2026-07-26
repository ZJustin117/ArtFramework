package spireui.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Process-global widget sessions for open synthetic composition windows.
 */
public final class WidgetSessions {

    private static final Map<String, WidgetSession> SESSIONS = new LinkedHashMap<String, WidgetSession>();

    private WidgetSessions() {}

    /**
     * Load, expand, and track a composition tree for {@code windowId}.
     */
    public static WidgetSession open(String windowId, String resource) {
        if (windowId == null || windowId.isEmpty()) {
            throw new IllegalArgumentException("windowId required");
        }
        UiNode raw = UiNodeLoader.loadClasspath(resource);
        UiNode expanded = new TemplateExpander().expand(raw);
        return openTree(windowId, expanded);
    }

    public static WidgetSession openTree(String windowId, UiNode expandedRoot) {
        close(windowId);
        WidgetSession session = new WidgetSession(windowId, expandedRoot);
        SESSIONS.put(windowId, session);
        return session;
    }

    public static WidgetSession get(String windowId) {
        if (windowId == null) {
            return null;
        }
        return SESSIONS.get(windowId);
    }

    public static boolean isOpen(String windowId) {
        return windowId != null && SESSIONS.containsKey(windowId);
    }

    public static void close(String windowId) {
        if (windowId != null) {
            SESSIONS.remove(windowId);
        }
    }

    public static List<String> listOpenIds() {
        return Collections.unmodifiableList(new ArrayList<String>(SESSIONS.keySet()));
    }

    public static void resetForTests() {
        SESSIONS.clear();
        ComponentRegistry.resetGlobalForTests();
    }
}
