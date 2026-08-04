package artframework.core;

import artframework.api.ArtFramework;
import artframework.render.LightwaveEffect;
import artframework.render.RenderHosts;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One-shot effect envelope (phase 0→1 + intensity spike). Driven by host tick, not frame signals.
 * Replaces imperative {@link artframework.render.LightwaveControls#pulse} for signal actions.
 */
public final class EffectPulse {

    private static final Map<String, PulseState> PULSES = new LinkedHashMap<String, PulseState>();

    private static final class PulseState {
        final String windowId;
        final String targetId;
        final String effectId;
        final float baseline;
        final float duration;
        final boolean closeAfter;
        final Runnable onDone;
        float elapsed;

        PulseState(
                String windowId,
                String targetId,
                String effectId,
                float baseline,
                float duration,
                boolean closeAfter,
                Runnable onDone) {
            this.windowId = windowId;
            this.targetId = targetId;
            this.effectId = effectId != null && !effectId.isEmpty() ? effectId : LightwaveEffect.ID;
            this.baseline = baseline;
            this.duration = duration > 0.05f ? duration : 0.4f;
            this.closeAfter = closeAfter;
            this.onDone = onDone;
            this.elapsed = 0f;
        }

        String key() {
            return windowId + "\0" + targetId + "\0" + effectId;
        }
    }

    private EffectPulse() {}

    public static boolean isActive(String windowId, String targetNodeId) {
        if (windowId == null) {
            return false;
        }
        String prefix = windowId + "\0";
        for (String k : PULSES.keySet()) {
            if (k.startsWith(prefix)) {
                if (targetNodeId == null || targetNodeId.isEmpty()) {
                    return true;
                }
                if (k.contains("\0" + targetNodeId + "\0") || k.contains("\0c1:" + windowId + ":" + targetNodeId + "\0")) {
                    return true;
                }
                // also match bare panel target ids used as c1:win:panel
                if (k.indexOf(targetNodeId) >= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void pulse(String windowId, String targetNodeId, String effectId, float duration) {
        if (windowId == null || targetNodeId == null) {
            return;
        }
        float baseline = baselineIntensity(windowId, targetNodeId);
        String tid = renderTargetId(windowId, targetNodeId);
        PulseState st =
                new PulseState(windowId, tid, effectId, baseline, duration, false, null);
        PULSES.put(st.key(), st);
        applyFrame(st);
    }

    public static void closeWithFx(String windowId, String targetNodeId, Runnable onClose) {
        if (windowId == null) {
            if (onClose != null) {
                onClose.run();
            }
            return;
        }
        String node = targetNodeId != null && !targetNodeId.isEmpty() ? targetNodeId : "panel";
        float baseline = baselineIntensity(windowId, node);
        String tid = renderTargetId(windowId, node);
        PulseState st =
                new PulseState(windowId, tid, LightwaveEffect.ID, baseline, 0.35f, true, onClose);
        PULSES.put(st.key(), st);
        applyFrame(st);
    }

    /** Advance active pulses. Call from host after {@code ArtFramework.tick}. */
    public static void tick(float dt) {
        if (PULSES.isEmpty() || dt <= 0f) {
            return;
        }
        List<String> done = new ArrayList<String>();
        for (Map.Entry<String, PulseState> e : PULSES.entrySet()) {
            PulseState st = e.getValue();
            st.elapsed += dt;
            if (st.elapsed >= st.duration) {
                st.elapsed = st.duration;
                applyFrame(st);
                done.add(e.getKey());
            } else {
                applyFrame(st);
            }
        }
        for (String k : done) {
            finish(k);
        }
    }

    public static void resetForTests() {
        PULSES.clear();
    }

    private static void applyFrame(PulseState st) {
        float t = st.elapsed / st.duration;
        if (t < 0f) {
            t = 0f;
        }
        if (t > 1f) {
            t = 1f;
        }
        float phase = t;
        float peak = 2f;
        float envelope = (float) Math.sin(Math.PI * Math.min(1.0, t * 1.05));
        if (envelope < 0f) {
            envelope = 0f;
        }
        float intensity = st.baseline + (peak - st.baseline) * envelope;
        if (t > 0.85f) {
            float u = (t - 0.85f) / 0.15f;
            intensity = intensity * (1f - u) + st.baseline * u;
        }
        try {
            RenderHosts.get().setEffectParam(st.targetId, st.effectId, "freeze", 1f);
            RenderHosts.get().setEffectParam(st.targetId, st.effectId, "phase", phase);
            RenderHosts.get().setEffectParam(st.targetId, st.effectId, "intensity", intensity);
            RenderHosts.get()
                    .setEffectParam(st.targetId, st.effectId, "width", 0.32f + 0.1f * envelope);
        } catch (Throwable ignored) {
        }
        mirrorTree(st.windowId, st.targetId, intensity);
    }

    private static void finish(String key) {
        PulseState st = PULSES.remove(key);
        if (st == null) {
            return;
        }
        try {
            RenderHosts.get().setEffectParam(st.targetId, st.effectId, "freeze", 0f);
            RenderHosts.get().setEffectParam(st.targetId, st.effectId, "width", 0.28f);
            RenderHosts.get()
                    .setEffectParam(st.targetId, st.effectId, "intensity", st.baseline);
        } catch (Throwable ignored) {
        }
        mirrorTree(st.windowId, st.targetId, st.baseline);
        if (st.closeAfter && st.onDone != null) {
            try {
                st.onDone.run();
            } catch (Throwable ignored) {
            }
        }
    }

    private static void mirrorTree(String windowId, String targetId, float intensity) {
        try {
            UiTree tree = ArtFramework.tree(windowId);
            if (tree == null) {
                return;
            }
            String nodeId = nodeIdFromTarget(windowId, targetId);
            UiInstance panel = tree.get(nodeId);
            if (panel == null) {
                panel = tree.get("panel");
            }
            if (panel == null) {
                panel = tree.get("p");
            }
            if (panel != null) {
                panel.setProp("fx_intensity", Float.valueOf(intensity));
            }
        } catch (Throwable ignored) {
        }
    }

    private static String nodeIdFromTarget(String windowId, String targetId) {
        String prefix = "c1:" + windowId + ":";
        if (targetId != null && targetId.startsWith(prefix)) {
            return targetId.substring(prefix.length());
        }
        return targetId != null ? targetId : "panel";
    }

    private static String renderTargetId(String windowId, String targetNodeId) {
        if (targetNodeId != null && targetNodeId.startsWith("c1:")) {
            return targetNodeId;
        }
        return "c1:" + windowId + ":" + (targetNodeId != null ? targetNodeId : "panel");
    }

    private static float baselineIntensity(String windowId, String targetNodeId) {
        try {
            artframework.component.WidgetSession session =
                    artframework.component.WidgetSessions.get(windowId);
            if (session != null) {
                if (session.hasSlider("wave_slider")) {
                    return clamp(session.getSlider("wave_slider"));
                }
                if (session.hasSlider("wave")) {
                    return clamp(session.getSlider("wave"));
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            UiTree tree = ArtFramework.tree(windowId);
            if (tree != null) {
                UiInstance n = tree.get(targetNodeId);
                if (n == null) {
                    n = tree.get("panel");
                }
                if (n != null && n.prop("fx_intensity") instanceof Number) {
                    return clamp(((Number) n.prop("fx_intensity")).floatValue());
                }
            }
        } catch (Throwable ignored) {
        }
        return 0.55f;
    }

    private static float clamp(float v) {
        if (v < 0f) {
            return 0f;
        }
        if (v > 2f) {
            return 2f;
        }
        return v;
    }
}
