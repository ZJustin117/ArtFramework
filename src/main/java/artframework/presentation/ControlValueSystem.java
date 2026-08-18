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
        for (EntityId entity : world.query(NodeIdentityComponent.class, ControlValueComponent.class)) {
            NodeIdentityComponent identity = world.get(entity, NodeIdentityComponent.class);
            ControlValueComponent current = world.get(entity, ControlValueComponent.class);
            if (UiTypes.SLIDER.equals(identity.type) || UiTypes.PROGRESS.equals(identity.type)) {
                ControlBoundsComponent bounds = world.get(entity, ControlBoundsComponent.class);
                float min = bounds != null ? bounds.min : 0f;
                float max = bounds != null ? bounds.max : 1f;
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

    public static ControlBoundsComponent bounds(NodePropertiesComponent props) {
        float min = number(props != null ? props.get("min") : null, 0f);
        float max = number(props != null ? props.get("max") : null, 1f);
        return new ControlBoundsComponent(min, max);
    }

    private static float number(Object value, float fallback) {
        if (value instanceof Number) {
            float result = ((Number) value).floatValue();
            return finite(result) ? result : fallback;
        }
        if (value != null) {
            try {
                float result = Float.parseFloat(String.valueOf(value));
                return finite(result) ? result : fallback;
            }
            catch (NumberFormatException ignored) { }
        }
        return fallback;
    }

    private static boolean finite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
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
