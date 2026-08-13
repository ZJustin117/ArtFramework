package artframework.context;

/** Data-only authority-frame observation following the latest native intent. */
public final class NativeIntentObservationComponent {
    public final long frameId;
    public final long sceneEpoch;
    public final String scene;

    public NativeIntentObservationComponent(long frameId, long sceneEpoch, String scene) {
        this.frameId = frameId;
        this.sceneEpoch = sceneEpoch;
        this.scene = scene != null ? scene : "";
    }
}
