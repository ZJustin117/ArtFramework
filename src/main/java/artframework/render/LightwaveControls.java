package artframework.render;

import artframework.api.ArtFramework;
import artframework.component.WidgetSessions;
import artframework.core.EffectPulse;
import artframework.core.UiInstance;
import artframework.core.UiTree;

/**
 * Wire C1 slider to {@link LightwaveEffect}. Pulse/close FX delegate to {@link EffectPulse}
 * (signal actions use the same path).
 */
public final class LightwaveControls {

    private LightwaveControls() {}

    public static boolean applyIntensity(String windowId, String sliderId, float sliderValue) {
        if (windowId == null) {
            return false;
        }
        if (EffectPulse.isActive(windowId, null)) {
            return false;
        }
        float intensity = clampIntensity(sliderValue);
        boolean any = setPanelIntensity(windowId, intensity);
        if (!any && sliderId != null && !sliderId.isEmpty()) {
            any =
                    ArtFramework.render()
                            .setEffectParam(
                                    "c1:" + windowId + ":" + sliderId,
                                    LightwaveEffect.ID,
                                    "intensity",
                                    intensity);
        }
        if (any) {
            mirrorTreeProp(windowId, intensity);
        }
        return any;
    }

    /** @see EffectPulse#pulse */
    public static void pulse(String windowId) {
        EffectPulse.pulse(windowId, "panel", LightwaveEffect.ID, 0.45f);
    }

    /** @see EffectPulse#closeWithFx */
    public static void closeWithFx(String windowId, Runnable onClose) {
        EffectPulse.closeWithFx(windowId, "panel", onClose);
    }

    /**
     * Advance active pulses. Prefer {@link artframework.api.ArtFramework#tick} in production
     * (StageHost already ticks the facade). Safe to call from unit tests that do not go through
     * the facade.
     */
    public static void tickPulses(float dt) {
        artframework.core.EffectPulse.tick(dt);
    }

    /** @deprecated use {@link #tickPulses(float)} */
    public static void flushPulses() {}

    public static void flushPendingCloses() {}

    public static void resetForTests() {
        EffectPulse.resetForTests();
    }

    private static boolean setPanelIntensity(String windowId, float intensity) {
        String[] candidates =
                new String[] {
                    "c1:" + windowId + ":panel",
                    "c1:" + windowId + ":p",
                };
        for (int i = 0; i < candidates.length; i++) {
            // Ambient band only — pulse overlay is a separate layer.
            if (ArtFramework.render()
                    .setEffectParam(
                            candidates[i],
                            LightwaveEffect.ID,
                            artframework.render.EffectBinding.LAYER_AMBIENT,
                            "intensity",
                            intensity)) {
                return true;
            }
        }
        return false;
    }

    private static void mirrorTreeProp(String windowId, float intensity) {
        try {
            UiTree tree = ArtFramework.tree(windowId);
            if (tree != null) {
                UiInstance panel = tree.get("panel");
                if (panel == null) {
                    panel = tree.get("p");
                }
                if (panel != null) {
                    panel.setProp("fx_intensity", Float.valueOf(intensity));
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static float clampIntensity(float v) {
        if (v < 0f) {
            return 0f;
        }
        if (v > 2f) {
            return 2f;
        }
        return v;
    }
}
