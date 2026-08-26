package artframework.sts1.render;

import artframework.api.ArtFramework;
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

        public DrawItem(int index, String kind, String label, int cost, boolean soldOut) {
            this.index = index;
            this.kind = kind != null ? kind : "";
            this.label = label != null ? label : "";
            this.cost = cost;
            this.soldOut = soldOut;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("index", Integer.valueOf(index));
            m.put("kind", kind);
            m.put("label", label);
            m.put("cost", Integer.valueOf(cost));
            m.put("soldOut", Boolean.valueOf(soldOut));
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
            out.add(new DrawItem(e.index, e.kind, e.label, e.cost, e.soldOut));
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
        out.add(new RoomChromeLine("title", "Merchant", true));
        out.add(new RoomChromeLine("gold", "Gold " + shop.gold, true));
        for (DrawItem item : buildFromProjection()) {
            out.add(new RoomChromeLine(
                    "entry:" + item.index,
                    item.label + "  " + item.cost + "G",
                    !item.soldOut));
        }
        if (shop.purgeAvailable) {
            out.add(new RoomChromeLine("purge", "Card Removal  " + shop.purgeCost + "G", true));
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
        m.put("drawCount", Integer.valueOf(items.size()));
        // Slice C phase 2: real ART-painted chrome rows (additive probe field).
        m.put("chromeLineCount", Integer.valueOf(chromeLines().size()));
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem d : items) {
            list.add(d.toMap());
        }
        m.put("items", list);
        return m;
    }
}
