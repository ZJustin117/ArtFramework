package artframework.assets;

/**
 * Host-neutral descriptor for one packed region inside an atlas page.
 *
 * <p>Packed pixel bounds are {@code width x height} within a {@code pageWidth x pageHeight} page.
 * Rotation is expressed by {@code degrees}. Only {@code 0} and {@code 90} are supported (matching
 * libGDX atlas packing); {@code 180} is treated as rotated for display-swap purposes because
 * {@link #rotated()} is simply {@code degrees != 0}. A rotated packed region is drawn with its
 * displayed dimensions swapped ({@link #displayWidth()}/{@link #displayHeight()}).
 *
 * <p>Trimmed regions keep the packed size in {@code width}/{@code height} and the untrimmed
 * size/placement in {@code originalWidth}/{@code originalHeight}/{@code offsetX}/{@code offsetY}.
 * {@code originalWidth}/{@code originalHeight} are expressed in <em>display orientation</em> (the
 * same orientation as {@link #displayWidth()}/{@link #displayHeight()}), so an untrimmed rotated
 * region defaults them to the swapped display dimensions and reports {@link #trimmed()} false.
 *
 * <p>Immutable value type with no host (libGDX/STS) dependencies, so an STS1 adapter can fill it
 * from a {@code TextureAtlas.AtlasRegion} without leaking host types. All derived accessors are
 * deterministic and side-effect free; invalid regions return safe/clamped values and never throw.
 *
 * <p>Intended split from {@link artframework.skeleton.SpineAtlasRegion}: this type is the generic
 * asset atlas region (page identity, page size, UV, rotation, trim metadata) for arbitrary
 * textures, while {@code SpineAtlasRegion} is scoped to Spine skeleton track metadata and carries
 * a Spine-specific {@code scale}. This class deliberately does not import or duplicate Spine
 * concerns; no extraction/sharing between the two is attempted in this slice.
 */
public final class AtlasRegion {

    public final String page;
    public final String name;
    public final int x;
    public final int y;
    public final int width;
    public final int height;
    public final int pageWidth;
    public final int pageHeight;
    public final int originalWidth;
    public final int originalHeight;
    public final int offsetX;
    public final int offsetY;
    public final int degrees;

    public AtlasRegion(
            String page,
            String name,
            int x,
            int y,
            int width,
            int height,
            int pageWidth,
            int pageHeight,
            int originalWidth,
            int originalHeight,
            int offsetX,
            int offsetY,
            int degrees) {
        this.page = page != null ? page : "";
        this.name = name != null ? name : "";
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        boolean rotated = degrees != 0;
        int displayWidth = rotated ? height : width;
        int displayHeight = rotated ? width : height;
        this.originalWidth = originalWidth > 0 ? originalWidth : displayWidth;
        this.originalHeight = originalHeight > 0 ? originalHeight : displayHeight;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.degrees = degrees;
    }

    public static AtlasRegion of(
            String page,
            String name,
            int x,
            int y,
            int width,
            int height,
            int pageWidth,
            int pageHeight) {
        return new AtlasRegion(page, name, x, y, width, height, pageWidth, pageHeight, 0, 0, 0, 0, 0);
    }

    /** True when the packed region is rotated in the page. */
    public boolean rotated() {
        return degrees != 0;
    }

    /**
     * True when the displayed size differs from the untrimmed {@code originalWidth}/
     * {@code originalHeight} (both in display orientation), i.e. the region was trimmed.
     */
    public boolean trimmed() {
        return displayWidth() != originalWidth || displayHeight() != originalHeight;
    }

    /** Displayed width: packed {@code height} when rotated, otherwise packed {@code width}. */
    public int displayWidth() {
        return rotated() ? height : width;
    }

    /** Displayed height: packed {@code width} when rotated, otherwise packed {@code height}. */
    public int displayHeight() {
        return rotated() ? width : height;
    }

    /** True when packed bounds are positive and fully inside a positive-sized page. */
    public boolean valid() {
        return width > 0
                && height > 0
                && pageWidth > 0
                && pageHeight > 0
                && x >= 0
                && y >= 0
                && (long) x + width <= pageWidth
                && (long) y + height <= pageHeight;
    }

    /** Packed pixel rect as {@code {x, y, width, height}}. */
    public float[] sourceRect() {
        return new float[] { x, y, width, height };
    }

    /**
     * Normalized packed rect as {@code {u, v, u2, v2}}, each value clamped into {@code [0,1]}.
     * When the page size is not positive, returns the fail-safe full-texture rect
     * {@code {0, 0, 1, 1}} instead of dividing by zero. Bounds are summed in {@code long} so
     * near-{@code Integer.MAX_VALUE} inputs cannot wrap into a negative rect.
     */
    public float[] uvRect() {
        if (pageWidth <= 0 || pageHeight <= 0) {
            return new float[] { 0f, 0f, 1f, 1f };
        }
        float u = clamp01((float) x / (float) pageWidth);
        float v = clamp01((float) y / (float) pageHeight);
        float u2 = clamp01((float) ((long) x + width) / (float) pageWidth);
        float v2 = clamp01((float) ((long) y + height) / (float) pageHeight);
        return new float[] { Math.min(u, u2), Math.min(v, v2), Math.max(u, u2), Math.max(v, v2) };
    }

    /** Untrimmed placement rect as {@code {offsetX, offsetY, originalWidth, originalHeight}}. */
    public float[] trimmedRect() {
        return new float[] { offsetX, offsetY, originalWidth, originalHeight };
    }

    private static float clamp01(float value) {
        if (value < 0f) {
            return 0f;
        }
        if (value > 1f) {
            return 1f;
        }
        return value;
    }
}
