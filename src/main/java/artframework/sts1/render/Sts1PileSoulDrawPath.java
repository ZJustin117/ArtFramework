package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.AssetResolveResult;
import artframework.assets.ResourceIds;
import artframework.component.Rect;
import artframework.context.PileSoulView;
import artframework.sts1.assets.Sts1VanillaCatalog;
import artframework.sts1.backend.Sts1PileSoulProjection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resource-backed, non-authoritative overlay description for native card pile/soul observations. */
public final class Sts1PileSoulDrawPath {
    public static final class DrawItem {
        public final String id, kind, zone, resourceId, label;
        public final int count;
        public final Rect bounds;
        public final boolean visible, resourceFound;

        DrawItem(PileSoulView.Entry entry) {
            id = entry.id; kind = entry.kind; zone = entry.zone; count = entry.count;
            bounds = entry.bounds; visible = entry.visible; label = entry.label;
            resourceId = resourceOrFallback(entry.kind, entry.zone, entry.resourceId);
            AssetResolveResult resolved = resolve(resourceId);
            resourceFound = resolved != null && resolved.found;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("id", id); out.put("kind", kind); out.put("zone", zone);
            out.put("resourceId", resourceId); out.put("label", label);
            out.put("count", Integer.valueOf(count)); out.put("visible", Boolean.valueOf(visible));
            out.put("resourceFound", Boolean.valueOf(resourceFound));
            out.put("x", Float.valueOf(bounds.x)); out.put("y", Float.valueOf(bounds.y));
            out.put("w", Float.valueOf(bounds.width)); out.put("h", Float.valueOf(bounds.height));
            return out;
        }
    }

    private Sts1PileSoulDrawPath() {}

    public static List<DrawItem> buildFromProjection() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        for (PileSoulView.Entry entry : Sts1PileSoulProjection.current().entries) {
            out.add(new DrawItem(entry));
        }
        return out;
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("available", Boolean.valueOf(Sts1PileSoulProjection.current().available));
        out.put("entryCount", Integer.valueOf(items.size()));
        out.put("nativeContinuation", Boolean.TRUE);
        out.put("nativePixelsSuppressed", Boolean.FALSE);
        out.put("cardPixelsNative", Boolean.TRUE);
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem item : items) list.add(item.toMap());
        out.put("items", list);
        return out;
    }

    private static AssetResolveResult resolve(String resourceId) {
        if (resourceId == null || resourceId.isEmpty()) return null;
        try { return ArtFramework.assets().resolve(resourceId); } catch (Throwable ignored) { return null; }
    }

    static String resourceOrFallback(String kind, String zone, String resourceId) {
        if (Sts1VanillaCatalog.isKnown(resourceId) && !isCardArt(resourceId)) return resourceId;
        if ("soul".equals(kind)) return ResourceIds.UI_SOUL_UNKNOWN;
        if ("draw".equals(zone)) return ResourceIds.UI_PILE_DRAW;
        if ("discard".equals(zone)) return ResourceIds.UI_PILE_DISCARD;
        if ("exhaust".equals(zone)) return ResourceIds.UI_PILE_EXHAUST;
        if ("deck".equals(zone)) return ResourceIds.UI_PILE_DECK;
        return ResourceIds.UI_CARD_GROUP_UNKNOWN;
    }

    private static boolean isCardArt(String resourceId) {
        return resourceId != null && resourceId.startsWith(ResourceIds.CARD_ART_PREFIX);
    }
}
