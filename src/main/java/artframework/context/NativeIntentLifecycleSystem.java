package artframework.context;

import artframework.core.SignalDecision;
import artframework.core.SignalListener;
import artframework.core.TransientSignalPaths;
import artframework.core.TransientSignalRuntime;
import artframework.core.UiSignal;
import artframework.ecs.EcsSystem;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;

import java.util.Map;

/** Consumes synchronous native lifecycle signals and applies durable observation transitions. */
public final class NativeIntentLifecycleSystem implements EcsSystem {
    public NativeIntentLifecycleSystem(TransientSignalRuntime runtime) {
        if (runtime == null) throw new IllegalArgumentException("runtime required");
        runtime.connectConsumer(TransientSignalPaths.NATIVE_INTENT_LIFECYCLE, new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) { return consume(signal); }
        });
    }

    private SignalDecision consume(UiSignal signal) {
        if (!"sts1-input".equals(signal.source) || !(signal.payload instanceof Map)) {
            return SignalDecision.stopRejected("invalid native lifecycle signal");
        }
        Map<?, ?> payload = (Map<?, ?>) signal.payload;
        Object surface = payload.get("surfaceId");
        Object name = payload.get("name");
        Object state = payload.get("state");
        Object message = payload.get("message");
        if (!(surface instanceof String) || !(name instanceof String) || !(state instanceof String)
                || !(message instanceof String) || payload.size() != 4) {
            return SignalDecision.stopRejected("invalid native lifecycle payload");
        }
        NativeIntentLifecycleComponent.State lifecycleState;
        try {
            lifecycleState = NativeIntentLifecycleComponent.State.valueOf((String) state);
        } catch (IllegalArgumentException invalidState) {
            return SignalDecision.stopRejected("invalid native lifecycle state");
        }
        PresentationContext context = PresentationRegistry.existingContext("sts1-input");
        if (context == null) return SignalDecision.stopRejected("native input context unavailable");
        PresentationKey key = new PresentationKey("sts1.input", (String) surface);
        EntityId entity = context.entity(key);
        if (entity == null) return SignalDecision.stopRejected("native input surface unavailable: " + surface);
        if (!context.world().contains(entity) || !key.equals(context.key(entity))) {
            return SignalDecision.stopRejected("native input identity mismatch");
        }
        writeLifecycle(context.world(), entity, (String) name, lifecycleState, (String) message);
        return SignalDecision.continueSignal();
    }

    private static void writeLifecycle(PresentationWorld world, EntityId entity, String name,
            NativeIntentLifecycleComponent.State state, String message) {
        if (state == NativeIntentLifecycleComponent.State.REQUESTED
                || state == NativeIntentLifecycleComponent.State.SENT) {
            world.remove(entity, NativeIntentObservationComponent.class);
        }
        world.put(entity, NativeIntentLifecycleComponent.class,
                new NativeIntentLifecycleComponent(name, state, message));
    }

    /** Retains durable observation-to-lifecycle transition logic. */
    @Override
    public void run(PresentationWorld world, artframework.ecs.EcsTick tick) {
        for (EntityId entity : world.query(
                NativeIntentLifecycleComponent.class, NativeIntentObservationComponent.class)) {
            NativeIntentLifecycleComponent lifecycle =
                    world.get(entity, NativeIntentLifecycleComponent.class);
            NativeIntentObservationComponent observation =
                    world.get(entity, NativeIntentObservationComponent.class);
            if (lifecycle.state == NativeIntentLifecycleComponent.State.EXECUTED) {
                world.put(entity, NativeIntentLifecycleComponent.class,
                        new NativeIntentLifecycleComponent(lifecycle.name,
                                observation.available
                                        ? NativeIntentLifecycleComponent.State.CONFIRMED
                                        : NativeIntentLifecycleComponent.State.FAILED,
                                observation.available
                                        ? "authority frame observed" : observation.message));
            }
        }
    }
}
