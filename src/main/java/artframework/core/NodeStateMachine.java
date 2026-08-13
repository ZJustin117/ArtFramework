package artframework.core;

import artframework.presentation.Node;
import artframework.presentation.NodeTree;
import artframework.presentation.NodeStateComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Thin finite-state machine per node. Transitions fire on bus signal match (exact or regex);
 * enter actions are registered {@link UiAction}s only — no scripts.
 */
public final class NodeStateMachine {

    public static final String STATE_IDLE = "idle";
    public static final String STATE_PLAYING = "playing";
    public static final String STATE_PAUSED = "paused";

    public static final class Transition {
        public final String from;
        public final String to;
        public final String match;
        public final Pattern matchPattern;
        public final List<Map<String, Object>> onEnter;

        Transition(
                String from,
                String to,
                String match,
                Pattern matchPattern,
                List<Map<String, Object>> onEnter) {
            this.from = from != null ? from : "*";
            this.to = to != null ? to : "";
            this.match = match != null ? match : "";
            this.matchPattern = matchPattern;
            this.onEnter =
                    onEnter != null
                            ? Collections.unmodifiableList(new ArrayList<Map<String, Object>>(onEnter))
                            : Collections.<Map<String, Object>>emptyList();
        }

        boolean matchesSignal(String signalName) {
            if (signalName == null) {
                return false;
            }
            if (matchPattern != null) {
                return matchPattern.matcher(signalName).matches();
            }
            return !match.isEmpty() && match.equals(signalName);
        }

        boolean fromAllows(String state) {
            return "*".equals(from) || (state != null && from.equals(state));
        }
    }

    private final Node owner;
    private final List<Transition> transitions = new ArrayList<Transition>();
    private final List<SignalSubscription> subscriptions = new ArrayList<SignalSubscription>();
    private final Map<String, List<Map<String, Object>>> enterByState =
            new LinkedHashMap<String, List<Map<String, Object>>>();

    public NodeStateMachine(Node owner, String initial) {
        if (owner == null) {
            throw new IllegalArgumentException("owner required");
        }
        this.owner = owner;
        owner.tree().world().put(owner.entityId(), NodeStateComponent.class,
                new NodeStateComponent(initial != null && !initial.isEmpty() ? initial : STATE_IDLE));
    }

    public String state() {
        NodeStateComponent component = owner.tree().world()
                .get(owner.entityId(), NodeStateComponent.class);
        return component != null ? component.value : STATE_IDLE;
    }

    public Node owner() {
        return owner;
    }

    public void addTransition(Transition t) {
        if (t == null || t.to.isEmpty()) {
            return;
        }
        transitions.add(t);
    }

    public void setEnterActions(String stateName, List<Map<String, Object>> actions) {
        if (stateName == null || stateName.isEmpty()) {
            return;
        }
        if (actions == null || actions.isEmpty()) {
            enterByState.remove(stateName);
        } else {
            enterByState.put(
                    stateName, Collections.unmodifiableList(new ArrayList<Map<String, Object>>(actions)));
        }
    }

    /** Force state without signal (AnimationPlayer internal). */
    public void setState(String next, boolean emitChanged) {
        if (next == null || next.isEmpty() || next.equals(state())) {
            return;
        }
        putState(next);
        runEnter(next, null);
        if (emitChanged && owner.declaresSignal(SignalNames.STATE_CHANGED)) {
            try {
                owner.emitSignal(SignalNames.STATE_CHANGED, next);
            } catch (RuntimeException ignored) {
            }
        }
    }

    public void setState(String next) {
        setState(next, true);
    }

    public void wire(NodeTree tree) {
        clearSubscriptions();
        if (tree == null) {
            return;
        }
        for (final Transition t : transitions) {
            if (t.match.isEmpty() && t.matchPattern == null) {
                continue;
            }
            SignalListener listener =
                    new SignalListener() {
                        @Override
                        public SignalDecision onSignal(UiSignal event) {
                            if (t.fromAllows(state()) && t.matchesSignal(event.name)) {
                                applyTransition(t, event);
                            }
                            return SignalDecision.continueSignal();
                        }
                    };
            SignalSubscription sub;
            if (t.matchPattern != null) {
                sub = tree.connectBus(t.matchPattern, listener);
            } else {
                sub = tree.connectBus(t.match, listener);
            }
            subscriptions.add(sub);
        }
    }

