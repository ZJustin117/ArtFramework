package artframework.context;

import java.util.LinkedHashMap;
import java.util.Map;

/** Always-on dungeon top panel HUD. */
public final class TopPanelView {

    public final int hp;
    public final int maxHp;
    public final int gold;
    public final int floor;
    public final int ascension;
    public final String characterName;
    public final String statusText;
    public final boolean available;

    public TopPanelView(
            int hp,
            int maxHp,
            int gold,
            int floor,
            int ascension,
            String characterName,
            boolean available) {
        this(hp, maxHp, gold, floor, ascension, characterName, "", available);
    }

    public TopPanelView(
            int hp,
            int maxHp,
            int gold,
            int floor,
            int ascension,
            String characterName,
            String statusText,
            boolean available) {
        this.hp = hp;
        this.maxHp = maxHp;
        this.gold = gold;
        this.floor = floor;
        this.ascension = ascension;
        this.characterName = characterName != null ? characterName : "";
        this.statusText = statusText != null ? statusText : "";
        this.available = available;
    }

    public static TopPanelView empty() {
        return new TopPanelView(0, 0, 0, 0, 0, "", "", false);
    }

    public static TopPanelView of(
            int hp, int maxHp, int gold, int floor, int ascension, String characterName) {
        return new TopPanelView(hp, maxHp, gold, floor, ascension, characterName, "", true);
    }

    public static TopPanelView of(
            int hp,
            int maxHp,
            int gold,
            int floor,
            int ascension,
            String characterName,
            String statusText) {
        return new TopPanelView(hp, maxHp, gold, floor, ascension, characterName, statusText, true);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("hp", Integer.valueOf(hp));
        m.put("maxHp", Integer.valueOf(maxHp));
        m.put("gold", Integer.valueOf(gold));
        m.put("floor", Integer.valueOf(floor));
        m.put("ascension", Integer.valueOf(ascension));
        m.put("characterName", characterName);
        m.put("statusText", statusText);
        m.put("available", Boolean.valueOf(available));
        return m;
    }
}
