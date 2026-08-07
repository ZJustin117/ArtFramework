package artframework.skeleton;

/** World pose supplied by the presentation backend. */
public final class SkeletonPoseComponent {
    public final float x;
    public final float y;
    public final float rotation;
    public final float scaleX;
    public final float scaleY;
    public final boolean flipX;
    public final boolean flipY;
    public final int zIndex;

    public SkeletonPoseComponent(float x, float y, float rotation, float scaleX,
            float scaleY, boolean flipX, boolean flipY, int zIndex) {
        if (!(scaleX > 0f) || !(scaleY > 0f)) {
            throw new IllegalArgumentException("pose scale must be positive");
        }
        this.x = x; this.y = y; this.rotation = rotation;
        this.scaleX = scaleX; this.scaleY = scaleY;
        this.flipX = flipX; this.flipY = flipY; this.zIndex = zIndex;
    }
}
