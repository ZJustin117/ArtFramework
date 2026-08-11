package artframework.core;

import artframework.presentation.Node;
import artframework.presentation.NodeTree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Wires declarative {@code connections} (and legacy {@code triggers}) to {@link SignalBus}
 * exact or regex subscriptions, invoking {@link UiActions}.
 */
public final class NodeConnections {

    private static final Map<String, List<SignalSubscription>> BY_WINDOW =
            new LinkedHashMap<String, List<SignalSubscription>>();

    private NodeConnections() {}

    public static void syncTree(NodeTree tree) {
        if (tree == null) {
            return;
        }
        clearWindow(windowId(tree));
        UiActions.ensureBuiltins();
        walk(tree, tree.root());
    }

    public static void clearWindow(String windowId) {
        if (windowId == null) {
            return;
        }
        List<SignalSubscription> list = BY_WINDOW.remove(windowId);
        if (list == null) {
            return;
        }
        for (SignalSubscription s : list) {
            if (s != null) {
                s.disconnect();
            }
        }
    }

    public static void resetForTests() {
        for (List<SignalSubscription> list : BY_WINDOW.values()) {
            for (SignalSubscription s : list) {
                if (s != null) {
                    s.disconnect();
                }
            }
        }
        BY_WINDOW.clear();
    }

    public static int subscriptionCount(String windowId) {
        List<SignalSubscription> list = BY_WINDOW.get(windowId);
        return list == null ? 0 : list.size();
    }

    private static void walk(NodeTree tree, Node inst) {
        if (inst == null) {
            return;
        }
        wireNode(tree, inst);
        for (Node c : inst.children()) {
            walk(tree, c);
        }
    }

