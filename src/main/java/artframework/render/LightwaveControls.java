package artframework.render;

import artframework.api.ArtFramework;
import artframework.component.WidgetSessions;
import artframework.core.AnimationPlayer;
import artframework.core.AnimationPlayers;
import artframework.core.UiInstance;
import artframework.core.UiTree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wire C1 slider / animation to {@link LightwaveEffect}. Pulse = one diagonal sweep (phase 0→1)
 * with a bright intensity spike — not a speed change and not a frozen corner stick.
 */
public final class LightwaveControls {

    private static final Map<String, Runnable> PENDING_CLOSE = new LinkedHashMap<String, Runnable>();
    private static final Map<String, PulseState> PULSES = new LinkedHashMap<String, PulseState>();

    private static final class PulseState {
        final float baseline;
        final float duration;
        float elapsed;
        final boolean closeAfter;

        PulseState(float baseline, float duration, boolean closeAfter) {
            this.baseline = baseline;
            this.duration = duration > 0.05f ? duration : 0.4f;
            this.elapsed = 0f;
            this.closeAfter = closeAfter;
        }
    }

    private LightwaveControls() {}

    public static boolean applyIntensity(String windowId, String sliderId, float sliderValue) {
        if (windowId == null) {
            return false;
        }
        // Do not fight an active pulse with slider/anim baseline writes mid-sweep.
        if (PULSES.containsKey(windowId)) {
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

    /**
     * One-shot: freeze time-scroll, drive phase 0→1 once while intensity spikes then returns to
     * the wave slider baseline. Speed param is left alone.
     */
    public static void pulse(String windowId) {
        if (windowId == null) {
            return;
        }
        float baseline = baselineIntensity(windowId);
        PULSES.put(windowId, new PulseState(baseline, 0.45f, false));
        applyPulseFrame(windowId, PULSES.get(windowId));
    }

    public static void closeWithFx(String windowId, Runnable onClose) {
        if (windowId == null) {
            if (onClose != null) {
                onClose.run();
            }
            return;
        }
        float baseline = baselineIntensity(windowId);
        PULSES.put(windowId, new PulseState(baseline, 0.35f, true));
        if (onClose != null) {
            PENDING_CLOSE.put(windowId, onClose);
        }
        applyPulseFrame(windowId, PULSES.get(windowId));
    }

    /**
     * Advance active pulses by {@code dt} seconds. Call from host after {@code ArtFramework.tick}.
     */
    public static void tickPulses(float dt) {
        if (PULSES.isEmpty() || dt <= 0f) {
            return;
        }
        List<String> done = new ArrayList<String>();
        for (Map.Entry<String, PulseState> e : PULSES.entrySet()) {
            String win = e.getKey();
            PulseState st = e.getValue();
            st.elapsed += dt;
            if (st.elapsed >= st.duration) {
                st.elapsed = st.duration;
                applyPulseFrame(win, st);
                done.add(win);
            } else {
                applyPulseFrame(win, st);
            }
        }
        for (String win : done) {
            finishPulse(win);
        }
    }

    /** @deprecated use {@link #tickPulses(float)} */
    public static void flushPulses() {
        // No-op without dt; StageHost must call tickPulses.
    }

    public static void flushPendingCloses() {
        // Completions handled in finishPulse when closeAfter.
    }

    public static void resetForTests() {
        PENDING_CLOSE.clear();
        PULSES.clear();
    }

    private static void applyPulseFrame(String windowId, PulseState st) {
        float t = st.elapsed / st.duration;
        if (t < 0f) {
            t = 0f;
        }
        if (t > 1f) {
            t = 1f;
        }
        // Phase sweeps once across the panel (0 → 1).
        float phase = t;
        // Intensity: hot in the first half, ease to baseline (visible flash without sticky corner).
        float peak = 2f;
        float envelope = (float) Math.sin(Math.PI * Math.min(1.0, t * 1.05));
        if (envelope < 0f) {
            envelope = 0f;
        }
        float intensity = st.baseline + (peak - st.baseline) * envelope;
        if (t > 0.85f) {
            // Blend to baseline at the end
            float u = (t - 0.85f) / 0.15f;
            intensity = intensity * (1f - u) + st.baseline * u;
        }
        try {
            String tid = "c1:" + windowId + ":panel";
            ArtFramework.render().setEffectParam(tid, LightwaveEffect.ID, "freeze", 1f);
            ArtFramework.render().setEffectParam(tid, LightwaveEffect.ID, "phase", phase);
            ArtFramework.render().setEffectParam(tid, LightwaveEffect.ID, "intensity", intensity);
            // Slightly wider band during pulse for readability
            ArtFramework.render()
                    .setEffectParam(tid, LightwaveEffect.ID, "width", 0.32f + 0.1f * envelope);
        } catch (Throwable ignored) {
        }
        mirrorTreeProp(windowId, intensity);
    }

    private static void finishPulse(String windowId) {
        PulseState st = PULSES.remove(windowId);
        float baseline = st != null ? st.baseline : baselineIntensity(windowId);
        setPanelIntensity(windowId, baseline);
        mirrorTreeProp(windowId, baseline);
        try {
            String tid = "c1:" + windowId + ":panel";
            ArtFramework.render().setEffectParam(tid, LightwaveEffect.ID, "freeze", 0f);
            ArtFramework.render().setEffectParam(tid, LightwaveEffect.ID, "width", 0.28f);
        } catch (Throwable ignored) {
        }
        if (st != null && st.closeAfter) {
            Runnable r = PENDING_CLOSE.remove(windowId);
            if (r != null) {
                try {
                    r.run();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static float baselineIntensity(String windowId) {
        try {
            if (WidgetSessions.get(windowId) != null
                    && WidgetSessions.get(windowId).hasSlider("wave")) {
                return clampIntensity(WidgetSessions.get(windowId).getSlider("wave"));
            }
        } catch (Throwable ignored) {
        }
        try {
            UiTree tree = ArtFramework.tree(windowId);
            if (tree != null && tree.get("panel") != null) {
                Object v = tree.get("panel").prop("fx_intensity");
                if (v instanceof Number) {
                    return clampIntensity(((Number) v).floatValue());
                }
            }
        } catch (Throwable ignored) {
        }
        return 0.55f;
    }

    private static boolean setPanelIntensity(String windowId, float intensity) {
        String[] candidates =
                new String[] {
                    "c1:" + windowId + ":panel",
                    "c1:" + windowId + ":p",
                };
        for (int i = 0; i < candidates.length; i++) {
            if (ArtFramework.render()
                    .setEffectParam(candidates[i], LightwaveEffect.ID, "intensity", intensity)) {
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
