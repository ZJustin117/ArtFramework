package artframework.core;

import artframework.api.ArtFramework;
import artframework.render.LightwaveEffect;
import artframework.presentation.EffectAttachment;
import artframework.presentation.EffectsComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRuntime;
import artframework.ecs.EntityId;

/**
 * Maps node props to live {@link EffectBinding} params. Ambient {@code fx_*} drives the base
 * lightwave band; {@code fx_pulse_*} drives an overlay band so pulse does not hijack ambient.
 */
public final class PropEffectBridge {

    private PropEffectBridge() {}

    public static void applyProp(PresentationContext context, EntityId target,
            String prop, Object value) {
        if (context == null || target == null || prop == null || prop.isEmpty()) return;
        PresentationRuntime.setProperty(context, target, prop, value);
        if (!(value instanceof Number)) return;
        float f = ((Number) value).floatValue();
        artframework.presentation.NodeIdentityComponent identity =
                PresentationRuntime.identity(context, target);
        if (identity == null || identity.name.isEmpty()) return;
        String win = PresentationRuntime.windowId(context);
        if (prop.startsWith("fx_pulse_")) {
            replaceEffect(context, target, EffectBindingLayer.PULSE,
                    prop.substring("fx_pulse_".length()), f, f > 0.02f);
        } else if ("fx_intensity".equals(prop) || "intensity".equals(prop)) {
            replaceEffect(context, target, EffectBindingLayer.AMBIENT, "intensity", f, true);
            try { artframework.render.LightwaveControls.applyIntensity(win, identity.name, f); }
            catch (Throwable ignored) { }
        } else if ("fx_phase".equals(prop) || "phase".equals(prop)) {
            replaceEffect(context, target, EffectBindingLayer.AMBIENT, "phase", f, true);
            replaceEffect(context, target, EffectBindingLayer.AMBIENT, "freeze", 1f, true);
            PresentationRuntime.setProperty(context, target, "fx_freeze", Float.valueOf(1f));
        } else if ("fx_freeze".equals(prop) || "freeze".equals(prop)) {
            replaceEffect(context, target, EffectBindingLayer.AMBIENT, "freeze", f, true);
        } else if ("fx_width".equals(prop)) {
            replaceEffect(context, target, EffectBindingLayer.AMBIENT, "width", f, true);
        } else if (prop.startsWith("fx_")) {
            String key = prop.substring(3);
            if (!key.isEmpty()) replaceEffect(context, target, EffectBindingLayer.AMBIENT, key, f, true);
        }
    }

    private static void replaceEffect(PresentationContext context, EntityId target,
            String layer, String key, float value, boolean enabled) {
        EffectsComponent effects = PresentationRuntime.component(context, target, EffectsComponent.class);
        if (effects == null) return;
        EffectAttachment attachment = effects.get(LightwaveEffect.ID, layer);
        if (attachment == null) attachment = new EffectAttachment(LightwaveEffect.ID, layer, null);
        effects = effects.withAttachment(attachment.withParam(key, Float.valueOf(value)).withEnabled(enabled));
        context.world().put(target, EffectsComponent.class, effects);
        artframework.render.RenderProjectionQueue.request(PresentationRuntime.windowId(context));
    }

    private static final class EffectBindingLayer {
        static final String AMBIENT = "ambient";
        static final String PULSE = "pulse";
        private EffectBindingLayer() {}
    }

    public static void applyProp(String windowId, String targetId, String prop, Object value) {
        PresentationContext context = PresentationRuntime.context(windowId);
        EntityId target = PresentationRuntime.find(context, targetId);
        if (target == null) target = PresentationRuntime.find(context, "root/" + targetId);
        if (target != null) applyProp(context, target, prop, value);
    }
}
