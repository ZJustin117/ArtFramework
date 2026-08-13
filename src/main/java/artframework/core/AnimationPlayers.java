package artframework.core;

import artframework.component.ArtNodeTypes;
import artframework.component.UiNode;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRuntime;
import artframework.presentation.NodeIdentityComponent;
import artframework.ecs.EntityId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Attaches {@link AnimationPlayer} instances to mounted animation_player nodes.
 * Supports declarative {@code auto_play}; signal wiring via {@link NodeConnections}
 * ({@code connections} / legacy {@code triggers}).
 */
public final class AnimationPlayers {

    private static final Map<String, AnimationPlayer> BY_KEY = new LinkedHashMap<String, AnimationPlayer>();

    private AnimationPlayers() {}

    /** Build host-side player cache from ECS declaration entities. */
    public static void syncContext(PresentationContext context) {
        if (context == null) return;
        String windowId = PresentationRuntime.windowId(context);
        clearWindow(windowId);
        for (EntityId entity : context.entities()) {
            NodeIdentityComponent identity = PresentationRuntime.identity(context, entity);
            if (identity == null || !ArtNodeTypes.ANIMATION_PLAYER.equals(identity.type)
                    || identity.name.isEmpty()) continue;
            AnimationPlayer player = new AnimationPlayer(context, entity);
            loadFromProps(player, PresentationRuntime.property(context, entity, "animations"));
            BY_KEY.put(key(windowId, identity.name), player);
            maybeAutoPlay(PresentationRuntime.property(context, entity, "auto_play"),
                    PresentationRuntime.property(context, entity, "autoPlay"), player);
        }
    }

    public static AnimationPlayer get(String windowId, String nodeId) {
        if (windowId == null || nodeId == null) {
            return null;
        }
        return BY_KEY.get(key(windowId, nodeId));
    }

    public static void tick(String windowId, float deltaSeconds) {
        if (windowId == null) {
            return;
        }
        String prefix = windowId + "/";
        for (Map.Entry<String, AnimationPlayer> e :
                new ArrayList<Map.Entry<String, AnimationPlayer>>(BY_KEY.entrySet())) {
            if (e.getKey().startsWith(prefix)) {
                e.getValue().tick(deltaSeconds);
            }
        }
    }

    public static void clearWindow(String windowId) {
        if (windowId == null) {
            return;
        }
        String prefix = windowId + "/";
        List<String> remove = new ArrayList<String>();
        for (String k : BY_KEY.keySet()) {
            if (k.startsWith(prefix)) {
                remove.add(k);
            }
        }
        for (String k : remove) {
            BY_KEY.remove(k);
        }
    }

    public static void resetForTests() {
        BY_KEY.clear();
    }

    @SuppressWarnings("unchecked")
    private static void loadFromProps(AnimationPlayer player, Object raw) {
        if (!(raw instanceof List)) {
            return;
        }
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<String, Object> m = (Map<String, Object>) item;
            String name = stringVal(m.get("name"));
            if (name.isEmpty()) {
                continue;
            }
            String target = stringVal(m.get("target"));
            float duration = floatVal(m.get("duration"), 0.2f);
            String mode = stringVal(m.get("mode"));
            if (mode.isEmpty()) {
                mode = stringVal(m.get("playback"));
            }
            int loopCount = intVal(m.get("loop_count"), intVal(m.get("loopCount"), 0));
            List<AnimationPlayer.Track> tracks = new ArrayList<AnimationPlayer.Track>();
            Object tr = m.get("tracks");
            if (tr instanceof List) {
                for (Object tItem : (List<?>) tr) {
                    if (!(tItem instanceof Map)) {
                        continue;
                    }
                    Map<String, Object> tm = (Map<String, Object>) tItem;
                    String prop = stringVal(tm.get("property"));
                    if (prop.isEmpty()) {
                        prop = stringVal(tm.get("prop"));
                    }
                    if (prop.isEmpty()) {
                        continue;
                    }
                    boolean fromCurrent = false;
                    Object fromRaw = tm.get("from");
                    if (fromRaw != null
                            && ("current".equalsIgnoreCase(String.valueOf(fromRaw).trim())
                                    || "cur".equalsIgnoreCase(String.valueOf(fromRaw).trim()))) {
                        fromCurrent = true;
                    }
                    float from = fromCurrent ? 0f : floatVal(fromRaw, 0f);
                    float to = floatVal(tm.get("to"), 1f);
                    tracks.add(new AnimationPlayer.Track(prop, from, to, fromCurrent));
                }
            }
            if (mode.isEmpty()) {
                player.register(new AnimationPlayer.Animation(name, target, duration, tracks));
            } else {
                player.register(
                        new AnimationPlayer.Animation(
                                name, target, duration, tracks, mode, loopCount));
            }
        }
    }

    private static void maybeAutoPlay(Object raw, Object alternate, AnimationPlayer player) {
        if (raw == null) raw = alternate;
        if (raw == null) {
            return;
        }
        String name = stringVal(raw);
        if (name.isEmpty() || !player.has(name)) {
            return;
        }
        player.play(name);
    }

    private static String key(String windowId, String nodeId) {
        return windowId + "/" + nodeId;
    }

    private static String stringVal(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static float floatVal(Object v, float def) {
        if (v instanceof Number) {
            return ((Number) v).floatValue();
        }
        if (v instanceof String) {
            try {
                return Float.parseFloat(((String) v).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    private static int intVal(Object v, int def) {
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        if (v instanceof String) {
            try {
                return Integer.parseInt(((String) v).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

}
