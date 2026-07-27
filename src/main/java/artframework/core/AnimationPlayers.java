package artframework.core;

import artframework.component.ArtNodeTypes;
import artframework.component.UiNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Attaches {@link AnimationPlayer} instances to mounted animation_player nodes.
 */
public final class AnimationPlayers {

    private static final Map<String, AnimationPlayer> BY_KEY = new LinkedHashMap<String, AnimationPlayer>();

    private AnimationPlayers() {}

    public static void syncTree(UiTree tree) {
        if (tree == null) {
            return;
        }
        clearWindow(tree.windowId());
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

    private static void walk(UiTree tree, UiInstance inst) {
        if (inst == null) {
            return;
        }
        if (ArtNodeTypes.ANIMATION_PLAYER.equals(inst.type()) && !inst.id().isEmpty()) {
            AnimationPlayer player = new AnimationPlayer(inst);
            loadFromDecl(player, inst.decl());
            BY_KEY.put(key(tree.windowId(), inst.id()), player);
        }
        for (UiInstance c : inst.children()) {
            walk(tree, c);
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadFromDecl(AnimationPlayer player, UiNode decl) {
        Object raw = decl.props.get("animations");
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
                    float from = floatVal(tm.get("from"), 0f);
                    float to = floatVal(tm.get("to"), 1f);
                    tracks.add(new AnimationPlayer.Track(prop, from, to));
                }
            }
            player.register(new AnimationPlayer.Animation(name, target, duration, tracks));
        }
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
}
