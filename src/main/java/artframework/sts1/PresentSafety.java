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
    private static String c1HostRecreation = "not attempted";

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
        artframework.sts1.backend.Sts1RelicPotionBlightProjection.clear();
        artframework.sts1.backend.Sts1OrbStanceProjection.clear();
        artframework.sts1.backend.Sts1PileSoulProjection.clear();
        artframework.sts1.backend.Sts1RoomShellProjection.clear();
        artframework.sts1.render.NativeRenderBridge.clearTransientEffectsForRecovery();
        artframework.sts1.skeleton.Sts1SkeletonBridge.stopAll();
        unmountAllPresentSurfaces();
        removeC2SurfaceItemsForRecovery();
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
        artframework.sts1.backend.Sts1RelicPotionBlightProjection.clear();
        artframework.sts1.backend.Sts1OrbStanceProjection.clear();
        artframework.sts1.backend.Sts1PileSoulProjection.clear();
        artframework.sts1.backend.Sts1RoomShellProjection.clear();
        artframework.sts1.render.NativeRenderBridge.clearTransientEffectsForRecovery();
        artframework.sts1.render.NativeRenderBridge.resetForTests();
        removeC2SurfaceItemsForRecovery();
        artframework.sts1.render.MapDrawPath.resetForTests();
        artframework.sts1.audio.ArtAudioBridge.resetForTests();
        artframework.sts1.skeleton.Sts1SkeletonBridge.onHostRecreated();
        recreateC1HostIfAvailable();
        artframework.render.RenderHosts.get().recreateHostCache();
        artframework.sts1.assets.Sts1AssetMaterializer.clearCache();
        artframework.render.RenderProjectionQueue.projectNow();
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
        m.put("c1HostRecreation", c1HostRecreation);
        return m;
    }

    public static void resetForTests() {
        panic = false;
        panicReason = "";
        recreationCount = 0;
        c1HostRecreation = "not attempted";
    }

    /** C1 is an optional BaseMod host; keep pure runtime recreation independent of its classes. */
    private static void recreateC1HostIfAvailable() {
        try {
            Class<?> bindings = Class.forName("artframework.c1.host.EffectTargetActors");
            Class<?> type = Class.forName("artframework.c1.host.StageHost");
            Object host = type.getMethod("get").invoke(null);
            if (host == null) {
                c1HostRecreation = "not installed";
                return;
            }
            bindings.getMethod("clearAll").invoke(null);
            Object rebuilt = type.getMethod("recreateHost").invoke(host);
            c1HostRecreation = Boolean.TRUE.equals(rebuilt) ? "rebuilt" : "failed: StageHost";
        } catch (ClassNotFoundException e) {
            c1HostRecreation = "unavailable";
        } catch (LinkageError e) {
            // Pure ECS tests and non-BaseMod hosts legitimately cannot link StageHost.
            c1HostRecreation = "unavailable";
        } catch (Throwable e) {
            c1HostRecreation = "failed: " + e.getClass().getSimpleName();
        }
    }

    private static void removeC2SurfaceItemsForRecovery() {
        artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.COMBAT_HAND);
        artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.COMBAT_CARD_SLOTS);
        artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.MAP);
        artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.COMBAT_CONTROLS);
        artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.COMBAT_ENERGY);
        artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.COMBAT_INTENTS);
        artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.COMBAT_PROCEED);
        artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.TOP_PANEL);
        artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.EVENT);
        artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.SELECT_GRID);
        artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.SELECT_HAND);
        artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.REWARD_COMBAT);
        artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.REWARD_CARD);
        artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.REWARD_BOSS_RELIC);
        artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.REST);
        artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.SHOP);
        artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.TREASURE);
    }

    private static void unmountAllPresentSurfaces() {
        String[] ids =
                new String[] {
                    SurfaceIds.COMBAT_HAND,
                    SurfaceIds.COMBAT_CARD_SLOTS,
                    SurfaceIds.COMBAT_CONTROLS,
                    SurfaceIds.COMBAT_SURFACE,
                    SurfaceIds.COMBAT_PROCEED,
                    SurfaceIds.COMBAT_ENERGY,
                     SurfaceIds.COMBAT_INTENTS,
                     SurfaceIds.COMBAT_TARGETING,
                     SurfaceIds.MAP,
                    SurfaceIds.SKELETON,
                    SurfaceIds.EVENT,
                    SurfaceIds.SELECT_GRID,
                    SurfaceIds.SELECT_HAND,
                    SurfaceIds.REWARD_COMBAT,
                    SurfaceIds.REWARD_CARD,
                    SurfaceIds.REWARD_BOSS_RELIC,
                    SurfaceIds.REST,
                    SurfaceIds.TREASURE,
                    SurfaceIds.SHOP,
                    SurfaceIds.TOP_PANEL
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
