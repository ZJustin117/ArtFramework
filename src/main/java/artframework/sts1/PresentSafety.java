package artframework.sts1;

import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.render.Sts1RenderPipeline;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Panic fallback and host recreation cleanup (16.9). Panic forces all present levels OFF and
 * clears suppress so native STS UI remains usable.
 */
public final class PresentSafety {

    private static boolean panic;
    private static String panicReason = "";
    private static int recreationCount;

    private PresentSafety() {}

    public static boolean isPanic() {
        return panic;
    }

    public static String panicReason() {
        return panicReason;
    }

    /** Disable full-present everywhere; keep projection backend bound for observe after clear. */
    public static void panic(String reason) {
        panic = true;
        panicReason = reason != null ? reason : "panic";
        FullPresentMode.resetForTests();
        // resetForTests clears levels; re-mark panic after
        panic = true;
        panicReason = reason != null ? reason : "panic";
        CombatInputRouter.setSuppressNativeInput(false);
        Sts1RenderPipeline.setOverlayObserve(false);
        unmountAllPresentSurfaces();
    }

    public static void clearPanic() {
        panic = false;
        panicReason = "";
    }

    /**
     * Android / GL context recreation: drop transient host state, keep policy levels unless panic.
     */
    public static void onHostRecreated() {
        recreationCount++;
        Sts1RenderPipeline.resetForTests();
        artframework.sts1.render.MapDrawPath.resetForTests();
        artframework.sts1.audio.ArtAudioBridge.resetForTests();
        artframework.sts1.skeleton.Sts1SkeletonBridge.resetForTests();
        if (panic) {
            // stay safe
            CombatInputRouter.setSuppressNativeInput(false);
        }
    }

    public static int recreationCount() {
        return recreationCount;
    }

    public static Map<String, Object> probeSlice() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("panic", Boolean.valueOf(panic));
        m.put("panicReason", panicReason);
        m.put("recreationCount", Integer.valueOf(recreationCount));
        return m;
    }

    public static void resetForTests() {
        panic = false;
        panicReason = "";
        recreationCount = 0;
    }

    private static void unmountAllPresentSurfaces() {
        String[] ids =
                new String[] {
                    SurfaceIds.COMBAT_HAND,
                    SurfaceIds.COMBAT_CARD_SLOTS,
                    SurfaceIds.COMBAT_CONTROLS,
                    SurfaceIds.COMBAT_SURFACE,
                    SurfaceIds.MAP,
                    SurfaceIds.SKELETON
                };
        for (String id : ids) {
            try {
                artframework.core.UiComponent c = ArtFramework.component(id);
                if (c != null && c.isMounted()) {
                    c.unmount();
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
