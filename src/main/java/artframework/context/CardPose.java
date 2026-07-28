package artframework.context;

/** Authoritative layout pose for a card in a context frame (hard-sync geometry). */
public final class CardPose {

    public final float x;
    public final float y;
    public final float rotation;
    public final float scale;
    public final float z;
    public final boolean visible;

    public CardPose(float x, float y, float rotation, float scale, float z, boolean visible) {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.scale = scale;
        this.z = z;
        this.visible = visible;
    }

    public static CardPose at(float x, float y) {
        return new CardPose(x, y, 0f, 1f, 0f, true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CardPose)) {
            return false;
        }
        CardPose p = (CardPose) o;
        return Float.compare(p.x, x) == 0
                && Float.compare(p.y, y) == 0
                && Float.compare(p.rotation, rotation) == 0
                && Float.compare(p.scale, scale) == 0
                && Float.compare(p.z, z) == 0
                && p.visible == visible;
    }

    @Override
    public int hashCode() {
        int h = Float.floatToIntBits(x);
        h = 31 * h + Float.floatToIntBits(y);
        h = 31 * h + Float.floatToIntBits(rotation);
        h = 31 * h + Float.floatToIntBits(scale);
        h = 31 * h + Float.floatToIntBits(z);
        h = 31 * h + (visible ? 1 : 0);
        return h;
    }
}