    public void clearSubscriptions() {
        for (SignalSubscription s : subscriptions) {
            if (s != null) {
                s.disconnect();
            }
        }
        subscriptions.clear();
    }

    private void applyTransition(Transition t, UiSignal event) {
        if (t.to.equals(state())) {
            runActionList(t.onEnter, event);
            return;
        }
        putState(t.to);
        runEnter(t.to, event);
        runActionList(t.onEnter, event);
        if (owner.declaresSignal(SignalNames.STATE_CHANGED)) {
            try {
                owner.emitSignal(SignalNames.STATE_CHANGED, t.to);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void runEnter(String stateName, UiSignal event) {
        List<Map<String, Object>> actions = enterByState.get(stateName);
        runActionList(actions, event);
    }

    private void putState(String next) {
        owner.tree().world().put(owner.entityId(), NodeStateComponent.class,
                new NodeStateComponent(next));
    }

    private void runActionList(List<Map<String, Object>> actions, UiSignal event) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        NodeTree tree = owner.tree();
        for (Map<String, Object> spec : actions) {
            if (spec == null) {
                continue;
            }
            String actionId = stringVal(spec.get("action"));
            if (actionId.isEmpty() || !UiActions.contains(actionId)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> args =
                    spec.get("args") instanceof Map
                            ? new LinkedHashMap<String, Object>((Map<String, Object>) spec.get("args"))
                            : new LinkedHashMap<String, Object>();
            try {
                UiActions.run(actionId, new UiActionContext(tree, owner, event, args));
            } catch (RuntimeException ignored) {
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static NodeStateMachine fromDecl(Node owner) {
        if (owner == null) {
            return null;
        }
        Object raw = owner.get("states");
        if (!(raw instanceof Map)) {
            return null;
        }
        Map<String, Object> root = (Map<String, Object>) raw;
        String initial = stringVal(root.get("initial"));
        if (initial.isEmpty()) {
            initial = STATE_IDLE;
        }
        NodeStateMachine fsm = new NodeStateMachine(owner, initial);
        Object tr = root.get("transitions");
        if (tr instanceof List) {
            for (Object item : (List<?>) tr) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<String, Object> m = (Map<String, Object>) item;
                String from = stringVal(m.get("from"));
                if (from.isEmpty()) {
                    from = "*";
                }
                String to = stringVal(m.get("to"));
                if (to.isEmpty()) {
                    continue;
                }
                String match = stringVal(m.get("match"));
                String on = stringVal(m.get("on"));
                if (match.isEmpty() && !on.isEmpty()) {
                    match = on;
                }
                Pattern pat = null;
                String mp = stringVal(m.get("match_pattern"));
                if (mp.isEmpty()) {
                    mp = stringVal(m.get("matchPattern"));
                }
                if (!mp.isEmpty()) {
                    try {
                        pat = Pattern.compile(mp);
                    } catch (PatternSyntaxException e) {
                        throw new IllegalArgumentException("invalid states match_pattern: " + mp);
                    }
                }
                List<Map<String, Object>> onEnter = null;
                Object oe = m.get("on_enter");
                if (oe == null) {
                    oe = m.get("onEnter");
                }
                if (oe instanceof List) {
                    onEnter = new ArrayList<Map<String, Object>>();
                    for (Object a : (List<?>) oe) {
                        if (a instanceof Map) {
                            onEnter.add((Map<String, Object>) a);
                        }
                    }
                }
                fsm.addTransition(new Transition(from, to, match, pat, onEnter));
            }
        }
        Object enters = root.get("on_enter");
        if (enters == null) {
            enters = root.get("enter");
        }
        if (enters instanceof Map) {
            Map<String, Object> em = (Map<String, Object>) enters;
            for (Map.Entry<String, Object> e : em.entrySet()) {
                if (e.getValue() instanceof List) {
                    List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
                    for (Object a : (List<?>) e.getValue()) {
                        if (a instanceof Map) {
                            list.add((Map<String, Object>) a);
                        }
                    }
                    fsm.setEnterActions(e.getKey(), list);
                }
            }
        }
        return fsm;
    }

    private static String stringVal(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }
}
