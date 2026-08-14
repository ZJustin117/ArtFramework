package artframework.context;

import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import java.util.List;

/** Stateless snapshot comparison system for native business intent confirmation. */
public final class BusinessConfirmationSystem implements EcsSystem {
    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        for (EntityId frameEntity : world.query(BusinessConfirmationFrameComponent.class)) {
            BusinessConfirmationFrameComponent frames =
                    world.get(frameEntity, BusinessConfirmationFrameComponent.class);
            for (EntityId entity : world.query(BusinessConfirmationComponent.class)) {
                BusinessConfirmationComponent request =
                        world.get(entity, BusinessConfirmationComponent.class);
                if (request.state == BusinessConfirmationComponent.State.PENDING) {
                    world.put(entity, BusinessConfirmationComponent.class,
                            evaluate(request, frames.before, frames.after));
                }
            }
            world.remove(frameEntity, BusinessConfirmationFrameComponent.class);
        }
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
