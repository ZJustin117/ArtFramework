package artframework.render;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/** Coalesces C1 host-cache projection while the production schedule mutates ECS visuals. */
public final class RenderProjectionQueue {
    private static final Set<String> DIRTY_WINDOWS = new LinkedHashSet<String>();
    private static boolean batching;

    private RenderProjectionQueue() {}

    public static void begin() {
        batching = true;
    }

    public static void request(String windowId) {
        if (windowId == null || windowId.isEmpty()) return;
        if (batching) {
            DIRTY_WINDOWS.add(windowId);
        } else {
            RenderHosts.get().rebuildFromEcsPlan();
        }
    }

    public static void flush() {
        try {
            if (!DIRTY_WINDOWS.isEmpty()) RenderHosts.get().rebuildFromEcsPlan();
        } finally {
            DIRTY_WINDOWS.clear();
            batching = false;
        }
    }

    public static void resetForTests() {
        DIRTY_WINDOWS.clear();
        batching = false;
    }
}
