package artframework.context;

import artframework.core.SignalDecision;
import artframework.core.SignalListener;
import artframework.core.TransientSignalPaths;
import artframework.core.TransientSignalRuntime;
import artframework.core.UiSignal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.PresentationWorld;

/** Consumes synchronous authority frames into the durable presentation projection. */
public final class AuthorityProjectionSystem implements EcsSystem {
    public AuthorityProjectionSystem(TransientSignalRuntime runtime) {
        if (runtime == null) throw new IllegalArgumentException("runtime required");
        runtime.connectConsumer(TransientSignalPaths.AUTHORITY_FRAME, new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                if (!"presentation-schedule".equals(signal.source)
                        || !(signal.payload instanceof ContextFrame)) {
                    return SignalDecision.stopRejected("invalid authority frame signal");
                }
                FrameDiff diff = PresentProjections.publishDirect((ContextFrame) signal.payload);
                Map<String, Object> metadata = new LinkedHashMap<String, Object>(signal.metadata);
                metadata.put("projectionApplied", Boolean.valueOf(diff.applied));
                metadata.put("projectionMessage", diff.message);
                return SignalDecision.replace(signal.replace(signal.payload, signal.source, metadata));
            }
        });
    }

    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        // Authority frames are delivered synchronously by the transient listener.
    }
}
