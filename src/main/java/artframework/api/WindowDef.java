package artframework.api;

/**
 * Registration descriptor for a window (synthetic layout or native template id).
 */
public final class WindowDef {

    public final String id;
    public final WindowClass windowClass;
    /** Layout resource path (C1) or native template id such as {@code sts.map} (C2). */
    public final String resource;

    public WindowDef(String id, WindowClass windowClass, String resource) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id required");
        }
        if (windowClass == null) {
            throw new IllegalArgumentException("windowClass required");
        }
        this.id = id;
        this.windowClass = windowClass;
        this.resource = resource != null ? resource : "";
    }
}
