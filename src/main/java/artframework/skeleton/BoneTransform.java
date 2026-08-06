package artframework.skeleton;

/**
 * Host-neutral 2D bone transform snapshot.
 */
public final class BoneTransform {

    public final float x;
    public final float y;
    public final float rotationDegrees;
    public final float scaleX;
    public final float scaleY;

    public BoneTransform(float x, float y, float rotationDegrees, float scaleX, float scaleY) {
        this.x = x;
        this.y = y;
        this.rotationDegrees = rotationDegrees;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }
}
