package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.AssetResolveResult;
import artframework.assets.ResourceIds;
import artframework.component.Rect;
import artframework.context.OrbStanceView;
import artframework.sts1.assets.Sts1VanillaCatalog;
import artframework.sts1.backend.Sts1OrbStanceProjection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resource-backed, non-authoritative overlay description for native orb/stance observations. */
public final class Sts1OrbStanceDrawPath {
    public static final class DrawItem {
        public final String id, kind, label, resourceId;
        public final int count, passive, evoke;
        public final boolean active, visible, resourceFound;
        public final Rect bounds;

        DrawItem(OrbStanceView.Entry entry) {
            id = entry.id; kind = entry.kind; label = entry.label;
            resourceId = resourceOrFallback(entry.kind, entry.resourceId);
            count = entry.count; passive = entry.passive; evoke = entry.evoke;
            active = entry.active; visible = entry.visible; bounds = entry.bounds;
            AssetResolveResult resolved = resolve(resourceId);
            resourceFound = resolved != null && resolved.found;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("id", id); out.put("kind", kind); out.put("label", label);
            out.put("resourceId", resourceId); out.put("count", Integer.valueOf(count));
            out.put("passive", Integer.valueOf(passive)); out.put("evoke", Integer.valueOf(evoke));
            out.put("active", Boolean.valueOf(active)); out.put("visible", Boolean.valueOf(visible));
            out.put("resourceFound", Boolean.valueOf(resourceFound));
            out.put("x", Float.valueOf(bounds.x)); out.put("y", Float.valueOf(bounds.y));
            out.put("w", Float.valueOf(bounds.width)); out.put("h", Float.valueOf(bounds.height));
            return out;
        }
    }

    private Sts1OrbStanceDrawPath() {}

    public static List<DrawItem> buildFromProjection() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        for (OrbStanceView.Entry entry : Sts1OrbStanceProjection.current().entries) {
            out.add(new DrawItem(entry));
        }
        return out;
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("available", Boolean.valueOf(Sts1OrbStanceProjection.current().available));
        out.put("entryCount", Integer.valueOf(items.size()));
        out.put("nativeContinuation", Boolean.TRUE);
        out.put("nativePixelsSuppressed", Boolean.FALSE);
        out.put("surface", "overlay-only");
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem item : items) list.add(item.toMap());
        out.put("items", list);
        return out;
    }

    private static AssetResolveResult resolve(String resourceId) {
        if (resourceId == null || resourceId.isEmpty()) return null;
        try { return ArtFramework.assets().resolve(resourceId); } catch (Throwable ignored) { return null; }
    }

    private static String resourceOrFallback(String kind, String resourceId) {
        if (Sts1VanillaCatalog.isKnown(resourceId)) return resourceId;
        if ("stance".equals(kind)) return ResourceIds.STANCE_UNKNOWN;
        return ResourceIds.ORB_UNKNOWN;
    }
}
