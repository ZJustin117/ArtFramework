package artframework.core;

import artframework.assets.HostAssetsHolder;
import artframework.c1.SyntheticRuntime;
import artframework.c1.WindowManager;
import artframework.component.WidgetSessions;

import java.util.ArrayList;
import java.util.List;

/**
 * Hot restyle after project present / open-tree present refresh (milestone 35.1).
 *
 * <p>Re-applies cascade themes on open {@link UiTree}s; re-attaches Stage for trees that still
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
        for (UiTree tree : UiTrees.listOpen()) {
            if (tree != null) {
                tree.refreshPresent();
            }
        }
    }

    public static void refreshOpenTreesAndReattachProjectFallback() {
        List<String> reattach = new ArrayList<String>();
        for (UiTree tree : UiTrees.listOpen()) {
            if (tree == null) {
                continue;
            }
            tree.refreshPresent();
            PresentResolved r = tree.resolvePresent();
            if (r != null && r.fromProject) {
                reattach.add(tree.windowId());
            }
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
        if (!WindowManager.contains(windowId) && WidgetSessions.get(windowId) == null) {
            return;
        }
        try {
            SyntheticRuntime.reattach(windowId);
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
            artframework.c1.host.StageHost host = artframework.c1.host.StageHost.get();
            if (host != null) {
                host.refreshDefaultSkin();
            }
        } catch (Throwable ignored) {
        }
    }
}
