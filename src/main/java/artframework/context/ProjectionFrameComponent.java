package artframework.context;

/** Data-only lifecycle metadata for the C2 context projection. */
public final class ProjectionFrameComponent {
    public final long frameId;
    public final long sceneEpoch;
    public final String scene;
    public final boolean available;
    public final boolean stale;

    public ProjectionFrameComponent(long frameId, long sceneEpoch, String scene,
            boolean available, boolean stale) {
        this.frameId = frameId;
        this.sceneEpoch = sceneEpoch;
        this.scene = scene != null ? scene : "";
        this.available = available;
        this.stale = stale;
    }
}
