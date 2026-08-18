package artframework.presentation;

/** Finite runtime range materialized for slider and progress controls. */
public final class ControlBoundsComponent {
    public final float min;
    public final float max;

    public ControlBoundsComponent(float min, float max) {
        if (Float.isNaN(min) || Float.isInfinite(min)
                || Float.isNaN(max) || Float.isInfinite(max)) {
            throw new IllegalArgumentException("control bounds must be finite");
        }
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
    }
}
