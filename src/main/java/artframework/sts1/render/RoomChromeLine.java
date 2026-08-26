package artframework.sts1.render;

/**
 * One minimal text-chrome row ART paints when it owns rest/shop/treasure pixels
 * (Slice C phase 2). Pure projection data: no host objects.
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
