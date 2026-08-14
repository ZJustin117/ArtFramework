package artframework.context;

/** Data-only authority-frame observation following the latest native intent. */
public final class NativeIntentObservationComponent {
    public final boolean available;
    public final long frameId;
    public final long sceneEpoch;
    public final String scene;
    public final String message;

    public NativeIntentObservationComponent(long frameId, long sceneEpoch, String scene) {
        this(true, frameId, sceneEpoch, scene, "");
    }

    private NativeIntentObservationComponent(boolean available, long frameId, long sceneEpoch,
            String scene, String message) {
        this.available = available;
        this.frameId = frameId;
        this.sceneEpoch = sceneEpoch;
        this.scene = scene != null ? scene : "";
        this.message = message != null ? message : "";
    }

    public static NativeIntentObservationComponent available(
            long frameId, long sceneEpoch, String scene) {
        return new NativeIntentObservationComponent(true, frameId, sceneEpoch, scene, "");
    }

    public static NativeIntentObservationComponent unavailable(long frameId, String message) {
        return new NativeIntentObservationComponent(false, frameId, -1L, "", message);
    }
}
