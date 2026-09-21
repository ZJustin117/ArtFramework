package artframework.render;

import artframework.component.Rect;

/** Immutable host-neutral render item ordering projection. */
public final class RenderItem {
    public final String id;
    public final RenderTargetKind kind;
    public final Rect bounds;
    public final RenderOrder order;
    public final boolean enabled;

    public RenderItem(String id, RenderTargetKind kind, Rect bounds, RenderOrder order,
            boolean enabled) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("render item id required");
        if (kind == null) throw new IllegalArgumentException("render item kind required");
        if (bounds == null) throw new IllegalArgumentException("render item bounds required");
        if (order == null) throw new IllegalArgumentException("render item order required");
        this.id = id;
        this.kind = kind;
        this.bounds = bounds;
        this.order = order;
        this.enabled = enabled;
    }
}
