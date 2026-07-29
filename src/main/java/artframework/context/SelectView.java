package artframework.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable grid/hand select surface snapshot for one present frame. */
public final class SelectView {

    public static final String KIND_GRID = "GRID";
    public static final String KIND_HAND = "HAND";

    public final String kind;
    public final List<CardView> pool;
    public final List<String> selectedInstanceIds;
    public final boolean confirmEnabled;
    public final boolean confirmVisible;
    public final boolean available;

    public SelectView(
            String kind,
            List<CardView> pool,
            List<String> selectedInstanceIds,
            boolean confirmEnabled,
            boolean confirmVisible,
            boolean available) {
        this.kind = kind != null ? kind : "";
        if (pool == null || pool.isEmpty()) {
            this.pool = Collections.emptyList();
        } else {
            this.pool = Collections.unmodifiableList(new ArrayList<CardView>(pool));
        }
        if (selectedInstanceIds == null || selectedInstanceIds.isEmpty()) {
            this.selectedInstanceIds = Collections.emptyList();
        } else {
            this.selectedInstanceIds =
                    Collections.unmodifiableList(new ArrayList<String>(selectedInstanceIds));
        }
        this.confirmEnabled = confirmEnabled;
        this.confirmVisible = confirmVisible;
        this.available = available;
    }

    public static SelectView empty() {
        return new SelectView("", null, null, false, false, false);
    }

    public static SelectView grid(
            List<CardView> pool,
            List<String> selectedInstanceIds,
            boolean confirmEnabled,
            boolean confirmVisible) {
        boolean avail = pool != null && !pool.isEmpty();
        return new SelectView(
                KIND_GRID, pool, selectedInstanceIds, confirmEnabled, confirmVisible, avail);
    }

    public static SelectView hand(
            List<CardView> pool,
            List<String> selectedInstanceIds,
            boolean confirmEnabled,
            boolean confirmVisible) {
        boolean avail = pool != null && !pool.isEmpty();
        return new SelectView(
                KIND_HAND, pool, selectedInstanceIds, confirmEnabled, confirmVisible, avail);
    }

    public int poolCount() {
        return pool.size();
    }

    public boolean isSelected(String instanceId) {
        return instanceId != null && selectedInstanceIds.contains(instanceId);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("kind", kind);
        m.put("available", Boolean.valueOf(available));
        m.put("poolCount", Integer.valueOf(pool.size()));
        m.put("selectedCount", Integer.valueOf(selectedInstanceIds.size()));
        m.put("confirmEnabled", Boolean.valueOf(confirmEnabled));
        m.put("confirmVisible", Boolean.valueOf(confirmVisible));
        List<String> selected = new ArrayList<String>(selectedInstanceIds);
        m.put("selectedInstanceIds", selected);
        List<Map<String, Object>> cards = new ArrayList<Map<String, Object>>();
        for (CardView c : pool) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("instanceId", c.ref.instanceId);
            row.put("cardId", c.ref.cardId);
            row.put("slot", Integer.valueOf(c.slotIndex));
            row.put("selected", Boolean.valueOf(c.selected || isSelected(c.ref.instanceId)));
            cards.add(row);
        }
        m.put("pool", cards);
        return m;
    }
}
