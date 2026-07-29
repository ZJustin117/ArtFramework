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

    public TreasureView(
            boolean chestOpen,
            boolean canOpen,
            String relicLabel,
            String relicResourceId,
            boolean available) {
        this.chestOpen = chestOpen;
        this.canOpen = canOpen;
        this.relicLabel = relicLabel != null ? relicLabel : "";
        this.relicResourceId = relicResourceId != null ? relicResourceId : "";
        this.available = available;
    }

    public static TreasureView empty() {
        return new TreasureView(false, false, "", "", false);
    }

    public static TreasureView closed() {
        return new TreasureView(false, true, "", "", true);
    }

    public static TreasureView opened(String relicLabel, String relicResourceId) {
        return new TreasureView(true, false, relicLabel, relicResourceId, true);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("chestOpen", Boolean.valueOf(chestOpen));
        m.put("canOpen", Boolean.valueOf(canOpen));
        m.put("relicLabel", relicLabel);
        m.put("relicResourceId", relicResourceId);
        m.put("available", Boolean.valueOf(available));
        return m;
    }
}
