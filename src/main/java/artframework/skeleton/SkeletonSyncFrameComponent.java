package artframework.skeleton;

/** Last accepted frame for the skeleton backend snapshot stream. */
public final class SkeletonSyncFrameComponent {
    public final long frameId;

    public SkeletonSyncFrameComponent(long frameId) {
        this.frameId = frameId;
    }
}
