package artframework.context;

import artframework.component.Rect;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable observe-first metadata for a native room shell. */
public final class RoomShellView {
    public final String kind;
    public final String id;
    public final String title;
    public final String phase;
    public final boolean available;
    public final Rect bounds;
    public final String resourceId;
    public final boolean visible;

    public RoomShellView(String kind, String id, String title, String phase, boolean available,
            Rect bounds, String resourceId, boolean visible) {
        this.kind = kind != null ? kind : "";
        this.id = id != null ? id : "";
        this.title = title != null ? title : "";
        this.phase = phase != null ? phase : "";
        this.available = available;
        this.bounds = bounds != null ? bounds : Rect.ZERO;
        this.resourceId = resourceId != null ? resourceId : "";
        this.visible = visible;
    }

    public static RoomShellView empty() {
        return new RoomShellView("", "", "", "", false, Rect.ZERO, "", false);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("kind", kind); out.put("id", id); out.put("title", title); out.put("phase", phase);
        out.put("available", Boolean.valueOf(available)); out.put("visible", Boolean.valueOf(visible));
        out.put("resourceId", resourceId);
        out.put("x", Float.valueOf(bounds.x)); out.put("y", Float.valueOf(bounds.y));
        out.put("w", Float.valueOf(bounds.width)); out.put("h", Float.valueOf(bounds.height));
        return out;
    }
}
