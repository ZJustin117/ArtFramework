package artframework.context;

/** Data-only record of the latest native input routed by ART. */
public final class NativeInputComponent {
    public final String name;
    public final String surfaceId;

    public NativeInputComponent(String name, String surfaceId) {
        this.name = name != null ? name : "";
        this.surfaceId = surfaceId != null ? surfaceId : "";
    }
}
