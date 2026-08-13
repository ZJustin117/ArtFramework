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
        this.value = value;
    }
}
