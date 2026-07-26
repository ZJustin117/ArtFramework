package artframework.c1;

import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.c1.layout.LayoutNode;
import artframework.c1.layout.LayoutNodeBridge;
import artframework.component.LmlUiNodeLoader;
import artframework.component.TemplateExpander;
import artframework.component.UiNode;
import artframework.component.UiNodeLoader;
import artframework.component.UiTypes;
import artframework.component.WidgetSession;
import artframework.component.WidgetSessions;
import artframework.core.UiTrees;
import artframework.render.RenderHosts;

/**
 * C1: synthetic window runtime. Loads composition DSL, opens {@link WidgetSession},
 * syncs {@link RenderHosts}, and attaches Stage via composition or legacy path.
 */
public final class SyntheticRuntime {

    private static StageBackend stageBackend;

    private SyntheticRuntime() {}

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
     * Load layout for {@code def}, track under {@link WindowManager}, open widget session,
     * sync render targets, attach to stage if ready.
     */
    public static LayoutNode open(WindowDef def) {
        if (def == null) {
            throw new IllegalArgumentException("def required");
        }
        if (def.windowClass != WindowClass.SYNTHETIC) {
            throw new IllegalArgumentException("expected SYNTHETIC: " + def.id);
        }

        UiNode uiRoot = loadLayout(def.resource);
        if (!UiTypes.WINDOW.equals(uiRoot.type)) {
            throw new IllegalArgumentException("layout root must be window: " + def.resource);
        }
        uiRoot = new TemplateExpander().expand(uiRoot);

        WidgetSession session = WidgetSessions.openTree(def.id, uiRoot);
        UiTrees.open(def.id, session.root());
        LayoutNode legacy = LayoutNodeBridge.toLegacyOrNull(session.root());
        if (legacy == null) {
            legacy = LayoutNode.window(
                    session.root().id,
                    session.root().propString("title", def.id),
                    session.root().layout.width,
                    session.root().layout.height,
                    java.util.Collections.<LayoutNode>emptyList());
        }

        WindowManager.put(def.id, legacy);
        try {
            RenderHosts.get().syncWidgetSession(session);
        } catch (RuntimeException ignored) {
        }

        if (stageBackend != null && stageBackend.isReady()) {
            try {
                if (LayoutNodeBridge.isLegacyTree(session.root())) {
                    stageBackend.attach(def.id, legacy);
                } else {
                    stageBackend.attachComposition(def.id, session.root());
                }
            } catch (RuntimeException e) {
                WindowManager.remove(def.id);
                WidgetSessions.close(def.id);
                UiTrees.close(def.id);
                RenderHosts.get().detachWidgetSession(def.id);
                stageBackend.detach(def.id);
                throw e;
            }
        }
        return legacy;
    }

    public static WidgetSession openComposition(WindowDef def) {
        open(def);
        return WidgetSessions.get(def.id);
    }

    public static WidgetSession widgetSession(String id) {
        return WidgetSessions.get(id);
    }

    public static void onClosed(String id) {
        if (stageBackend != null) {
            stageBackend.detach(id);
        }
        WindowManager.remove(id);
        WidgetSessions.close(id);
        UiTrees.close(id);
        RenderHosts.get().detachWidgetSession(id);
    }

    public static void resetForTests() {
        stageBackend = null;
        WindowManager.resetForTests();
        WidgetSessions.resetForTests();
        UiTrees.resetForTests();
        RenderHosts.resetForTests();
    }

    static UiNode loadLayout(String resource) {
        if (resource == null || resource.isEmpty()) {
            throw new IllegalArgumentException("layout resource required");
        }
        String lower = resource.toLowerCase();
        if (lower.endsWith(".lml") || lower.endsWith(".xml")) {
            return LmlUiNodeLoader.loadClasspath(resource);
        }
        if (lower.endsWith(".json") || !resource.contains(".")) {
            return UiNodeLoader.loadClasspath(resource);
        }
        throw new IllegalArgumentException("unsupported layout format: " + resource);
    }
}
