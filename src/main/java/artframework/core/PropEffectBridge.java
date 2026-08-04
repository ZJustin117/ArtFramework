package artframework.core;

import artframework.api.ArtFramework;
import artframework.render.EffectBinding;
import artframework.render.LightwaveEffect;
import artframework.render.RenderHosts;

/**
 * Maps node props to live {@link EffectBinding} params. Ambient {@code fx_*} drives the base
 * lightwave band; {@code fx_pulse_*} drives an overlay band so pulse does not hijack ambient.
 */
public final class PropEffectBridge {

    private PropEffectBridge() {}

    public static void applyProp(UiTree tree, UiInstance target, String prop, Object value) {
        if (tree == null || target == null || prop == null || prop.isEmpty()) {
            return;
        }
        target.setProp(prop, value);
        if (!(value instanceof Number)) {
            return;
        }
        float f = ((Number) value).floatValue();
        String win = tree.windowId();
        String nodeId = target.id();
        if (nodeId == null || nodeId.isEmpty()) {
            return;
        }
        String tid = "c1:" + win + ":" + nodeId;

        if (prop.startsWith("fx_pulse_")) {
            applyPulseProp(tid, prop.substring("fx_pulse_".length()), f);
            return;
        }
        if ("fx_intensity".equals(prop) || "intensity".equals(prop)) {
            RenderHosts.get()
                    .setEffectParam(
                            tid,
                            LightwaveEffect.ID,
                            EffectBinding.LAYER_AMBIENT,
                            "intensity",
                            f);
            try {
                artframework.render.LightwaveControls.applyIntensity(win, nodeId, f);
            } catch (Throwable ignored) {
            }
        } else if ("fx_phase".equals(prop) || "phase".equals(prop)) {
            RenderHosts.get()
                    .setEffectParam(
                            tid, LightwaveEffect.ID, EffectBinding.LAYER_AMBIENT, "phase", f);
            RenderHosts.get()
                    .setEffectParam(
                            tid, LightwaveEffect.ID, EffectBinding.LAYER_AMBIENT, "freeze", 1f);
            target.setProp("fx_freeze", Float.valueOf(1f));
        } else if ("fx_freeze".equals(prop) || "freeze".equals(prop)) {
            RenderHosts.get()
                    .setEffectParam(
                            tid, LightwaveEffect.ID, EffectBinding.LAYER_AMBIENT, "freeze", f);
        } else if ("fx_width".equals(prop)) {
            RenderHosts.get()
                    .setEffectParam(
                            tid, LightwaveEffect.ID, EffectBinding.LAYER_AMBIENT, "width", f);
        } else if (prop.startsWith("fx_")) {
            String key = prop.substring(3);
            if (!key.isEmpty()) {
                RenderHosts.get()
                        .setEffectParam(
                                tid, LightwaveEffect.ID, EffectBinding.LAYER_AMBIENT, key, f);
            }
        }
    }

    private static void applyPulseProp(String tid, String key, float f) {
        if (key == null || key.isEmpty()) {
            return;
        }
        EffectBinding pulse =
                RenderHosts.get().ensurePulseLightwave(tid);
        if (pulse == null) {
            return;
        }
        // Any non-zero intensity enables the overlay band; zero disables after settle.
        if ("intensity".equals(key)) {
            pulse.setEnabled(f > 0.02f);
            pulse.setParamFloat("intensity", f);
            return;
        }
        if ("phase".equals(key)) {
            pulse.setEnabled(true);
            pulse.setParamFloat("phase", f);
            pulse.setParamFloat("freeze", 1f);
            return;
        }
        if ("freeze".equals(key) || "width".equals(key) || "angle".equals(key)) {
            pulse.setParamFloat(key, f);
            return;
        }
        pulse.setParamFloat(key, f);
    }

    public static void applyProp(String windowId, String targetId, String prop, Object value) {
        UiTree tree = ArtFramework.tree(windowId);
        if (tree == null) {
            return;
        }
        UiInstance inst = tree.get(targetId);
        if (inst == null) {
            inst = tree.find(targetId);
        }
        if (inst == null) {
            return;
        }
        applyProp(tree, inst, prop, value);
    }
}
