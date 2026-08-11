package artframework.presentation;

/** Visibility and inherited opacity state. */
public final class VisibilityComponent {
    public final boolean visible;
    public final float opacity;

    public VisibilityComponent(boolean visible, float opacity) {
        this.visible = visible;
        this.opacity = opacity < 0f ? 0f : (opacity > 1f ? 1f : opacity);
    }
}
