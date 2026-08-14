package artframework.core;

import artframework.api.ArtFramework;
import artframework.ecs.ArtEcs;
import artframework.ecs.EntityId;
import artframework.render.LightwaveEffect;
import artframework.presentation.EffectPulseComponent;
import artframework.presentation.HostBindingComponent;
import artframework.presentation.NodePropertiesComponent;
import artframework.presentation.ControlValueComponent;
import artframework.presentation.EffectsComponent;
import artframework.presentation.NodeIdentityComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One-shot effect envelope (phase 0→1 + intensity spike). Driven by host tick, not frame signals.
 * Replaces imperative {@link artframework.render.LightwaveControls#pulse} for signal actions.
 */
public final class EffectPulse {

    // Completion callbacks are host notifications, never presentation authority or component data.
    private static final Map<EntityId, Runnable> CLOSE_CALLBACKS =
            new LinkedHashMap<EntityId, Runnable>();

    private EffectPulse() {}

    public static boolean isActive(String windowId, String targetNodeId) {
        if (windowId == null) {
            return false;
        }
        for (EntityId entity : ArtEcs.world().query(EffectPulseComponent.class)) {
            EffectPulseComponent pulse = ArtEcs.world().get(entity, EffectPulseComponent.class);
            if (!windowId.equals(pulse.windowId)) continue;
            if (targetNodeId == null || targetNodeId.isEmpty()
                    || pulse.targetId.equals(targetNodeId)
                    || pulse.targetId.endsWith(":" + targetNodeId)) {
                return true;
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
        start(new EffectPulseComponent(windowId, tid,
                effectId != null && !effectId.isEmpty() ? effectId : LightwaveEffect.ID,
                baseline, duration, 0f, false));
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
        EntityId entity = start(new EffectPulseComponent(windowId, tid, LightwaveEffect.ID,
                baseline, 0.35f, 0f, true));
        if (onClose != null) CLOSE_CALLBACKS.put(entity, onClose);
    }

    /** Advance active pulses. Call from host after {@code ArtFramework.tick}. */
    public static void tick(float dt) {
        if (dt <= 0f) {
            return;
        }
        List<EntityId> done = new ArrayList<EntityId>();
        for (EntityId entity : ArtEcs.world().query(EffectPulseComponent.class)) {
            EffectPulseComponent pulse = ArtEcs.world().get(entity, EffectPulseComponent.class);
            float elapsed = pulse.elapsed + dt;
            if (elapsed >= pulse.duration) {
                pulse = new EffectPulseComponent(pulse.windowId, pulse.targetId, pulse.effectId,
                        pulse.baseline, pulse.duration, pulse.duration, pulse.closeAfter);
                applyFrame(pulse);
                done.add(entity);
            } else {
                pulse = new EffectPulseComponent(pulse.windowId, pulse.targetId, pulse.effectId,
                        pulse.baseline, pulse.duration, elapsed, pulse.closeAfter);
                ArtEcs.world().put(entity, EffectPulseComponent.class, pulse);
                applyFrame(pulse);
            }
        }
        for (EntityId entity : done) {
            finish(entity);
        }
    }

    public static void resetForTests() {
        for (EntityId entity : new ArrayList<EntityId>(ArtEcs.world().query(EffectPulseComponent.class))) {
            ArtEcs.world().destroyEntity(entity);
        }
        CLOSE_CALLBACKS.clear();
    }

    private static EntityId start(EffectPulseComponent pulse) {
        EntityId entity = ArtEcs.world().createEntity();
        ArtEcs.world().put(entity, EffectPulseComponent.class, pulse);
        applyFrame(pulse);
        return entity;
    }

    private static void applyFrame(EffectPulseComponent st) {
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
        updateEffect(st.windowId, st.targetId, st.effectId, "freeze", Float.valueOf(1f));
        updateEffect(st.windowId, st.targetId, st.effectId, "phase", Float.valueOf(phase));
        updateEffect(st.windowId, st.targetId, st.effectId, "intensity", Float.valueOf(intensity));
        updateEffect(st.windowId, st.targetId, st.effectId, "width",
                Float.valueOf(0.32f + 0.1f * envelope));
        mirrorTree(st.windowId, st.targetId, intensity);
    }

    private static void finish(EntityId entity) {
        EffectPulseComponent st = ArtEcs.world().get(entity, EffectPulseComponent.class);
        updateEffect(st.windowId, st.targetId, st.effectId, "freeze", Float.valueOf(0f));
        updateEffect(st.windowId, st.targetId, st.effectId, "width", Float.valueOf(0.28f));
        updateEffect(st.windowId, st.targetId, st.effectId, "intensity", Float.valueOf(st.baseline));
        mirrorTree(st.windowId, st.targetId, st.baseline);
        Runnable callback = CLOSE_CALLBACKS.remove(entity);
        ArtEcs.world().destroyEntity(entity);
        if (st.closeAfter) {
            try {
                if (callback != null) callback.run(); else ArtFramework.close(st.windowId);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void mirrorTree(String windowId, String targetId, float intensity) {
        EntityId entity = findC1Entity(windowId, nodeIdFromTarget(windowId, targetId));
        if (entity == null) {
            entity = findC1Entity(windowId, "panel");
        }
        if (entity == null) {
            entity = findC1Entity(windowId, "p");
        }
        if (entity != null) {
            NodePropertiesComponent properties = ArtEcs.world().get(entity, NodePropertiesComponent.class);
            if (properties != null) {
                ArtEcs.world().put(entity, NodePropertiesComponent.class,
                        properties.with("fx_intensity", Float.valueOf(intensity)));
            }
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
        EntityId slider = findC1Entity(windowId, "wave_slider");
        if (slider == null) {
            slider = findC1Entity(windowId, "wave");
        }
        if (slider != null) {
            ControlValueComponent value = ArtEcs.world().get(slider, ControlValueComponent.class);
            if (value != null && value.value instanceof Number) {
                return clamp(((Number) value.value).floatValue());
            }
        }
        EntityId target = findC1Entity(windowId, targetNodeId);
        if (target == null) {
            target = findC1Entity(windowId, "panel");
        }
        if (target != null) {
            NodePropertiesComponent properties = ArtEcs.world().get(target, NodePropertiesComponent.class);
            Object intensity = properties != null ? properties.get("fx_intensity") : null;
            if (intensity instanceof Number) {
                return clamp(((Number) intensity).floatValue());
            }
        }
        return 0.55f;
    }

    /** Find a C1 visual entity by its host realization key, never through a tree facade. */
    private static EntityId findC1Entity(String windowId, String effectKey) {
        if (windowId == null || effectKey == null || effectKey.isEmpty()) {
            return null;
        }
        PresentationContext context = PresentationRegistry.existingContext("tree:" + windowId);
        if (context != null) {
            for (EntityId entity : context.entities()) {
                NodeIdentityComponent identity = ArtEcs.world().get(entity, NodeIdentityComponent.class);
                if (identity != null && effectKey.equals(identity.name)) return entity;
            }
        }
        String key = windowId + ":" + effectKey;
        for (EntityId entity : ArtEcs.world().query(HostBindingComponent.class)) {
            HostBindingComponent binding = ArtEcs.world().get(entity, HostBindingComponent.class);
            if (binding != null && "SCENE2D_C1".equals(binding.hostKind)
                    && (key.equals(binding.localKey)
                    || binding.localKey.endsWith(":" + effectKey)
                    || binding.localKey.endsWith("/" + effectKey))) {
                return entity;
            }
        }
        return null;
    }

    private static void updateEffect(
            String windowId, String targetId, String effectId, String name, Object value) {
        EntityId entity = findC1Entity(windowId, nodeIdFromTarget(windowId, targetId));
        if (entity == null) {
            entity = findC1Entity(windowId, "panel");
        }
        if (entity == null) return;
        EffectsComponent effects = ArtEcs.world().get(entity, EffectsComponent.class);
        if (effects == null) return;
        String layer = effects.get(effectId, "pulse") != null ? "pulse" : "ambient";
        ArtEcs.world().put(entity, EffectsComponent.class,
                effects.withParam(effectId, layer, name, value));
        artframework.render.RenderProjectionQueue.request(windowId);
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
