package artframework.core;

/**
 * RGBA color token for {@link Theme} (pure; no GL types).
 */
public final class ThemeColor {

    public final float r;
    public final float g;
    public final float b;
    public final float a;

    public ThemeColor(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ThemeColor)) {
            return false;
        }
        ThemeColor c = (ThemeColor) o;
        return Float.compare(c.r, r) == 0
                && Float.compare(c.g, g) == 0
                && Float.compare(c.b, b) == 0
                && Float.compare(c.a, a) == 0;
    }

    @Override
    public int hashCode() {
        int h = Float.floatToIntBits(r);
        h = 31 * h + Float.floatToIntBits(g);
        h = 31 * h + Float.floatToIntBits(b);
        h = 31 * h + Float.floatToIntBits(a);
        return h;
    }

    @Override
    public String toString() {
        return "ThemeColor{" + r + "," + g + "," + b + "," + a + "}";
    }
}
