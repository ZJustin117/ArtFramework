package artframework.presentation;

/** Host-neutral visual role and resource/text payload key. */
public final class DrawComponent {
    public final String role;
    public final String resourceId;
    public final String text;

    public DrawComponent(String role, String resourceId, String text) {
        this.role = role != null ? role : "";
        this.resourceId = resourceId != null ? resourceId : "";
        this.text = text != null ? text : "";
    }
}
