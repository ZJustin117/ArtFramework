package spireui.component;

/**
 * Layout modifiers on a {@link UiNode}. Effects do not use this.
 * Godot-aligned: min size, size flags, stretch ratio, cross-axis align.
 */
public final class LayoutSpec {

    public static final LayoutSpec EMPTY =
            new LayoutSpec(0f, 0f, 0f, 0f, 0f, 0f, SizeFlags.DEFAULT, SizeFlags.DEFAULT, 1f, Align.BEGIN);

    public final float width;
    public final float height;
    public final float minWidth;
    public final float minHeight;
    public final float pad;
    public final float gap;
    /** @deprecated prefer {@link #sizeFlagsH} / main-axis expand; kept for callers */
    public final boolean grow;
    public final int sizeFlagsH;
    public final int sizeFlagsV;
    public final float stretchRatio;
    public final String align;

    /** Legacy constructor: width/height/pad/gap/grow. */
    public LayoutSpec(float width, float height, float pad, float gap, boolean grow) {
        this(
                width,
                height,
                0f,
                0f,
                pad,
                gap,
                SizeFlags.fromGrow(grow),
                SizeFlags.fromGrow(grow),
                1f,
                Align.BEGIN);
    }

    public LayoutSpec(
            float width,
            float height,
            float minWidth,
            float minHeight,
            float pad,
            float gap,
            int sizeFlagsH,
            int sizeFlagsV,
            float stretchRatio,
            String align) {
        this.width = width;
        this.height = height;
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.pad = pad;
        this.gap = gap;
        this.sizeFlagsH = sizeFlagsH;
        this.sizeFlagsV = sizeFlagsV;
        this.stretchRatio = stretchRatio > 0f ? stretchRatio : 1f;
        this.align = Align.normalize(align);
        this.grow = SizeFlags.expands(sizeFlagsH) || SizeFlags.expands(sizeFlagsV);
    }

    public LayoutSpec withSize(float w, float h) {
        return new LayoutSpec(
                w, h, minWidth, minHeight, pad, gap, sizeFlagsH, sizeFlagsV, stretchRatio, align);
    }

    public boolean hasWidth() {
        return width > 0f;
    }

    public boolean hasHeight() {
        return height > 0f;
    }

    public boolean hasMinWidth() {
        return minWidth > 0f;
    }

    public boolean hasMinHeight() {
        return minHeight > 0f;
    }

    public boolean expandsH() {
        return SizeFlags.expands(sizeFlagsH);
    }

    public boolean expandsV() {
        return SizeFlags.expands(sizeFlagsV);
    }
}
