package artframework.context;

import artframework.component.ImmutableUiValue;
import artframework.component.MapNodeRef;
import artframework.core.SignalDecision;
import artframework.core.SignalDispatchResult;
import artframework.core.SignalListener;
import artframework.core.TransientSignalPaths;
import artframework.core.TransientSignalRuntime;
import artframework.core.UiSignal;
import artframework.ecs.EntityId;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Consumes synchronous surface intent signals and records their immediate ECS result. */
public final class SurfaceIntentExecutionSystem {
    private static final String WIRE_TYPE = "__art_type";
    private static final String PRODUCER_SURFACE = "__producerSurfaceId";
    private static final Object NO_PRODUCER = new Object();
    private static final ThreadLocal<String> PRODUCER = new ThreadLocal<String>();
    private static final ThreadLocal<Deque<Object>> PRODUCER_PREVIOUS =
            new ThreadLocal<Deque<Object>>();

    /** Begins a producer scope, retaining the enclosing scope for nested dispatches. */
    public static String beginProducer(String surfaceId) {
        Deque<Object> previousValues = PRODUCER_PREVIOUS.get();
        if (previousValues == null) {
            previousValues = new LinkedList<Object>();
            PRODUCER_PREVIOUS.set(previousValues);
        }
        String previous = PRODUCER.get();
        previousValues.push(previous == null ? NO_PRODUCER : previous);
        PRODUCER.set(surfaceId);
        return previous;
    }

    /** Ends the innermost producer scope and restores its enclosing producer value. */
    public static void endProducer() {
        Deque<Object> previousValues = PRODUCER_PREVIOUS.get();
        if (previousValues == null || previousValues.isEmpty()) {
            PRODUCER.remove();
            PRODUCER_PREVIOUS.remove();
            return;
        }
        Object previousValue = previousValues.pop();
        String previous = previousValue == NO_PRODUCER ? null : (String) previousValue;
        restoreProducer(previous, previousValues);
    }

    /** Ends a producer scope using the value returned by its matching begin call. */
    public static void endProducer(String previous) {
        Deque<Object> previousValues = PRODUCER_PREVIOUS.get();
        if (previousValues != null && !previousValues.isEmpty()) previousValues.pop();
        restoreProducer(previous, previousValues);
    }

    private static void restoreProducer(String previous, Deque<Object> previousValues) {
        if (previous == null) PRODUCER.remove();
        else PRODUCER.set(previous);
        if (previousValues == null || previousValues.isEmpty()) PRODUCER_PREVIOUS.remove();
    }

