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
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem d : items) {
            list.add(d.toMap());
        }
        m.put("items", list);
        return m;
    }
}
