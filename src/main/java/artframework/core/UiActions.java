package artframework.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Global catalog of UI actions for declarative connections. Register ≠ invoke.
 */
public final class UiActions {

    public static final String PLAY = "play";
    public static final String PAUSE = "pause";
    public static final String STOP = "stop";
    public static final String RESUME = "resume";
    public static final String SET_PROP = "set_prop";
    public static final String PULSE_EFFECT = "pulse_effect";
    public static final String EMIT = "emit";
    public static final String CLOSE_WINDOW = "close_window";

    private static final Map<String, UiAction> BY_ID = new LinkedHashMap<String, UiAction>();
    private static boolean builtinsInstalled;

    private UiActions() {}

    public static void ensureBuiltins() {
        if (builtinsInstalled) {
            return;
        }
        builtinsInstalled = true;
        BuiltinUiActions.install();
    }

    public static void register(String id, UiAction action) {
        if (id == null || id.isEmpty() || action == null) {
            throw new IllegalArgumentException("id and action required");
        }
        ensureBuiltins();
        BY_ID.put(id.trim(), action);
    }

    public static UiAction get(String id) {
        ensureBuiltins();
        if (id == null || id.isEmpty()) {
            return null;
        }
        return BY_ID.get(id.trim());
    }

    public static boolean contains(String id) {
        return get(id) != null;
    }

    public static List<String> ids() {
        ensureBuiltins();
        return Collections.unmodifiableList(new ArrayList<String>(BY_ID.keySet()));
    }

    public static boolean run(String id, UiActionContext ctx) {
        UiAction action = get(id);
        if (action == null) {
            throw new IllegalArgumentException("unknown ui action: " + id);
        }
        return action.run(ctx);
    }

    public static void resetForTests() {
        BY_ID.clear();
        builtinsInstalled = false;
    }
}
