package artframework.core;

import artframework.component.ImmutableUiValue;
import artframework.presentation.NodeStateComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRuntime;
import artframework.ecs.EntityId;

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
        this.onEnter = immutableActions(onEnter);
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

    private final PresentationContext context;
    private final EntityId ownerEntity;
    private final List<Transition> transitions = new ArrayList<Transition>();
    private final List<SignalSubscription> subscriptions = new ArrayList<SignalSubscription>();
    private final Map<String, List<Map<String, Object>>> enterByState =
            new LinkedHashMap<String, List<Map<String, Object>>>();

    public NodeStateMachine(PresentationContext context, EntityId owner, String initial) {
        if (context == null || owner == null) throw new IllegalArgumentException("context and owner required");
        this.context = context;
        this.ownerEntity = owner;
        context.world().put(owner, NodeStateComponent.class,
                new NodeStateComponent(initial != null && !initial.isEmpty() ? initial : STATE_IDLE));
    }

    public String state() {
        NodeStateComponent component = context.world().get(ownerEntity, NodeStateComponent.class);
        return component != null ? component.value : STATE_IDLE;
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
            enterByState.put(stateName, immutableActions(actions));
        }
    }

    private static List<Map<String, Object>> immutableActions(List<Map<String, Object>> actions) {
        if (actions == null || actions.isEmpty()) return Collections.emptyList();
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> action : actions) {
            if (action == null) {
                result.add(null);
            } else {
                @SuppressWarnings("unchecked")
                Map<String, Object> copy = (Map<String, Object>) ImmutableUiValue.copy(action);
                result.add(copy);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** Force state without signal (AnimationPlayer internal). */
    public void setState(String next) {
        if (next == null || next.isEmpty() || next.equals(state())) {
            return;
        }
        putState(next);
        runEnter(next, null);
    }

    public void wire(PresentationContext context) {
        clearSubscriptions();
        if (context == null) return;
        if (context != this.context) {
            throw new IllegalArgumentException("state machine context mismatch");
        }
        for (final Transition transition : transitions) {
            if (transition.match.isEmpty() && transition.matchPattern == null) continue;
            SignalListener listener = new SignalListener() {
                @Override public SignalDecision onSignal(UiSignal event) {
                    if (transition.fromAllows(state()) && transition.matchesSignal(event.name)) {
                        applyTransition(transition, event);
                    }
                    return SignalDecision.continueSignal();
                }
            };
            subscriptions.add(transition.matchPattern != null
                    ? PresentationRuntime.connectBus(context, transition.matchPattern, listener)
                    : PresentationRuntime.connectBus(context, transition.match, listener));
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
    }

    private void runEnter(String stateName, UiSignal event) {
        List<Map<String, Object>> actions = enterByState.get(stateName);
        runActionList(actions, event);
    }

    private void putState(String next) {
        context.world().put(ownerEntity, NodeStateComponent.class,
                new NodeStateComponent(next));
    }

    private void runActionList(List<Map<String, Object>> actions, UiSignal event) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
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
                UiActions.run(actionId, new UiActionContext(context, ownerEntity, event, args));
            } catch (RuntimeException ignored) {
            }
        }
    }

    private boolean declares(String signal) {
        artframework.presentation.SignalPortsComponent ports = PresentationRuntime.component(
                context, ownerEntity, artframework.presentation.SignalPortsComponent.class);
        return ports != null && ports.canEmit(signal);
    }

    /** ECS-native declaration parser for state-machine entities. */
    @SuppressWarnings("unchecked")
    public static NodeStateMachine fromDecl(PresentationContext context, EntityId owner) {
        Object raw = PresentationRuntime.property(context, owner, "states");
        if (!(raw instanceof Map)) return null;
        Map<String, Object> root = (Map<String, Object>) raw;
        String initial = stringVal(root.get("initial"));
        NodeStateMachine fsm = new NodeStateMachine(context, owner,
                initial.isEmpty() ? STATE_IDLE : initial);
        Object transitions = root.get("transitions");
        if (transitions instanceof List) {
            for (Object item : (List<?>) transitions) {
                if (!(item instanceof Map)) continue;
                Map<String, Object> spec = (Map<String, Object>) item;
                String to = stringVal(spec.get("to"));
                if (to.isEmpty()) continue;
                String from = stringVal(spec.get("from"));
                String match = stringVal(spec.get("match"));
                if (match.isEmpty()) match = stringVal(spec.get("on"));
                String expression = stringVal(spec.get("match_pattern"));
                if (expression.isEmpty()) expression = stringVal(spec.get("matchPattern"));
                Pattern pattern = expression.isEmpty() ? null : Pattern.compile(expression);
                List<Map<String, Object>> onEnter = new ArrayList<Map<String, Object>>();
                Object rawEnter = spec.get("on_enter") != null ? spec.get("on_enter") : spec.get("onEnter");
                if (rawEnter instanceof List) {
                    for (Object action : (List<?>) rawEnter) if (action instanceof Map) {
                        onEnter.add((Map<String, Object>) action);
                    }
                }
                fsm.addTransition(new Transition(from.isEmpty() ? "*" : from, to, match, pattern, onEnter));
            }
        }
        Object enter = root.get("on_enter") != null ? root.get("on_enter") : root.get("enter");
        if (enter instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) enter).entrySet()) {
                if (!(entry.getValue() instanceof List)) continue;
                List<Map<String, Object>> actions = new ArrayList<Map<String, Object>>();
                for (Object action : (List<?>) entry.getValue()) if (action instanceof Map) {
                    actions.add((Map<String, Object>) action);
                }
                fsm.setEnterActions(String.valueOf(entry.getKey()), actions);
            }
        }
        return fsm;
    }

    private static String stringVal(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }
}
