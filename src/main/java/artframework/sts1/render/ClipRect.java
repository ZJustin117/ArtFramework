package artframework.sts1.render;

/** Axis-aligned clip in present space (milestone 16.3). */
public final class ClipRect {

    public final float x;
    public final float y;
    public final float width;
    public final float height;

    public ClipRect(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0f, width);
        this.height = Math.max(0f, height);
    }

    public static ClipRect none() {
        return new ClipRect(0f, 0f, 0f, 0f);
    }

    public boolean isEmpty() {
        return width <= 0f || height <= 0f;
    }

    public boolean contains(float px, float py) {
        return px >= x && py >= y && px < x + width && py < y + height;
    }
}
