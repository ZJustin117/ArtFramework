package artframework.context;

/** Immutable control presentation state. */
public final class ControlView {

    public final String id;
    public final String text;
    public final String iconResourceId;
    public final boolean visible;
    public final boolean enabled;

    public ControlView(String id, String text, String iconResourceId, boolean visible, boolean enabled) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("control id required");
        }
        this.id = id;
        this.text = text != null ? text : "";
        this.iconResourceId = iconResourceId != null ? iconResourceId : "";
        this.visible = visible;
        this.enabled = enabled;
    }
}
