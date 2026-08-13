package artframework.presentation;

import artframework.component.UiTypes;
import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

/** Stateless normalization for data-only control values. */
public final class ControlValueSystem implements EcsSystem {
    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        for (EntityId entity : world.query(NodeIdentityComponent.class, NodePropertiesComponent.class,
                ControlValueComponent.class)) {
            NodeIdentityComponent identity = world.get(entity, NodeIdentityComponent.class);
            NodePropertiesComponent props = world.get(entity, NodePropertiesComponent.class);
            ControlValueComponent current = world.get(entity, ControlValueComponent.class);
            if (UiTypes.SLIDER.equals(identity.type) || UiTypes.PROGRESS.equals(identity.type)) {
                float min = number(props.get("min"), 0f);
                float max = number(props.get("max"), 1f);
                float value = number(current.value, min);
                world.put(entity, ControlValueComponent.class, new ControlValueComponent(Float.valueOf(clamp(value, min, max))));
            } else if (UiTypes.TEXTFIELD.equals(identity.type)) {
                world.put(entity, ControlValueComponent.class,
                        new ControlValueComponent(current.value != null ? String.valueOf(current.value) : ""));
            } else if (UiTypes.CHECKBOX.equals(identity.type)) {
                world.put(entity, ControlValueComponent.class,
                        new ControlValueComponent(Boolean.valueOf(Boolean.TRUE.equals(current.value))));
            }
        }
    }

    public static Object initialValue(String type, NodePropertiesComponent props) {
        if (UiTypes.SLIDER.equals(type)) {
            return Float.valueOf(number(props.get("value"), number(props.get("min"), 0f)));
        }
        if (UiTypes.TEXTFIELD.equals(type)) {
            Object value = props.get("text");
            return value != null ? String.valueOf(value) : "";
        }
        if (UiTypes.CHECKBOX.equals(type)) {
            return Boolean.valueOf(Boolean.TRUE.equals(props.get("checked")));
        }
        if (UiTypes.PROGRESS.equals(type)) {
            Object value = props.get("value");
            if (value == null) value = props.get("progress");
            return Float.valueOf(number(value, number(props.get("min"), 0f)));
        }
        return null;
    }

    private static float number(Object value, float fallback) {
        if (value instanceof Number) return ((Number) value).floatValue();
        if (value != null) {
            try { return Float.parseFloat(String.valueOf(value)); }
            catch (NumberFormatException ignored) { }
        }
        return fallback;
    }

    private static float clamp(float value, float min, float max) {
        if (min > max) {
            float swap = min;
            min = max;
            max = swap;
        }
        return Math.max(min, Math.min(max, value));
    }
}
