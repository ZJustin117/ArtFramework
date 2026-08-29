package artframework.context;

import artframework.component.Rect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable observe-first snapshot for native relic, potion, and blight pixels. */
public final class RelicPotionBlightView {
    public final List<Entry> entries;
    public final boolean available;

    public RelicPotionBlightView(List<Entry> entries, boolean available) {
        this.entries = entries == null || entries.isEmpty()
                ? Collections.<Entry>emptyList()
                : Collections.unmodifiableList(new ArrayList<Entry>(entries));
        this.available = available;
    }

    public static RelicPotionBlightView empty() {
        return new RelicPotionBlightView(null, false);
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
        public final String resourceId;
        public final int count;
        public final boolean usable;
        public final Rect bounds;
        public final boolean visible;

        public Entry(String id, String kind, String label, String resourceId, int count,
                boolean usable, Rect bounds, boolean visible) {
            this.id = id != null ? id : "";
            this.kind = kind != null ? kind : "";
            this.label = label != null ? label : "";
            this.resourceId = resourceId != null ? resourceId : "";
            this.count = count;
            this.usable = usable;
            this.bounds = bounds != null ? bounds : Rect.ZERO;
            this.visible = visible;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("id", id); out.put("kind", kind); out.put("label", label);
            out.put("resourceId", resourceId); out.put("count", Integer.valueOf(count));
            out.put("usable", Boolean.valueOf(usable)); out.put("visible", Boolean.valueOf(visible));
            out.put("x", Float.valueOf(bounds.x)); out.put("y", Float.valueOf(bounds.y));
            out.put("w", Float.valueOf(bounds.width)); out.put("h", Float.valueOf(bounds.height));
            return out;
        }
    }
}
