package artframework.context;

import artframework.core.SignalDecision;
import artframework.core.SignalListener;
import artframework.core.TransientSignalPaths;
import artframework.core.TransientSignalRuntime;
import artframework.core.UiSignal;
import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import java.util.List;
import java.util.Map;

/** Stateless snapshot comparison system for native business intent confirmation. */
public final class BusinessConfirmationSystem implements EcsSystem {
    public BusinessConfirmationSystem(TransientSignalRuntime runtime) {
        if (runtime == null) throw new IllegalArgumentException("runtime required");
        runtime.connectConsumer(TransientSignalPaths.AUTHORITY_BUSINESS_CONFIRMATION,
                new SignalListener() {
                    @Override public SignalDecision onSignal(UiSignal signal) {
                        return consume(signal);
                    }
                });
    }

    private SignalDecision consume(UiSignal signal) {
        if (!"presentation-schedule".equals(signal.source)
                || !(signal.payload instanceof Map)) {
            return SignalDecision.stopRejected("invalid business confirmation signal");
        }
        Map<?, ?> payload = (Map<?, ?>) signal.payload;
        Object before = payload.get("before");
        Object after = payload.get("after");
        if ((before != null && !(before instanceof ContextFrame))
                || !(after instanceof ContextFrame) || payload.size() != 2) {
            return SignalDecision.stopRejected("invalid business confirmation payload");
        }
        PresentationWorld world = artframework.ecs.ArtEcs.world();
        for (EntityId entity : world.query(BusinessConfirmationComponent.class)) {
            BusinessConfirmationComponent request = world.get(entity, BusinessConfirmationComponent.class);
            if (request.state == BusinessConfirmationComponent.State.PENDING) {
                world.put(entity, BusinessConfirmationComponent.class,
                        evaluate(request, (ContextFrame) before, (ContextFrame) after));
            }
        }
        return SignalDecision.continueSignal();
    }

    public BusinessConfirmationSystem() {
        // Compatibility constructor for isolated pure-ECS callers; no transient transport.
    }

    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        // Frame pairs arrive through the synchronous transient listener.
    }

    public static BusinessConfirmationComponent evaluate(
            BusinessConfirmationComponent request, ContextFrame before, ContextFrame after) {
        if (request == null || after == null || !after.available) {
            return request == null ? null : request.observe(
                    BusinessConfirmationComponent.State.FAILED,
                    after != null ? after.frameId : -1L,
                    after != null ? after.sceneEpoch : -1L,
                    "authority unavailable");
        }
        if (before == null || before.sceneEpoch != after.sceneEpoch) {
            return request.observe(BusinessConfirmationComponent.State.CONFIRMED,
                    after.frameId, after.sceneEpoch, "scene advanced");
        }
        boolean changed = changed(request.domain, before, after);
        return request.observe(changed ? BusinessConfirmationComponent.State.CONFIRMED
                        : BusinessConfirmationComponent.State.PENDING,
                after.frameId, after.sceneEpoch, changed ? evidence(request.domain) : "awaiting domain change");
    }

    private static boolean changed(BusinessConfirmationComponent.Domain domain,
            ContextFrame before, ContextFrame after) {
        switch (domain) {
            case CARD: return before.cards.size() != after.cards.size()
                    || !sameCardIds(before.cards, after.cards);
            case MAP: return before.mapView.nodes.size() != after.mapView.nodes.size()
                    || !before.mapView.toMap().equals(after.mapView.toMap());
            case EVENT: return before.eventView.options.size() != after.eventView.options.size();
            case REWARD: return before.rewardView.items.size() != after.rewardView.items.size();
            case SELECT: return before.selectView.pool.size() != after.selectView.pool.size()
                    || !before.selectView.selectedInstanceIds.equals(after.selectView.selectedInstanceIds);
            case ROOM: return !before.scene.equals(after.scene);
            default: return false;
        }
    }

    private static boolean sameCardIds(List<CardView> left, List<CardView> right) {
        if (left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); i++) {
            if (!left.get(i).ref.instanceId.equals(right.get(i).ref.instanceId)) return false;
        }
        return true;
    }

    private static String evidence(BusinessConfirmationComponent.Domain domain) {
        return domain.name().toLowerCase() + " snapshot changed";
    }
}
