package artframework.render;

import artframework.api.ArtFramework;
import artframework.core.EffectPulse;
import artframework.ecs.ArtEcs;
import artframework.ecs.EntityId;
import artframework.presentation.EffectsComponent;
import artframework.presentation.HostBindingComponent;
import artframework.presentation.NodePropertiesComponent;
import artframework.presentation.NodeIdentityComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRegistry;

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
            any = setIntensity(windowId, sliderId, intensity);
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

    /** Advance active pulses through the schedule-owned effect system. */
    @Deprecated
    public static void tickPulses(float dt) {
        ArtFramework.executeEffectPulses(dt);
    }

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
            if (setIntensity(windowId, candidates[i].substring(("c1:" + windowId + ":").length()),
                    intensity)) {
                return true;
            }
        }
        return false;
    }

    private static void mirrorTreeProp(String windowId, float intensity) {
        EntityId panel = entity(windowId, "panel");
        if (panel == null) panel = entity(windowId, "p");
        if (panel != null) {
            NodePropertiesComponent properties = ArtEcs.world().get(panel, NodePropertiesComponent.class);
            if (properties != null) ArtEcs.world().put(panel, NodePropertiesComponent.class,
                    properties.with("fx_intensity", Float.valueOf(intensity)));
        }
    }

    private static boolean setIntensity(String windowId, String effectKey, float intensity) {
        EntityId entity = entity(windowId, effectKey);
        if (entity == null) return false;
        EffectsComponent effects = ArtEcs.world().get(entity, EffectsComponent.class);
        if (effects == null || effects.get(LightwaveEffect.ID, "ambient") == null) return false;
        ArtEcs.world().put(entity, EffectsComponent.class, effects.withParam(
                LightwaveEffect.ID, "ambient", "intensity", Float.valueOf(intensity)));
        RenderProjectionQueue.request(windowId);
        return true;
    }

    private static EntityId entity(String windowId, String effectKey) {
        if (windowId == null || effectKey == null || effectKey.isEmpty()) return null;
        PresentationContext context = PresentationRegistry.existingContext("tree:" + windowId);
        if (context != null) {
            for (EntityId entity : context.entities()) {
                NodeIdentityComponent identity = ArtEcs.world().get(entity, NodeIdentityComponent.class);
                if (identity != null && effectKey.equals(identity.name)) return entity;
            }
        }
        String localKey = windowId + ":" + effectKey;
        for (EntityId entity : ArtEcs.world().query(HostBindingComponent.class)) {
            HostBindingComponent binding = ArtEcs.world().get(entity, HostBindingComponent.class);
            if (binding != null && "SCENE2D_C1".equals(binding.hostKind)
                    && (localKey.equals(binding.localKey)
                    || binding.localKey.endsWith(":" + effectKey)
                    || binding.localKey.endsWith("/" + effectKey))) return entity;
        }
        return null;
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
