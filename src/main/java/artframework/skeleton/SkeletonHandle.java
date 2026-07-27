package artframework.skeleton;

/**
 * Opaque loaded skeleton instance.
 */
public final class SkeletonHandle {

    public final String providerId;
    public final String skeletonId;
    public final Object nativeRef;
    private boolean alive = true;

    public SkeletonHandle(String providerId, String skeletonId, Object nativeRef) {
        this.providerId = providerId != null ? providerId : "";
        this.skeletonId = skeletonId != null ? skeletonId : "";
        this.nativeRef = nativeRef;
    }

    public boolean isAlive() {
        return alive;
    }

    void markDisposed() {
        alive = false;
    }
}
