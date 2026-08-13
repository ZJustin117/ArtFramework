package artframework.context;

/** Data-only intent identity recorded before the host signal executor runs. */
public final class SurfaceIntentComponent {
    public final String name;
    public final String surfaceId;

    public SurfaceIntentComponent(String name, String surfaceId) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("name required");
        this.name = name;
        this.surfaceId = surfaceId != null ? surfaceId : "";
    }
}
