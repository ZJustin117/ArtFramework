package artframework.context;

import java.util.LinkedHashMap;
import java.util.Map;

/** Treasure / chest room chrome. */
public final class TreasureView {

    public final boolean chestOpen;
    public final boolean canOpen;
    public final String relicLabel;
    public final String relicResourceId;
    public final boolean available;
    /**
     * NRO-04 D08: the ACTUAL {@code AbstractChest} sprite ResourceId for the live chest
     * ({@code ui.treasure.chest.<kind>[.opened]}), distinct from the mis-mapped
     * {@code UI_TREASURE_CHEST_CLOSED/OPEN} text-row ids. Empty when unavailable; the draw path
     * falls back to a medium CLOSED chest so it never invents a wrong kind.
     */
    public final String chestResourceId;

    public TreasureView(
            boolean chestOpen,
            boolean canOpen,
            String relicLabel,
            String relicResourceId,
            boolean available) {
        this(chestOpen, canOpen, relicLabel, relicResourceId, available, "");
    }

    public TreasureView(
            boolean chestOpen,
            boolean canOpen,
            String relicLabel,
            String relicResourceId,
            boolean available,
            String chestResourceId) {
        this.chestOpen = chestOpen;
        this.canOpen = canOpen;
        this.relicLabel = relicLabel != null ? relicLabel : "";
        this.relicResourceId = relicResourceId != null ? relicResourceId : "";
        this.available = available;
        this.chestResourceId = chestResourceId != null ? chestResourceId : "";
    }

    public static TreasureView empty() {
        return new TreasureView(false, false, "", "", false, "");
    }

    public static TreasureView closed() {
        return closed("");
    }

    public static TreasureView closed(String chestResourceId) {
        return new TreasureView(false, true, "", "", true, chestResourceId);
    }

    public static TreasureView opened(String relicLabel, String relicResourceId) {
        return opened(relicLabel, relicResourceId, "");
    }

    public static TreasureView opened(
            String relicLabel, String relicResourceId, String chestResourceId) {
        return new TreasureView(true, false, relicLabel, relicResourceId, true, chestResourceId);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("chestOpen", Boolean.valueOf(chestOpen));
        m.put("canOpen", Boolean.valueOf(canOpen));
        m.put("relicLabel", relicLabel);
        m.put("relicResourceId", relicResourceId);
        m.put("available", Boolean.valueOf(available));
        m.put("chestResourceId", chestResourceId);
        return m;
    }
}
