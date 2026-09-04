package artframework.context;

import artframework.core.SignalDecision;
import artframework.core.SignalListener;
import artframework.core.TransientSignalPaths;
import artframework.core.TransientSignalRuntime;
import artframework.core.UiSignal;
import artframework.ecs.EntityId;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;

import java.util.Map;

/** Consumes synchronous lifecycle signals while keeping mounted state ECS-owned. */
public final class SurfaceLifecycleSystem {
    public SurfaceLifecycleSystem(TransientSignalRuntime runtime) {
        if (runtime == null) throw new IllegalArgumentException("runtime required");
        runtime.connectConsumer(TransientSignalPaths.SURFACE_LIFECYCLE, new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                return consume(signal);
            }
        });
    }

    private SignalDecision consume(UiSignal signal) {
        if (!"c2-surfaces".equals(signal.source) || !(signal.payload instanceof Map)) {
            return SignalDecision.stopRejected("invalid surface lifecycle signal");
        }
        Map<?, ?> payload = (Map<?, ?>) signal.payload;
        Object idValue = payload.get("surfaceId");
        Object mountedValue = payload.get("mounted");
        if (!(idValue instanceof String) || !(mountedValue instanceof Boolean)
                || payload.size() != 2) {
            return SignalDecision.stopRejected("invalid surface lifecycle payload");
        }
        String id = (String) idValue;
        PresentationContext context = PresentationRegistry.existingContext("c2-surfaces");
        if (context == null) return SignalDecision.stopRejected("surface context unavailable");
        EntityId entity = context.entity(new PresentationKey("sts1.surface", id));
        if (entity == null) return SignalDecision.stopRejected("surface not mounted: " + id);
        SurfaceIdentityComponent identity = context.world().get(entity, SurfaceIdentityComponent.class);
        if (identity == null || !id.equals(identity.id)) {
            return SignalDecision.stopRejected("surface identity mismatch: " + id);
        }
        context.world().put(entity, SurfaceLifecycleComponent.class,
                new SurfaceLifecycleComponent(((Boolean) mountedValue).booleanValue()));
        return SignalDecision.continueSignal();
    }
}
