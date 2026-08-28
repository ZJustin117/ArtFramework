package artframework.sts1.render;

/**
 * One minimal text-chrome row ART paints for delegated rest/shop/treasure surfaces
 * (Slice C phase 2). This is exposed pixel supply, not complete base-art parity.
 */
public final class RoomChromeLine {

    public final String id;
    public final String text;
    public final boolean enabled;

    public RoomChromeLine(String id, String text, boolean enabled) {
        this.id = id != null ? id : "";
        this.text = text != null ? text : "";
        this.enabled = enabled;
    }
}
