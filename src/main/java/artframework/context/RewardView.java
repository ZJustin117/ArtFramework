package artframework.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Combat / card / boss-relic reward snapshot. {@code kind} is {@code combat}, {@code card}, or
 * {@code boss_relic}.
 */
public final class RewardView {

    public final String kind;
    public final String title;
    public final List<RewardItemView> items;
    public final boolean available;

    public RewardView(String kind, String title, List<RewardItemView> items, boolean available) {
        this.kind = kind != null ? kind : "";
        this.title = title != null ? title : "";
        if (items == null || items.isEmpty()) {
            this.items = Collections.emptyList();
        } else {
            this.items = Collections.unmodifiableList(new ArrayList<RewardItemView>(items));
        }
        this.available = available;
    }

    public static RewardView empty() {
        return new RewardView("", "", null, false);
    }

    public static RewardView of(String kind, String title, List<RewardItemView> items) {
        return new RewardView(kind, title, items, items != null && !items.isEmpty());
    }

    public int itemCount() {
        return items.size();
    }

    public RewardItemView find(int index) {
        for (RewardItemView i : items) {
            if (i.index == index) {
                return i;
            }
        }
        return null;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("kind", kind);
        m.put("title", title);
        m.put("available", Boolean.valueOf(available));
        m.put("itemCount", Integer.valueOf(items.size()));
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (RewardItemView i : items) {
            list.add(i.toMap());
        }
        m.put("items", list);
        return m;
    }
}
