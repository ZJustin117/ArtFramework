package artframework.core;

import artframework.component.ArtNodeTypes;
import artframework.component.UiNode;
import artframework.presentation.Node;
import artframework.presentation.NodeTree;

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

    public static void syncTree(NodeTree tree) {
        if (tree == null) {
            return;
        }
        clearWindow(windowId(tree));
        walk(tree, tree.root());
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

    private static void walk(NodeTree tree, Node inst) {
        if (inst == null) {
            return;
        }
        if (ArtNodeTypes.ANIMATION_PLAYER.equals(inst.type()) && !inst.name().isEmpty()) {
            AnimationPlayer player = new AnimationPlayer(inst);
            loadFromProps(player, inst);
            BY_KEY.put(key(windowId(tree), inst.name()), player);
            maybeAutoPlay(inst, player);
        }
        for (Node c : inst.children()) {
            walk(tree, c);
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadFromProps(AnimationPlayer player, Node owner) {
        Object raw = owner.get("animations");
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

    private static void maybeAutoPlay(Node owner, AnimationPlayer player) {
        Object raw = owner.get("auto_play");
        if (raw == null) {
            raw = owner.get("autoPlay");
        }
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

    private static String windowId(NodeTree tree) {
        return tree.windowId();
    }
}
