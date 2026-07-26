package spireui.c1;

import spireui.api.WindowClass;
import spireui.api.WindowDef;
import spireui.c1.layout.LayoutLoader;
import spireui.c1.layout.LayoutNode;

/**
 * C1: synthetic window runtime. Always loads layout DSL; optionally attaches to a
 * {@link StageBackend} when Stage/StsSkin are ready.
 */
public final class SyntheticRuntime {

    private static StageBackend stageBackend;

    private SyntheticRuntime() {}

    /** Logic runtime is ready (layout open/close). */
    public static boolean isAvailable() {
        return true;
    }

    public static boolean isStageReady() {
        return stageBackend != null && stageBackend.isReady();
    }

    public static void installStageBackend(StageBackend backend) {
        stageBackend = backend;
    }

    public static StageBackend stageBackend() {
        return stageBackend;
    }

    /**
     * Load layout for {@code def}, track under {@link WindowManager}, attach to stage if ready.
     */
    public static LayoutNode open(WindowDef def) {
        if (def == null) {
            throw new IllegalArgumentException("def required");
        }
        if (def.windowClass != WindowClass.SYNTHETIC) {
            throw new IllegalArgumentException("expected SYNTHETIC: " + def.id);
        }
        LayoutNode root = LayoutLoader.loadClasspath(def.resource);
        if (root.type != LayoutNode.Type.WINDOW) {
            throw new IllegalArgumentException("layout root must be window: " + def.resource);
        }
        WindowManager.put(def.id, root);
        if (stageBackend != null && stageBackend.isReady()) {
            try {
                stageBackend.attach(def.id, root);
            } catch (RuntimeException e) {
                WindowManager.remove(def.id);
                stageBackend.detach(def.id);
                throw e;
            }
        }
        return root;
    }

    public static void onClosed(String id) {
        if (stageBackend != null) {
            stageBackend.detach(id);
        }
        WindowManager.remove(id);
    }

    public static void resetForTests() {
        stageBackend = null;
        WindowManager.resetForTests();
    }
}
