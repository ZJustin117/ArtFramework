package artframework.context;

import artframework.component.Rect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable observe-first snapshot for native card pile, card-group, and soul chrome. */
public final class PileSoulView {
    public final List<Entry> entries;
    public final boolean available;

    public PileSoulView(List<Entry> entries, boolean available) {
        this.entries = entries == null || entries.isEmpty()
                ? Collections.<Entry>emptyList()
                : Collections.unmodifiableList(new ArrayList<Entry>(entries));
        this.available = available;
    }

    public static PileSoulView empty() {
        return new PileSoulView(null, false);
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
        public final String zone;
        public final int count;
        public final Rect bounds;
        public final boolean visible;
        public final String resourceId;
        public final String label;

        public Entry(String id, String kind, String zone, int count, Rect bounds,
                boolean visible, String resourceId, String label) {
            this.id = id != null ? id : "";
            this.kind = kind != null ? kind : "";
            this.zone = zone != null ? zone : "";
            this.count = count;
            this.bounds = bounds != null ? bounds : Rect.ZERO;
            this.visible = visible;
            this.resourceId = resourceId != null ? resourceId : "";
            this.label = label != null ? label : "";
        }

        public Map<String, Object> toMap() {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("id", id); out.put("kind", kind); out.put("zone", zone);
            out.put("count", Integer.valueOf(count)); out.put("visible", Boolean.valueOf(visible));
            out.put("resourceId", resourceId); out.put("label", label);
            out.put("x", Float.valueOf(bounds.x)); out.put("y", Float.valueOf(bounds.y));
            out.put("w", Float.valueOf(bounds.width)); out.put("h", Float.valueOf(bounds.height));
            return out;
        }
    }
}
