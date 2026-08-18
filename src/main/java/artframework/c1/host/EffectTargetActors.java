package artframework.c1.host;

import com.badlogic.gdx.scenes.scene2d.Actor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps C1 effect target keys ({@code windowId + ":" + effectKey}) to live scene2d actors at
 * inflate time. StageHost syncs FX bounds from these actors — avoids findActor / LayoutEngine
 * Y-order mismatches (e.g. Hello label lightwave on the title row).
 */
public final class EffectTargetActors {

    private static final Map<String, Actor> BY_KEY = new LinkedHashMap<String, Actor>();

    private EffectTargetActors() {}

    public static String key(String windowId, String effectKey) {
        if (windowId == null || effectKey == null) {
            return "";
        }
        return windowId + ":" + effectKey;
    }

    public static void put(String windowId, String effectKey, Actor actor) {
        if (windowId == null || effectKey == null || effectKey.isEmpty() || actor == null) {
            return;
        }
        BY_KEY.put(key(windowId, effectKey), actor);
    }

    public static Actor get(String windowId, String effectKey) {
        if (windowId == null || effectKey == null) {
            return null;
        }
        return BY_KEY.get(key(windowId, effectKey));
    }

    public static Map<String, Actor> entriesForWindow(String windowId) {
        if (windowId == null || windowId.isEmpty()) {
            return Collections.emptyMap();
        }
        String prefix = windowId + ":";
        Map<String, Actor> out = new LinkedHashMap<String, Actor>();
        for (Map.Entry<String, Actor> e : BY_KEY.entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                out.put(e.getKey().substring(prefix.length()), e.getValue());
            }
        }
        return out;
    }

    public static void clearWindow(String windowId) {
        if (windowId == null) {
            return;
        }
        String prefix = windowId + ":";
        java.util.List<String> rm = new java.util.ArrayList<String>();
        for (String k : BY_KEY.keySet()) {
            if (k.startsWith(prefix)) {
                rm.add(k);
            }
        }
        for (String k : rm) {
            BY_KEY.remove(k);
        }
    }

    /** Clear all disposable scene2d bindings during Stage recreation. */
    public static void clearAll() {
        BY_KEY.clear();
    }

    public static void resetForTests() {
        BY_KEY.clear();
    }
}
