package artframework.core;

import artframework.component.ArtNodeTypes;
import artframework.component.UiNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Attaches {@link AnimationPlayer} instances to mounted animation_player nodes.
 * Supports declarative {@code auto_play} and {@code triggers} on the player node.
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
            wireTriggers(tree, inst, player);
            maybeAutoPlay(inst, player);
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

    @SuppressWarnings("unchecked")
    private static void wireTriggers(final UiTree tree, UiInstance owner, final AnimationPlayer player) {
        Object raw = owner.prop("triggers");
        if (!(raw instanceof List)) {
            return;
        }
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<String, Object> m = (Map<String, Object>) item;
            String source = stringVal(m.get("source"));
            String signal = stringVal(m.get("signal"));
            final String play = stringVal(m.get("play"));
            if (source.isEmpty() || signal.isEmpty() || play.isEmpty()) {
                continue;
            }
            if (".".equals(source) || "self".equals(source)) {
                source = owner.id();
            }
            final String sourceId = source;
            if (!player.has(play)) {
                continue;
            }
            try {
                tree.connect(
                        sourceId,
                        signal,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                if (player.has(play)) {
                                    player.play(play);
                                }
                            }
                        });
            } catch (RuntimeException ignored) {
                // Undeclared signal on source — skip wiring
            }
        }
    }

    private static void maybeAutoPlay(UiInstance owner, AnimationPlayer player) {
        Object raw = owner.prop("auto_play");
        if (raw == null) {
            raw = owner.prop("autoPlay");
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
}
