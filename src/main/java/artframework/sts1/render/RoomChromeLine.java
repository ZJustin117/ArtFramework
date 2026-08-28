package artframework.sts1.render;

/**
 * One minimal text-chrome row ART paints for delegated rest/shop/treasure surfaces
 * (Slice C phase 2). This is exposed pixel supply, not complete base-art parity.
 */
public final class RoomChromeLine {

    public final String id;
    public final String text;
    public final boolean enabled;
    public final boolean visible;
    public final String role;
    public final String resourceId;
    public final float x;
    public final float y;
    public final float w;
    public final float h;

    public RoomChromeLine(String id, String text, boolean enabled) {
        this(id, text, enabled, true, "", "", 0f, 0f, 0f, 0f);
    }

    public RoomChromeLine(
            String id,
            String text,
            boolean enabled,
            boolean visible,
            String role,
            String resourceId,
            float x,
            float y,
            float w,
            float h) {
        this.id = id != null ? id : "";
        this.text = text != null ? text : "";
        this.enabled = enabled;
        this.visible = visible;
        this.role = role != null ? role : "";
        this.resourceId = resourceId != null ? resourceId : "";
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
}
