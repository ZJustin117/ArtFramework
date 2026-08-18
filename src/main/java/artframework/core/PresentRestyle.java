package artframework.core;

import artframework.assets.HostAssetsHolder;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRegistry;
import artframework.presentation.PresentationRuntime;
import artframework.ecs.EntityId;

import java.util.ArrayList;
import java.util.List;

/**
 * Hot restyle after project present / open-tree present refresh (milestone 35.1).
 *
     * <p>Re-applies cascade themes on open ECS contexts; re-attaches Stage for contexts that still
 * resolve from project fallback (node override windows keep their skin until remount).
 */
public final class PresentRestyle {

    private PresentRestyle() {}

    /** After {@link ProjectPresent#set}: HostAssets prefer + C1 restyle (UI pack activated by set). */
    public static void onProjectPresentChanged() {
        applyPresentPack(ProjectPresent.resolved());
        refreshOpenTreesAndReattachProjectFallback();
        refreshDefaultSkinQuiet();
    }

    /** Refresh all open trees from cascade (no Stage reattach). */
    public static void refreshOpenTrees() {
        for (String windowId : PresentationRuntime.openWindowIds()) {
            artframework.render.RenderProjectionQueue.projectNow();
        }
    }

    public static void refreshOpenTreesAndReattachProjectFallback() {
        List<String> reattach = new ArrayList<String>();
        for (String windowId : PresentationRuntime.openWindowIds()) {
            PresentationContext context = PresentationRuntime.context(windowId);
            EntityId root = PresentationRuntime.root(context);
            PresentResolved r = PresentResolve.forEntity(context, root);
            if (r != null && r.fromProject) reattach.add(windowId);
        }
        for (String id : reattach) {
            reattachStage(id);
        }
    }

    /** Re-apply Stage skin for one open window (uses current resolve). */
    public static void reattachStage(String windowId) {
        if (windowId == null || windowId.isEmpty()) {
            return;
        }
        if (!PresentationRuntime.isOpen(windowId)) {
            return;
        }
        try {
            PresentRestyleHost.reattach(windowId);
        } catch (Throwable ignored) {
        }
    }

    public static void applyPresentPack(PresentResolved resolved) {
        if (resolved == null) {
            return;
        }
        try {
            HostAssetsHolder.get().preferPresentPack(resolved.packId);
        } catch (Throwable ignored) {
        }
    }

    private static void refreshDefaultSkinQuiet() {
        try {
            PresentRestyleHost.refreshDefaultSkin();
        } catch (Throwable ignored) {
        }
    }
}
