package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.AssetResolveResult;
import artframework.component.Rect;
import artframework.context.RelicPotionBlightView;
import artframework.sts1.backend.Sts1RelicPotionBlightProjection;
import artframework.sts1.assets.Sts1VanillaCatalog;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resource-backed, non-authoritative overlay description for native relic-family observations. */
public final class Sts1RelicPotionBlightDrawPath {
    public static final class DrawItem {
        public final String id, kind, label, resourceId;
        public final int count;
        public final boolean usable, visible, resourceFound;
        public final Rect bounds;

        DrawItem(RelicPotionBlightView.Entry entry) {
            id = entry.id; kind = entry.kind; label = entry.label;
            resourceId = resourceOrFallback(entry.kind, entry.resourceId);
            count = entry.count; usable = entry.usable; visible = entry.visible; bounds = entry.bounds;
            AssetResolveResult resolved = resolve(resourceId);
            resourceFound = resolved != null && resolved.found;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("id", id); out.put("kind", kind); out.put("label", label);
            out.put("resourceId", resourceId); out.put("count", Integer.valueOf(count));
            out.put("usable", Boolean.valueOf(usable)); out.put("visible", Boolean.valueOf(visible));
            out.put("resourceFound", Boolean.valueOf(resourceFound));
            out.put("x", Float.valueOf(bounds.x)); out.put("y", Float.valueOf(bounds.y));
            out.put("w", Float.valueOf(bounds.width)); out.put("h", Float.valueOf(bounds.height));
            return out;
        }
    }

    private Sts1RelicPotionBlightDrawPath() {}

    public static List<DrawItem> buildFromProjection() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        for (RelicPotionBlightView.Entry entry : Sts1RelicPotionBlightProjection.current().entries) {
            out.add(new DrawItem(entry));
        }
        return out;
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("available", Boolean.valueOf(Sts1RelicPotionBlightProjection.current().available));
        out.put("entryCount", Integer.valueOf(items.size()));
        out.put("nativeContinuation", Boolean.TRUE);
        out.put("nativePixelsSuppressed", Boolean.FALSE);
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
        if ("potion".equals(kind)) return artframework.assets.ResourceIds.POTION_UNKNOWN;
        if ("blight".equals(kind)) return artframework.assets.ResourceIds.BLIGHT_UNKNOWN;
        return artframework.assets.ResourceIds.RELIC_UNKNOWN;
    }
}
