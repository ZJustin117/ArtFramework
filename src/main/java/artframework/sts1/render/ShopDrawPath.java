package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.context.ShopView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shop full-present draw description (25.7). */
public final class ShopDrawPath {

    public static final class DrawItem {
        public final int index;
        public final String kind;
        public final String label;
        public final int cost;
        public final boolean soldOut;
        public final String resourceId;

        public DrawItem(int index, String kind, String label, int cost, boolean soldOut,
                String resourceId) {
            this.index = index;
            this.kind = kind != null ? kind : "";
            this.label = label != null ? label : "";
            this.cost = cost;
            this.soldOut = soldOut;
            this.resourceId = resourceId != null ? resourceId : "";
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("index", Integer.valueOf(index));
            m.put("kind", kind);
            m.put("label", label);
            m.put("cost", Integer.valueOf(cost));
            m.put("soldOut", Boolean.valueOf(soldOut));
            m.put("resourceId", resourceId);
            return m;
        }
    }

    private ShopDrawPath() {}

    public static boolean shouldSuppressNativeShop() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.SHOP);
    }

    public static List<DrawItem> buildFromProjection() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        for (ShopView.ShopEntryView e : ArtFramework.projection().shop().entries) {
            out.add(new DrawItem(e.index, e.kind, e.label, e.cost, e.soldOut,
                    resourceFor(e.kind, e.label, e.resourceId, e.soldOut)));
        }
        return out;
    }

    /**
     * Pure projection of the current ShopView into paintable rows: title, gold, one row per
     * entry and the purge service when available. Empty while the view is unavailable so the
     * renderer never invents pixels. Entry prices stay whatever ShopView carries (G4 full
     * projection is a later slice); no placeholder data is added here.
     */
    public static List<RoomChromeLine> chromeLines() {
        List<RoomChromeLine> out = new ArrayList<RoomChromeLine>();
        ShopView shop = ArtFramework.projection().shop();
        if (!shop.available) {
            return out;
        }
        out.add(line("title", "Merchant", true, "shop-merchant",
                ResourceIds.UI_SHOP_MERCHANT, 0));
        out.add(line("gold", "Gold " + shop.gold, true, "shop-gold",
                ResourceIds.UI_SHOP_GOLD, 1));
        int row = 2;
        for (DrawItem item : buildFromProjection()) {
            out.add(line(
                    "entry:" + item.index,
                    item.label + "  " + item.cost + "G",
                    !item.soldOut,
                    "shop-entry",
                    item.resourceId,
                    row++));
        }
        if (shop.purgeAvailable) {
            out.add(line("purge", "Card Removal  " + shop.purgeCost + "G", true,
                    "shop-purge", ResourceIds.UI_SHOP_PURGE, row));
        }
        return out;
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        ShopView sv = ArtFramework.projection().shop();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("count", Integer.valueOf(items.size()));
        m.put("gold", Integer.valueOf(sv.gold));
        m.put("purgeAvailable", Boolean.valueOf(sv.purgeAvailable));
        m.put("purgeCost", Integer.valueOf(sv.purgeCost));
        m.put("available", Boolean.valueOf(sv.available));
        m.put("suppressNativeShop", Boolean.valueOf(shouldSuppressNativeShop()));
        m.put("presentLevel", FullPresentMode.shopLevel().name());
        artframework.sts1.FullPresentCapability cap =
                artframework.sts1.input.CombatInputRouter.capability(SurfaceIds.SHOP);
        m.put("capability", cap.state.name());
        m.put("capabilityReason", cap.reason);
        m.put("drawCount", Integer.valueOf(chromeLines().size()));
        // Slice C phase 2: real ART-painted chrome rows (additive probe field).
        m.put("chromeLineCount", Integer.valueOf(chromeLines().size()));
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem d : items) {
            list.add(d.toMap());
        }
        m.put("items", list);
        return m;
    }

    private static RoomChromeLine line(String id, String text, boolean enabled, String role,
            String resourceId, int row) {
        float x = defaultX();
        float y = defaultY(row);
        float w = "title".equals(id) ? 420f : 360f;
        float h = 40f;
        return new RoomChromeLine(id, text, enabled, true, role, resourceId,
                x - w / 2f, y - h / 2f, w, h);
    }

    private static float defaultX() {
        try { return com.megacrit.cardcrawl.core.Settings.WIDTH * 0.5f; }
        catch (Throwable t) { return 960f; }
    }

    private static float defaultY(int row) {
        try { return com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.66f - row * 40f; }
        catch (Throwable t) { return 712.8f - row * 40f; }
    }

    public static String resourceFor(String kind, String label, String resourceId, boolean soldOut) {
        if (soldOut) return ResourceIds.UI_SHOP_SOLD_OUT;
        if (catalogKnows(resourceId)) return resourceId;
        if ("card".equals(kind) && catalogKnows(ResourceIds.cardArt(label))) {
            return ResourceIds.cardArt(label);
        }
        if ("card".equals(kind)) return ResourceIds.UI_REWARD_CARD;
        if ("relic".equals(kind)) return ResourceIds.UI_REWARD_RELIC;
        if ("potion".equals(kind)) return ResourceIds.UI_SHOP_ENTRY_PANEL;
        if ("gold".equals(kind)) return ResourceIds.UI_SHOP_GOLD;
        if ("purge".equals(kind)) return ResourceIds.UI_SHOP_PURGE;
        return ResourceIds.UI_SHOP_ENTRY_PANEL;
    }

    private static boolean catalogKnows(String resourceId) {
        return resourceId != null && !resourceId.isEmpty()
                && artframework.sts1.assets.Sts1VanillaCatalog.isKnown(resourceId);
    }
}
