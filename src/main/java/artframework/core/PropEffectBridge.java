package artframework.core;

import artframework.api.ArtFramework;
import artframework.render.LightwaveEffect;
import artframework.presentation.EffectAttachment;
import artframework.presentation.EffectsComponent;

/**
 * Maps node props to live {@link EffectBinding} params. Ambient {@code fx_*} drives the base
 * lightwave band; {@code fx_pulse_*} drives an overlay band so pulse does not hijack ambient.
 */
public final class PropEffectBridge {

    private PropEffectBridge() {}

    public static void applyProp(artframework.presentation.NodeTree tree,
            artframework.presentation.Node target, String prop, Object value) {
        if (tree == null || target == null || prop == null || prop.isEmpty()) {
            return;
        }
        target.set(prop, value);
        if (!(value instanceof Number)) {
            return;
        }
        float f = ((Number) value).floatValue();
        String win = tree.windowId();
        String nodeId = target.name();
        if (nodeId == null || nodeId.isEmpty()) {
            return;
        }
        if (prop.startsWith("fx_pulse_")) {
            applyPulseProp(target, prop.substring("fx_pulse_".length()), f);
            return;
        }
        if ("fx_intensity".equals(prop) || "intensity".equals(prop)) {
            applyAmbientProp(target, "intensity", f);
            try {
                artframework.render.LightwaveControls.applyIntensity(win, nodeId, f);
            } catch (Throwable ignored) {
            }
        } else if ("fx_phase".equals(prop) || "phase".equals(prop)) {
            applyAmbientProp(target, "phase", f);
            applyAmbientProp(target, "freeze", 1f);
            target.set("fx_freeze", Float.valueOf(1f));
        } else if ("fx_freeze".equals(prop) || "freeze".equals(prop)) {
            applyAmbientProp(target, "freeze", f);
        } else if ("fx_width".equals(prop)) {
            applyAmbientProp(target, "width", f);
        } else if (prop.startsWith("fx_")) {
            String key = prop.substring(3);
            if (!key.isEmpty()) {
                applyAmbientProp(target, key, f);
            }
        }
    }

    private static void applyAmbientProp(artframework.presentation.Node target, String key, float value) {
        replaceEffect(target, EffectBindingLayer.AMBIENT, key, value, true);
    }

    private static void applyPulseProp(artframework.presentation.Node target, String key, float f) {
        if (key == null || key.isEmpty()) {
            return;
        }
        if ("intensity".equals(key)) {
            replaceEffect(target, EffectBindingLayer.PULSE, key, f, f > 0.02f);
            return;
        }
        if ("phase".equals(key)) {
            replaceEffect(target, EffectBindingLayer.PULSE, key, f, true);
            replaceEffect(target, EffectBindingLayer.PULSE, "freeze", 1f, true);
            return;
        }
        replaceEffect(target, EffectBindingLayer.PULSE, key, f, true);
    }

    private static void replaceEffect(
            artframework.presentation.Node target, String layer, String key, float value, boolean enabled) {
        EffectsComponent effects = target.tree().world().get(target.entityId(), EffectsComponent.class);
        if (effects == null) return;
        EffectAttachment attachment = effects.get(LightwaveEffect.ID, layer);
        if (attachment == null) {
            attachment = new EffectAttachment(LightwaveEffect.ID, layer, null);
            effects = effects.withAttachment(attachment);
        }
        effects = effects.withAttachment(attachment.withParam(key, Float.valueOf(value)).withEnabled(enabled));
        target.tree().world().put(target.entityId(), EffectsComponent.class, effects);
        artframework.render.RenderHosts.get().syncC1Window(target.tree().windowId());
    }

    private static final class EffectBindingLayer {
        static final String AMBIENT = "ambient";
        static final String PULSE = "pulse";
        private EffectBindingLayer() {}
    }

    public static void applyProp(String windowId, String targetId, String prop, Object value) {
        artframework.presentation.NodeTree tree = ArtFramework.tree(windowId);
        if (tree == null) {
            return;
        }
        artframework.presentation.Node inst = tree.find(targetId);
        if (inst == null) {
            inst = tree.find("root/" + targetId);
        }
        if (inst == null) {
            return;
        }
        applyProp(tree, inst, prop, value);
    }
}
