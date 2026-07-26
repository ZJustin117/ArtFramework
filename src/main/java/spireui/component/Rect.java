package spireui.component;

/**
 * Axis-aligned bounds in layout space (pure; no GL).
 */
public final class Rect {

    public static final Rect ZERO = new Rect(0f, 0f, 0f, 0f);

    public final float x;
    public final float y;
    public final float width;
    public final float height;

    public Rect(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float right() {
        return x + width;
    }

    public float top() {
        return y + height;
    }

    @Override
    public String toString() {
        return "Rect{x=" + x + ",y=" + y + ",w=" + width + ",h=" + height + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Rect)) {
            return false;
        }
        Rect r = (Rect) o;
        return Float.compare(r.x, x) == 0
                && Float.compare(r.y, y) == 0
                && Float.compare(r.width, width) == 0
                && Float.compare(r.height, height) == 0;
    }

    @Override
    public int hashCode() {
        int h = Float.floatToIntBits(x);
        h = 31 * h + Float.floatToIntBits(y);
        h = 31 * h + Float.floatToIntBits(width);
        h = 31 * h + Float.floatToIntBits(height);
        return h;
    }
}
