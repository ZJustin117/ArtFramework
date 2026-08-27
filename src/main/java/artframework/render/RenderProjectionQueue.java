package artframework.render;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/** Coalesces C1 host-cache projection while the production schedule mutates ECS visuals. */
public final class RenderProjectionQueue {
    private static final Set<String> DIRTY_WINDOWS = new LinkedHashSet<String>();
    private static int batchDepth;
    private static boolean fullProjectionRequested;
    private static int rebuildCountForTests;

    private RenderProjectionQueue() {}

    public static void begin() {
        batchDepth++;
    }

    public static void request(String windowId) {
        if (windowId == null || windowId.isEmpty()) return;
        if (batchDepth > 0) {
            DIRTY_WINDOWS.add(windowId);
        } else {
            rebuild();
        }
    }

    /** Immediately project the complete immutable ECS render plan for a lifecycle boundary. */
    public static void projectNow() {
        if (batchDepth > 0) {
            fullProjectionRequested = true;
        } else {
            rebuild();
        }
    }

    /** Immediately project only active C2 surfaces for an STS render pass. */
    public static void projectActiveSurfaces(Set<String> activeSurfaceIds) {
        RenderHosts.get().rebuildActiveC2SurfacesFromEcsPlan(activeSurfaceIds);
    }

    public static void flush() {
        if (batchDepth == 0) return;
        batchDepth--;
        if (batchDepth > 0) return;
        try {
            if (fullProjectionRequested || !DIRTY_WINDOWS.isEmpty()) rebuild();
        } finally {
            DIRTY_WINDOWS.clear();
            fullProjectionRequested = false;
        }
    }

    public static void resetForTests() {
        DIRTY_WINDOWS.clear();
        batchDepth = 0;
        fullProjectionRequested = false;
        rebuildCountForTests = 0;
    }

    public static int rebuildCountForTests() {
        return rebuildCountForTests;
    }

    private static void rebuild() {
        rebuildCountForTests++;
        RenderHosts.get().rebuildFromEcsPlan();
    }
}
