package artframework.context;

/** Data-only result of the latest surface intent execution. */
public final class SurfaceResultComponent {
    public final IntentResult.Status status;
    public final String message;

    public SurfaceResultComponent(IntentResult.Status status, String message) {
        if (status == null) throw new IllegalArgumentException("status required");
        this.status = status;
        this.message = message != null ? message : "";
    }
}
