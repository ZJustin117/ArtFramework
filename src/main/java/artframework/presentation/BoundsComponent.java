package artframework.presentation;

import artframework.component.Rect;

/** Host-neutral layout and draw bounds. */
public final class BoundsComponent {
    public final Rect rect;
    public final float z;

    public BoundsComponent(Rect rect, float z) {
        this.rect = rect != null ? rect : Rect.ZERO;
        this.z = z;
    }
}