    public SurfaceIntentExecutionSystem(TransientSignalRuntime runtime) {
        if (runtime == null) throw new IllegalArgumentException("runtime required");
        runtime.connectConsumer(TransientSignalPaths.SURFACE_INTENT, new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) { return consume(signal); }
        });
    }

    public static List<Object> normalizeArgs(Object... args) {
        List<Object> copy = new ArrayList<Object>();
        if (args != null) for (Object arg : args) copy.add(normalize(arg,
                Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>())));
        return Collections.unmodifiableList(copy);
    }

    private SignalDecision consume(UiSignal signal) {
        if (!"c2-surfaces".equals(signal.source) || !(signal.payload instanceof Map)) {
            return SignalDecision.stopRejected("invalid surface intent signal");
        }
        Map<?, ?> payload = (Map<?, ?>) signal.payload;
        Object surfaceValue = payload.get("surfaceId");
        Object nameValue = payload.get("name");
        Object argsValue = payload.get("args");
        Object producerValue = payload.get(PRODUCER_SURFACE);
        if (!(surfaceValue instanceof String) || !(nameValue instanceof String)
                || !(argsValue instanceof List)
                || (producerValue != null && !(producerValue instanceof String))
                || (payload.size() != 3 && payload.size() != 4)) {
            return SignalDecision.stopRejected("invalid surface intent payload");
        }
        String surfaceId = (String) surfaceValue;
        String producer = PRODUCER.get();
        if (producer == null && producerValue instanceof String) producer = (String) producerValue;
        if (producer != null && !surfaceId.equals(producer)) {
            return SignalDecision.stopRejected("surface replacement cannot retarget producer entity");
        }
        PresentationContext context = PresentationRegistry.existingContext("c2-surfaces");
        if (context == null) return SignalDecision.stopRejected("surface context unavailable");
        EntityId entity = context.entity(new PresentationKey("sts1.surface", surfaceId));
        if (entity == null) return SignalDecision.stopRejected("surface not mounted: " + surfaceId);
        SurfaceIdentityComponent identity = context.world().get(entity, SurfaceIdentityComponent.class);
        if (identity == null || !surfaceId.equals(identity.id)) {
            return SignalDecision.stopRejected("surface identity mismatch: " + surfaceId);
        }
        Object[] args = denormalizeArgs((List<?>) argsValue);
        SignalDispatchResult result = NativeOperationDispatcher.dispatch(UiIntent.of(
                (String) nameValue, surfaceId, args));
        IntentResult outcome = outcome(result);
        context.world().put(entity, SurfaceResultComponent.class,
                new SurfaceResultComponent(outcome.status, outcome.message));
        return SignalDecision.continueSignal();
    }

    private static IntentResult outcome(SignalDispatchResult result) {
        if (result == null) return IntentResult.rejected("no result");
        if (result.isRejected()) return IntentResult.rejected(result.message);
        if (result.message != null && result.message.startsWith("queued:")) {
            return IntentResult.queued(result.message);
        }
        return IntentResult.accepted(result.message != null ? result.message : "");
    }

    private static Object normalize(Object value, Set<Object> active) {
        if (value instanceof CardRef) {
            CardRef ref = (CardRef) value;
            Map<String, Object> wire = new LinkedHashMap<String, Object>();
            wire.put(WIRE_TYPE, "CardRef"); wire.put("instanceId", ref.instanceId); wire.put("cardId", ref.cardId);
            return wire;
        }
        if (value instanceof MapNodeRef) {
            MapNodeRef ref = (MapNodeRef) value;
            Map<String, Object> wire = new LinkedHashMap<String, Object>();
            wire.put(WIRE_TYPE, "MapNodeRef"); wire.put("row", Integer.valueOf(ref.row));
            wire.put("col", Integer.valueOf(ref.col)); wire.put("roomType", ref.roomType);
            return wire;
        }
        if (value instanceof Map) {
            Map<?, ?> source = (Map<?, ?>) value; enter(source, active);
            try {
                Map<String, Object> copy = new LinkedHashMap<String, Object>();
                if (source.containsKey(WIRE_TYPE)) throw new IllegalArgumentException("intent value key reserved: " + WIRE_TYPE);
                for (Map.Entry<?, ?> entry : source.entrySet()) {
                    if (!(entry.getKey() instanceof String)) throw new IllegalArgumentException("intent value map keys must be strings");
                    copy.put((String) entry.getKey(), normalize(entry.getValue(), active));
                }
                return copy;
            } finally { active.remove(source); }
        }
        if (value instanceof List) {
            List<?> source = (List<?>) value; enter(source, active);
            try { List<Object> copy = new ArrayList<Object>(); for (Object item : source) copy.add(normalize(item, active)); return copy; }
            finally { active.remove(source); }
        }
        return ImmutableUiValue.copy(value);
    }

    private static Object[] denormalizeArgs(List<?> values) {
        Object[] result = new Object[values.size()];
        for (int i = 0; i < result.length; i++) result[i] = denormalize(values.get(i));
        return result;
    }

    private static Object denormalize(Object value) {
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if ("CardRef".equals(map.get(WIRE_TYPE)) && map.get("instanceId") instanceof String && map.get("cardId") instanceof String) return new CardRef((String) map.get("instanceId"), (String) map.get("cardId"));
            if ("MapNodeRef".equals(map.get(WIRE_TYPE)) && map.get("row") instanceof Number && map.get("col") instanceof Number && map.get("roomType") instanceof String) return new MapNodeRef(((Number) map.get("row")).intValue(), ((Number) map.get("col")).intValue(), (String) map.get("roomType"));
            Map<String, Object> copy = new LinkedHashMap<String, Object>(); for (Map.Entry<?, ?> entry : map.entrySet()) copy.put((String) entry.getKey(), denormalize(entry.getValue())); return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List) { List<Object> copy = new ArrayList<Object>(); for (Object item : (List<?>) value) copy.add(denormalize(item)); return Collections.unmodifiableList(copy); }
        return value;
    }

    private static void enter(Object value, Set<Object> active) {
        if (!active.add(value)) throw new IllegalArgumentException("cyclic intent value");
    }
}
