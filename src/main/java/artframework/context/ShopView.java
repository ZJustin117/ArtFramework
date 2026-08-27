package artframework.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Merchant shop snapshot. */
public final class ShopView {

    public final int gold;
    public final List<ShopEntryView> entries;
    public final boolean purgeAvailable;
    public final int purgeCost;
    public final boolean available;

    public ShopView(
            int gold,
            List<ShopEntryView> entries,
            boolean purgeAvailable,
            int purgeCost,
            boolean available) {
        this.gold = gold;
        if (entries == null || entries.isEmpty()) {
            this.entries = Collections.emptyList();
        } else {
            this.entries = Collections.unmodifiableList(new ArrayList<ShopEntryView>(entries));
        }
        this.purgeAvailable = purgeAvailable;
        this.purgeCost = purgeCost;
        this.available = available;
    }

    public static ShopView empty() {
        return new ShopView(0, null, false, 0, false);
    }

    public static ShopView of(int gold, List<ShopEntryView> entries, boolean purge, int purgeCost) {
        return new ShopView(gold, entries, purge, purgeCost, true);
    }

    public int entryCount() {
        return entries.size();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("gold", Integer.valueOf(gold));
        m.put("purgeAvailable", Boolean.valueOf(purgeAvailable));
        m.put("purgeCost", Integer.valueOf(purgeCost));
        m.put("available", Boolean.valueOf(available));
        m.put("entryCount", Integer.valueOf(entries.size()));
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (ShopEntryView e : entries) {
            list.add(e.toMap());
        }
        m.put("entries", list);
        return m;
    }

    public static final class ShopEntryView {
        public final int index;
        public final String kind;
        public final String label;
        public final int cost;
        public final boolean soldOut;
        public final String resourceId;

        public ShopEntryView(
                int index, String kind, String label, int cost, boolean soldOut, String resourceId) {
            this.index = index;
            this.kind = kind != null ? kind : "";
            this.label = label != null ? label : "";
            this.cost = cost;
            this.soldOut = soldOut;
            this.resourceId = resourceId != null ? resourceId : "";
        }

        public static ShopEntryView of(int index, String kind, String label, int cost) {
            return new ShopEntryView(index, kind, label, cost, false, "");
        }

        public static ShopEntryView of(int index, String kind, String label, int cost,
                boolean soldOut, String resourceId) {
            return new ShopEntryView(index, kind, label, cost, soldOut, resourceId);
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
}
