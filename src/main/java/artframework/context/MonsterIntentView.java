package artframework.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Combat monster intent row (observe-first). */
public final class MonsterIntentView {

    public final List<IntentEntry> entries;
    public final boolean available;

    public MonsterIntentView(List<IntentEntry> entries, boolean available) {
        if (entries == null || entries.isEmpty()) {
            this.entries = Collections.emptyList();
        } else {
            this.entries = Collections.unmodifiableList(new ArrayList<IntentEntry>(entries));
        }
        this.available = available;
    }

    public static MonsterIntentView empty() {
        return new MonsterIntentView(null, false);
    }

    public static MonsterIntentView of(List<IntentEntry> entries) {
        return new MonsterIntentView(entries, entries != null && !entries.isEmpty());
    }

    public int entryCount() {
        return entries.size();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("available", Boolean.valueOf(available));
        m.put("entryCount", Integer.valueOf(entries.size()));
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (IntentEntry e : entries) {
            list.add(e.toMap());
        }
        m.put("entries", list);
        return m;
    }

    public static final class IntentEntry {
        public final String monsterId;
        public final String monsterName;
        public final String intentType;
        public final String iconResourceId;
        public final int multiAmount;
        public final float x;
        public final float y;

        public IntentEntry(
                String monsterId,
                String monsterName,
                String intentType,
                String iconResourceId,
                int multiAmount,
                float x,
                float y) {
            this.monsterId = monsterId != null ? monsterId : "";
            this.monsterName = monsterName != null ? monsterName : "";
            this.intentType = intentType != null ? intentType : "";
            this.iconResourceId = iconResourceId != null ? iconResourceId : "";
            this.multiAmount = multiAmount;
            this.x = x;
            this.y = y;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("monsterId", monsterId);
            m.put("monsterName", monsterName);
            m.put("intentType", intentType);
            m.put("iconResourceId", iconResourceId);
            m.put("multiAmount", Integer.valueOf(multiAmount));
            m.put("x", Float.valueOf(x));
            m.put("y", Float.valueOf(y));
            return m;
        }
    }
}
