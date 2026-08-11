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
import artframework.presentation.NodeTrees;
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

        try {
            WidgetSession session = WidgetSessions.openTree(def.id, uiRoot);
            NodeTrees.open(def.id, session.root(), null);
            SyntheticComponents.mount(def.id);
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
            RenderHosts.get().syncWidgetSession(session);
            artframework.presentation.NodeTree presentationTree = NodeTrees.get(def.id);
            if (presentationTree != null) {
                RenderHosts.get().syncFrame(
                        presentationTree.frame(), artframework.render.RenderTargetKind.SYNTHETIC_WIDGET);
            }
            if (stageBackend != null && stageBackend.isReady()) {
                // Always composition path so EffectTargetActors registers for FX stage sync.
                // Legacy LayoutActors skipped the registry → demo Hello FX stayed at (0,32).
                stageBackend.attachComposition(def.id, session.root());
            }
            return legacy;
        } catch (RuntimeException e) {
            cleanupFailedOpen(def.id);
            throw e;
        }
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
        NodeTrees.close(id);
        SyntheticComponents.unmount(id);
        RenderHosts.get().detachWidgetSession(id);
    }

    /**
     * Re-attach stage for an open window using current session/layout (hot restyle 35.1).
     * No-op if stage not ready or window not open.
     */
    public static void reattach(String id) {
        if (id == null || id.isEmpty() || stageBackend == null || !stageBackend.isReady()) {
            return;
        }
        WidgetSession session = WidgetSessions.get(id);
        if (session != null && session.root() != null) {
            stageBackend.detach(id);
            stageBackend.attachComposition(id, session.root());
            return;
        }
        LayoutNode legacy = WindowManager.get(id);
        if (legacy != null) {
            stageBackend.detach(id);
            stageBackend.attach(id, legacy);
        }
    }

    private static void cleanupFailedOpen(String id) {
        if (stageBackend != null) {
            try {
                stageBackend.detach(id);
            } catch (RuntimeException ignored) {
            }
        }
        WindowManager.remove(id);
        WidgetSessions.close(id);
        NodeTrees.close(id);
        SyntheticComponents.unmount(id);
        RenderHosts.get().detachWidgetSession(id);
    }

    public static void resetForTests() {
        stageBackend = null;
        WindowManager.resetForTests();
        WidgetSessions.resetForTests();
        NodeTrees.resetForTests();
        SyntheticComponents.resetForTests();
        RenderHosts.resetForTests();
    }

    static UiNode loadLayout(String resource) {
        return loadLayoutResource(resource);
    }

    /** Public load for PresentPack templates/windows (JSON or LML). */
    public static UiNode loadLayoutResource(String resource) {
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
