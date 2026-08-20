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
import artframework.presentation.C1Materializer;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRuntime;
import artframework.render.RenderHosts;

/**
 * C1: synthetic window runtime. Loads composition DSL into ECS,
 * syncs {@link RenderHosts}, and attaches Stage via composition or legacy path.
 */
public final class SyntheticRuntime {

    private static StageBackend stageBackend;
    private static RetirementHook retirementHook;

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

    public interface RetirementHook {
        void onRetired(String windowId);
    }

    public static void installRetirementHook(RetirementHook hook) {
        retirementHook = hook;
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

        boolean retired = false;
        try {
            UiNode uiRoot = loadLayout(def.resource);
            if (!UiTypes.WINDOW.equals(uiRoot.type)) {
                throw new IllegalArgumentException("layout root must be window: " + def.resource);
            }
            uiRoot = new TemplateExpander().expand(uiRoot);
            PresentationContext existing = PresentationRuntime.context(def.id);
            if (existing != null) {
                retired = true;
                retireUiOps(def.id);
                PresentationRuntime.clearSignals(existing);
                artframework.presentation.PresentationRegistry.close(PresentationRuntime.c1Scope(def.id));
            }
            PresentationContext context = artframework.presentation.PresentationRegistry.context(
                    PresentationRuntime.c1Scope(def.id));
            C1Materializer.mount(context, uiRoot);
            artframework.core.NodeConnections.syncContext(context);
            artframework.core.AnimationPlayers.syncContext(context);
            artframework.core.NodeStateMachines.syncContext(context);
            SyntheticComponents.mount(def.id);
            LayoutNode legacy = LayoutNodeBridge.toLegacyOrNull(uiRoot);
            if (legacy == null) {
                legacy = LayoutNode.window(
                        uiRoot.id,
                        uiRoot.propString("title", def.id),
                        uiRoot.layout.width,
                        uiRoot.layout.height,
                        java.util.Collections.<LayoutNode>emptyList());
            }

            artframework.render.RenderProjectionQueue.projectNow();
            if (stageBackend != null && stageBackend.isReady()) {
                // Always composition path so EffectTargetActors registers for FX stage sync.
                // Legacy LayoutActors skipped the registry → demo Hello FX stayed at (0,32).
                stageBackend.attachComposition(def.id, uiRoot);
            }
            return legacy;
        } catch (RuntimeException e) {
            cleanupFailedOpen(def.id, !retired);
            throw e;
        }
    }

    public static void onClosed(String id) {
        retireUiOps(id);
        if (stageBackend != null) {
            stageBackend.detach(id);
        }
        closeContext(id);
        SyntheticComponents.onClosed(id);
        artframework.render.RenderProjectionQueue.projectNow();
    }

    /**
     * Re-attach stage for an open window using current session/layout (hot restyle 35.1).
     * No-op if stage not ready or window not open.
     */
    public static void reattach(String id) {
        if (id == null || id.isEmpty() || stageBackend == null || !stageBackend.isReady()) {
            return;
        }
        UiNode declaration = PresentationRuntime.declaration(PresentationRuntime.context(id));
        if (declaration != null) {
            stageBackend.detach(id);
            stageBackend.attachComposition(id, declaration);
            return;
        }
        LayoutNode legacy = WindowManager.get(id);
        if (legacy != null) {
            stageBackend.detach(id);
            stageBackend.attach(id, legacy);
        }
    }

    private static void cleanupFailedOpen(String id, boolean retire) {
        if (retire) retireUiOps(id);
        if (stageBackend != null) {
            try {
                stageBackend.detach(id);
            } catch (RuntimeException ignored) {
            }
        }
        closeContext(id);
        SyntheticComponents.onClosed(id);
        artframework.render.RenderProjectionQueue.projectNow();
    }

    public static void resetForTests() {
        stageBackend = null;
        artframework.presentation.PresentationRegistry.resetForTests();
        SyntheticComponents.resetForTests();
        RenderHosts.resetForTests();
    }

    static UiNode loadLayout(String resource) {
        return loadLayoutResource(resource);
    }

    private static void closeContext(String id) {
        artframework.core.NodeConnections.clearWindow(id);
        artframework.core.AnimationPlayers.clearWindow(id);
        artframework.core.NodeStateMachines.clearWindow(id);
        PresentationContext context = PresentationRuntime.context(id);
        if (context != null) PresentationRuntime.clearSignals(context);
        artframework.presentation.PresentationRegistry.close(PresentationRuntime.c1Scope(id));
    }

    private static void retireUiOps(String id) {
        if (retirementHook != null) retirementHook.onRetired(id);
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
