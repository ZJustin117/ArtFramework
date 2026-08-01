package artframework.c2;

/** Layout state for an EntityPresent slot. */
public final class EntitySlotTransformComponent {
    public final float x;
    public final float y;
    public final float scale;
    public final boolean laidOut;

    public EntitySlotTransformComponent(float x, float y, float scale, boolean laidOut) {
        this.x = x;
        this.y = y;
        this.scale = scale;
        this.laidOut = laidOut;
    }
}