    @SuppressWarnings("unchecked")
    private static void wireNode(final NodeTree tree, final Node owner) {
        List<Map<String, Object>> specs = new ArrayList<Map<String, Object>>();
        Object connections = owner.get("connections");
        if (connections instanceof List) {
            for (Object item : (List<?>) connections) {
                if (item instanceof Map) {
                    specs.add((Map<String, Object>) item);
                }
            }
        }
        Object triggers = owner.get("triggers");
        if (triggers instanceof List) {
            for (Object item : (List<?>) triggers) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<String, Object> legacy = (Map<String, Object>) item;
                Map<String, Object> norm = normalizeTrigger(owner, legacy);
                if (norm != null) {
                    specs.add(norm);
                }
            }
        }
        for (Map<String, Object> spec : specs) {
            wireSpec(tree, owner, spec);
        }
    }

    private static Map<String, Object> normalizeTrigger(Node owner, Map<String, Object> m) {
        String source = stringVal(m.get("source"));
        String signal = stringVal(m.get("signal"));
        String play = stringVal(m.get("play"));
        if (source.isEmpty() || signal.isEmpty()) {
            return null;
        }
        if (".".equals(source) || "self".equals(source)) {
            source = owner.name();
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("match", SignalHub.name(source, signal));
        String action = stringVal(m.get("action"));
        if (action.isEmpty()) {
            action = UiActions.PLAY;
        }
        out.put("action", action);
        Map<String, Object> args = new LinkedHashMap<String, Object>();
        if (m.get("args") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> rawArgs = (Map<String, Object>) m.get("args");
            args.putAll(rawArgs);
        }
        if (!play.isEmpty()) {
            args.put("name", play);
            args.put("play", play);
        }
        if (!args.containsKey("player") && !owner.name().isEmpty()) {
            args.put("player", owner.name());
        }
        Object player = m.get("player");
        if (player != null) {
            args.put("player", String.valueOf(player));
        }
        out.put("args", args);
        return out;
    }

    private static void wireSpec(
            final NodeTree tree, final Node owner, Map<String, Object> spec) {
        final String actionId = stringVal(spec.get("action"));
        if (actionId.isEmpty()) {
            return;
        }
        if (!UiActions.contains(actionId)) {
            throw new IllegalArgumentException("unknown ui action in connections: " + actionId);
        }
        @SuppressWarnings("unchecked")
        final Map<String, Object> args =
                spec.get("args") instanceof Map
                        ? new LinkedHashMap<String, Object>((Map<String, Object>) spec.get("args"))
                        : new LinkedHashMap<String, Object>();
        promoteArg(spec, args, "name");
        promoteArg(spec, args, "play");
        promoteArg(spec, args, "player");
        promoteArg(spec, args, "target");
        promoteArg(spec, args, "prop");
        promoteArg(spec, args, "property");
        promoteArg(spec, args, "effect");
        promoteArg(spec, args, "window");
        promoteArg(spec, args, "duration");
        promoteArg(spec, args, "mode");
        promoteArg(spec, args, "from_payload");
        promoteArg(spec, args, "value");
        promoteArg(spec, args, "fx");
        promoteArg(spec, args, "if_payload");
        promoteArg(spec, args, "ifPayload");
        promoteArg(spec, args, "from_slider");
        promoteArg(spec, args, "fromSlider");
        if (UiActions.EMIT.equals(actionId)) {
            promoteArg(spec, args, "signal");
        }

        String match = stringVal(spec.get("match"));
        String matchPattern = stringVal(spec.get("match_pattern"));
        if (matchPattern.isEmpty()) {
            matchPattern = stringVal(spec.get("matchPattern"));
        }
        // source+signal shorthand → exact bus name
        if (match.isEmpty() && matchPattern.isEmpty()) {
            String source = stringVal(spec.get("source"));
            String signal = stringVal(spec.get("signal"));
            if (!source.isEmpty() && !signal.isEmpty()) {
                if (".".equals(source) || "self".equals(source)) {
                    source = owner.name();
                }
                match = SignalHub.name(source, signal);
            }
        }
        if (match.isEmpty() && matchPattern.isEmpty()) {
            return;
        }

        final SignalListener listener =
                new SignalListener() {
                    @Override
                    public SignalDecision onSignal(UiSignal event) {
                        if (!payloadAllows(args, event)) {
                            return SignalDecision.continueSignal();
                        }
                        try {
                            UiActions.run(
                                    actionId, new UiActionContext(tree, owner, event, args));
                        } catch (RuntimeException ignored) {
                        }
                        return SignalDecision.continueSignal();
                    }
                };

        SignalSubscription sub;
        if (!matchPattern.isEmpty()) {
            try {
                Pattern p = Pattern.compile(matchPattern);
                sub = tree.connectBus(p, listener);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException(
                        "invalid match_pattern: " + matchPattern + " — " + e.getMessage());
            }
        } else {
            sub = tree.connectBus(match, listener);
        }
        track(windowId(tree), sub);
    }

    private static void promoteArg(
            Map<String, Object> spec, Map<String, Object> args, String key) {
        if (!args.containsKey(key) && spec.get(key) != null) {
            args.put(key, spec.get(key));
        }
    }

    /**
     * Optional {@code if_payload} / {@code ifPayload}: only run when the signal's first payload
     * element equals this string (e.g. animation name on {@code finished}).
     */
    private static boolean payloadAllows(Map<String, Object> args, UiSignal event) {
        if (args == null) {
            return true;
        }
        Object want = args.get("if_payload");
        if (want == null) {
            want = args.get("ifPayload");
        }
        if (want == null) {
            return true;
        }
        String expected = String.valueOf(want).trim();
        if (expected.isEmpty()) {
            return true;
        }
        Object got = null;
        if (event != null && event.payload != null) {
            if (event.payload instanceof Object[]) {
                Object[] arr = (Object[]) event.payload;
                if (arr.length > 0) {
                    got = arr[0];
                }
            } else {
                got = event.payload;
            }
        }
        return expected.equals(String.valueOf(got));
    }

    private static void track(String windowId, SignalSubscription sub) {
        if (sub == null) {
            return;
        }
        List<SignalSubscription> list = BY_WINDOW.get(windowId);
        if (list == null) {
            list = new ArrayList<SignalSubscription>();
            BY_WINDOW.put(windowId, list);
        }
        list.add(sub);
    }

    private static String stringVal(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private static String windowId(NodeTree tree) {
        return tree.context().world().scope().replace("tree:", "");
    }
}
