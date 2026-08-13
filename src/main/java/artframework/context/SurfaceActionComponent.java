package artframework.context;

/** Data-only record of the latest action submitted by a C2 surface. */
public final class SurfaceActionComponent {
    public final String name;

    public SurfaceActionComponent(String name) {
        this.name = name != null ? name : "";
    }
}
