package artframework.core;

import artframework.presentation.ConnectionDeclarationsComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRuntime;
import artframework.ecs.EntityId;
import artframework.component.ImmutableUiValue;

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

    private static final Map<String, List<OwnedSubscription>> BY_WINDOW =
            new LinkedHashMap<String, List<OwnedSubscription>>();

    private NodeConnections() {}

    /** Rebuild disposable subscriptions from ECS declaration components. */
    public static void syncContext(PresentationContext context) {
        if (context == null) return;
        String windowId = PresentationRuntime.windowId(context);
        clearContext(context);
        UiActions.ensureBuiltins();
        for (EntityId entity : context.entities()) {
            wireEntity(context, entity);
        }
    }

    private static void wireEntity(final PresentationContext context, final EntityId owner) {
        ConnectionDeclarationsComponent declarations = PresentationRuntime.component(
                context, owner, ConnectionDeclarationsComponent.class);
        if (declarations == null) return;
        List<Map<String, Object>> specs = new ArrayList<Map<String, Object>>(declarations.connections);
        for (Map<String, Object> legacy : declarations.legacyTriggers) {
            Map<String, Object> norm = normalizeTrigger(context, owner, legacy);
            if (norm != null) specs.add(norm);
        }
        for (Map<String, Object> spec : specs) wireSpec(context, owner, spec);
    }

    private static Map<String, Object> normalizeTrigger(PresentationContext context, EntityId owner,
            Map<String, Object> declaration) {
        artframework.presentation.NodeIdentityComponent identity = PresentationRuntime.identity(context, owner);
        String source = stringVal(declaration.get("source"));
        String signal = stringVal(declaration.get("signal"));
        if (source.isEmpty() || signal.isEmpty()) return null;
        source = sourcePath(context, source, identity);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("match", SignalHub.name(PresentationRuntime.windowId(context), source, signal));
        String action = stringVal(declaration.get("action"));
        result.put("action", action.isEmpty() ? UiActions.PLAY : action);
        Map<String, Object> args = new LinkedHashMap<String, Object>();
        if (declaration.get("args") instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, Object> raw = (Map<String, Object>) declaration.get("args");
            args.putAll(raw);
        }
        String play = stringVal(declaration.get("play"));
        if (!play.isEmpty()) { args.put("name", play); args.put("play", play); }
        if (!args.containsKey("player") && identity != null && !identity.name.isEmpty()) args.put("player", identity.name);
        if (declaration.get("player") != null) args.put("player", String.valueOf(declaration.get("player")));
        result.put("args", args);
        return result;
    }

    private static void wireSpec(final PresentationContext context, final EntityId owner,
            Map<String, Object> spec) {
        final String actionId = stringVal(spec.get("action"));
        if (actionId.isEmpty()) return;
        if (!UiActions.contains(actionId)) throw new IllegalArgumentException("unknown ui action in connections: " + actionId);
        @SuppressWarnings("unchecked")
        final Map<String, Object> args = spec.get("args") instanceof Map
                ? ImmutableUiValue.copyMap((Map<String, Object>) spec.get("args"))
                : java.util.Collections.<String, Object>emptyMap();
        String match = stringVal(spec.get("match"));
        String pattern = stringVal(spec.get("match_pattern"));
        if (pattern.isEmpty()) pattern = stringVal(spec.get("matchPattern"));
        if (match.isEmpty() && pattern.isEmpty()) {
            String source = stringVal(spec.get("source"));
            String signal = stringVal(spec.get("signal"));
            source = sourcePath(context, source, PresentationRuntime.identity(context, owner));
            if (!source.isEmpty() && !signal.isEmpty()) {
                match = SignalHub.name(PresentationRuntime.windowId(context), source, signal);
            }
        }
        if (match.isEmpty() && pattern.isEmpty()) return;
        final SignalListener listener = new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal event) {
                if (payloadAllows(args, event)) UiActions.run(actionId,
                        new UiActionContext(context, owner, event, args));
                return SignalDecision.continueSignal();
            }
        };
        SignalSubscription sub = pattern.isEmpty()
                ? PresentationRuntime.connectBus(context, match, listener)
                : PresentationRuntime.connectBus(context, Pattern.compile(pattern), listener);
        track(context, PresentationRuntime.windowId(context), owner, sub);
    }

    public static void clearWindow(String windowId) {
        if (windowId == null) {
            return;
        }
        List<OwnedSubscription> list = BY_WINDOW.remove(windowId);
        if (list == null) {
            return;
        }
        for (OwnedSubscription owned : list) {
            if (owned != null && owned.subscription != null) {
                owned.subscription.disconnect();
            }
        }
    }

    public static void resetForTests() {
        for (List<OwnedSubscription> list : BY_WINDOW.values()) {
            for (OwnedSubscription owned : list) {
                if (owned != null && owned.subscription != null) {
                    owned.subscription.disconnect();
                }
            }
        }
        BY_WINDOW.clear();
    }

    public static int subscriptionCount(String windowId) {
        List<OwnedSubscription> list = BY_WINDOW.get(windowId);
        return list == null ? 0 : list.size();
    }

    /** Remove only declarations owned by one ECS entity. */
    public static void clearEntity(PresentationContext context, EntityId entity) {
        if (context == null || entity == null) return;
        String windowId = PresentationRuntime.windowId(context);
        List<OwnedSubscription> list = BY_WINDOW.get(windowId);
        if (list == null) return;
        for (OwnedSubscription owned : new ArrayList<OwnedSubscription>(list)) {
            if (owned.context == context && owned.owner.equals(entity)) {
                owned.subscription.disconnect();
                list.remove(owned);
            }
        }
        if (list.isEmpty()) BY_WINDOW.remove(windowId);
    }

    public static void clearContext(PresentationContext context) {
        if (context == null) return;
        String windowId = PresentationRuntime.windowId(context);
        List<OwnedSubscription> list = BY_WINDOW.get(windowId);
        if (list == null) return;
        for (OwnedSubscription owned : new ArrayList<OwnedSubscription>(list)) {
            if (owned.context == context) {
                owned.subscription.disconnect();
                list.remove(owned);
            }
        }
        if (list.isEmpty()) BY_WINDOW.remove(windowId);
    }

    private static final class OwnedSubscription {
        final PresentationContext context;
        final EntityId owner;
        final SignalSubscription subscription;

        OwnedSubscription(PresentationContext context, EntityId owner, SignalSubscription subscription) {
            this.context = context;
            this.owner = owner;
            this.subscription = subscription;
        }
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

    private static void track(PresentationContext context, String windowId, EntityId owner,
            SignalSubscription sub) {
        if (sub == null) {
            return;
        }
        List<OwnedSubscription> list = BY_WINDOW.get(windowId);
        if (list == null) {
            list = new ArrayList<OwnedSubscription>();
            BY_WINDOW.put(windowId, list);
        }
        list.add(new OwnedSubscription(context, owner, sub));
    }

    private static String stringVal(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private static String sourcePath(
            PresentationContext context,
            String source,
            artframework.presentation.NodeIdentityComponent ownerIdentity) {
        if (".".equals(source) || "self".equals(source)) {
            return ownerIdentity != null ? ownerIdentity.key.localId : "";
        }
        artframework.ecs.EntityId sourceEntity = PresentationRuntime.find(context, source);
        artframework.presentation.NodeIdentityComponent sourceIdentity =
                PresentationRuntime.identity(context, sourceEntity);
        return sourceIdentity != null ? sourceIdentity.key.localId : source;
    }

}
