package artframework.presentation;

/**
 * Current value of a synthetic control.
 *
 * <p>This is data only. The component is used for sliders, text fields, checkboxes, and progress
 * controls regardless of how their declaration or external value originated.</p>
 */
public final class ControlValueComponent {
    public final Object value;

    public ControlValueComponent(Object value) {
        if (value instanceof Float) {
            float number = ((Float) value).floatValue();
            if (Float.isNaN(number) || Float.isInfinite(number)) {
                throw new IllegalArgumentException("control value must be finite");
            }
        } else if (!(value instanceof String) && !(value instanceof Boolean)) {
            throw new IllegalArgumentException("unsupported control value: "
                    + (value == null ? "null" : value.getClass().getName()));
        }
        this.value = value;
    }
}
