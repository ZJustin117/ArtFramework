package artframework.skeleton;

/**
 * Neutral metadata for a Spine/libGDX atlas region.
 */
public final class SpineAtlasRegion {

    public final String page;
    public final String name;
    public final int x;
    public final int y;
    public final int width;
    public final int height;
    public final int originalWidth;
    public final int originalHeight;
    public final int offsetX;
    public final int offsetY;
    public final int degrees;
    public final float scale;

    public SpineAtlasRegion(
            String page,
            String name,
            int x,
            int y,
            int width,
            int height,
            int originalWidth,
            int originalHeight,
            int offsetX,
            int offsetY,
            int degrees,
            float scale) {
        this.page = page != null ? page : "";
        this.name = name != null ? name : "";
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.originalWidth = originalWidth > 0 ? originalWidth : width;
        this.originalHeight = originalHeight > 0 ? originalHeight : height;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.degrees = degrees;
        this.scale = scale;
    }

    public boolean rotated() {
        return degrees != 0;
    }
}
