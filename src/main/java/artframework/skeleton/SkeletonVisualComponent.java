package artframework.skeleton;

/** Visibility and tint state for a presentation skeleton. */
public final class SkeletonVisualComponent {
    public final boolean visible;
    public final float red;
    public final float green;
    public final float blue;
    public final float alpha;

    public SkeletonVisualComponent(boolean visible, float red, float green,
            float blue, float alpha) {
        this.visible = visible;
        this.red = clamp(red); this.green = clamp(green); this.blue = clamp(blue);
        this.alpha = clamp(alpha);
    }

    public SkeletonVisualComponent(boolean visible) {
        this(visible, 1f, 1f, 1f, 1f);
    }

    private static float clamp(float value) {
        return value < 0f ? 0f : (value > 1f ? 1f : value);
    }
}
