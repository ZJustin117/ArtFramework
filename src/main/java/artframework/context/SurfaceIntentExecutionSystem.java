package artframework.context;

import artframework.core.SignalDispatchResult;
import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

/** Executes one-shot surface requests and records their immediate ECS result. */
public final class SurfaceIntentExecutionSystem implements EcsSystem {
    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        for (EntityId entity : world.query(SurfaceIntentExecutionComponent.class)) {
            SurfaceIntentExecutionComponent request =
                    world.get(entity, SurfaceIntentExecutionComponent.class);
            try {
                Object[] args = new Object[request.args.size()];
                for (int i = 0; i < args.length; i++) {
                    args[i] = SurfaceIntentExecutionComponent.denormalize(request.args.get(i));
                }
                SignalDispatchResult result = NativeOperationDispatcher.dispatch(UiIntent.of(
                        request.name, request.surfaceId, args));
                IntentResult outcome;
                if (result == null) {
                    outcome = IntentResult.rejected("no result");
                } else if (result.isRejected()) {
                    outcome = IntentResult.rejected(result.message);
                } else if (result.message != null && result.message.startsWith("queued:")) {
                    outcome = IntentResult.queued(result.message);
                } else {
                    outcome = IntentResult.accepted(result.message != null ? result.message : "");
                }
                world.put(entity, SurfaceResultComponent.class,
                        new SurfaceResultComponent(outcome.status, outcome.message));
            } finally {
                world.remove(entity, SurfaceIntentExecutionComponent.class);
            }
        }
    }
}
