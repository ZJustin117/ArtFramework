package artframework.context;

import artframework.component.Rect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable observe-first snapshot for native orb slots and current stance state. */
public final class OrbStanceView {
    public final List<Entry> entries;
    public final boolean available;

    public OrbStanceView(List<Entry> entries, boolean available) {
        this.entries = entries == null || entries.isEmpty()
                ? Collections.<Entry>emptyList()
                : Collections.unmodifiableList(new ArrayList<Entry>(entries));
        this.available = available;
    }

    public static OrbStanceView empty() {
        return new OrbStanceView(null, false);
    }

    public int entryCount() { return entries.size(); }

    public Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("available", Boolean.valueOf(available));
        out.put("entryCount", Integer.valueOf(entries.size()));
        List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
        for (Entry entry : entries) values.add(entry.toMap());
        out.put("entries", values);
        return out;
    }

    public static final class Entry {
        public final String id;
        public final String kind;
        public final String label;
        public final int count;
        public final int passive;
        public final int evoke;
        public final boolean active;
        public final String resourceId;
        public final Rect bounds;
        public final boolean visible;
        /** Native stance sprite rotation in degrees; host-neutral. */
        public final float angle;
        /** Native sprite batch tint (multiplied through additive blend); host-neutral. */
        public final float colorR;
        public final float colorG;
        public final float colorB;
        public final float colorA;
        /** Native draw x/y in world pixels, already including the -256 and hb_h/2 offsets. */
        public final float centerX;
        public final float centerY;
        /** Source region size actually drawn: 512x512 when no image is present. */
        public final float width;
        public final float height;
        public final float scale;
        public final boolean hasImage;
        /** Native sprite origin used for rotation (256,256). */
        public final float originX;
        public final float originY;

        public Entry(String id, String kind, String label, int count, int passive, int evoke,
                boolean active, String resourceId, Rect bounds, boolean visible) {
            this(id, kind, label, count, passive, evoke, active, resourceId, bounds, visible,
                    0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 1f, false, 0f, 0f);
        }

        public Entry(String id, String kind, String label, int count, int passive, int evoke,
                boolean active, String resourceId, Rect bounds, boolean visible,
                float angle, float colorR, float colorG, float colorB, float colorA,
                float centerX, float centerY, float width, float height, float scale,
                boolean hasImage, float originX, float originY) {
            this.id = id != null ? id : "";
            this.kind = kind != null ? kind : "";
            this.label = label != null ? label : "";
            this.count = count;
            this.passive = passive;
            this.evoke = evoke;
            this.active = active;
            this.resourceId = resourceId != null ? resourceId : "";
            this.bounds = bounds != null ? bounds : Rect.ZERO;
            this.visible = visible;
            this.angle = angle;
            this.colorR = colorR;
            this.colorG = colorG;
            this.colorB = colorB;
            this.colorA = colorA;
            this.centerX = centerX;
            this.centerY = centerY;
            this.width = width;
            this.height = height;
            this.scale = scale;
            this.hasImage = hasImage;
            this.originX = originX;
            this.originY = originY;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("id", id); out.put("kind", kind); out.put("label", label);
            out.put("count", Integer.valueOf(count)); out.put("passive", Integer.valueOf(passive));
            out.put("evoke", Integer.valueOf(evoke)); out.put("active", Boolean.valueOf(active));
            out.put("resourceId", resourceId); out.put("visible", Boolean.valueOf(visible));
            out.put("x", Float.valueOf(bounds.x)); out.put("y", Float.valueOf(bounds.y));
            out.put("w", Float.valueOf(bounds.width)); out.put("h", Float.valueOf(bounds.height));
            out.put("angle", Float.valueOf(angle));
            out.put("colorR", Float.valueOf(colorR)); out.put("colorG", Float.valueOf(colorG));
            out.put("colorB", Float.valueOf(colorB)); out.put("colorA", Float.valueOf(colorA));
            out.put("centerX", Float.valueOf(centerX)); out.put("centerY", Float.valueOf(centerY));
            out.put("width", Float.valueOf(width)); out.put("height", Float.valueOf(height));
            out.put("scale", Float.valueOf(scale)); out.put("hasImage", Boolean.valueOf(hasImage));
            out.put("originX", Float.valueOf(originX)); out.put("originY", Float.valueOf(originY));
            return out;
        }
    }
}
