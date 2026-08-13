package artframework.c2;

import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Query-only immutable EntityPresent compatibility views derived from ECS slot components. */
public final class EntityPresentViews {
    private static final String CONTEXT_SCOPE = "c2-entity-present";

    private EntityPresentViews() {}

    public static List<EntitySlot> list() {
        PresentationContext context = PresentationRegistry.context(CONTEXT_SCOPE);
        PresentationWorld world = context.world();
        List<EntitySlot> result = new ArrayList<EntitySlot>();
        for (EntityId entity : world.query(EntitySlotIdentityComponent.class)) {
            EntitySlotIdentityComponent identity = world.get(entity, EntitySlotIdentityComponent.class);
            EntitySlotSnapshotComponent snapshot = world.get(entity, EntitySlotSnapshotComponent.class);
            EntitySlotTransformComponent transform = world.get(entity, EntitySlotTransformComponent.class);
            result.add(new EntitySlot(identity.slotId, identity.kind, identity.refId,
                    snapshot != null ? snapshot.snapshot : null,
                    transform != null ? transform.x : 0f,
                    transform != null ? transform.y : 0f,
                    transform != null ? transform.scale : 1f,
                    transform != null && transform.laidOut));
        }
        return Collections.unmodifiableList(result);
    }
}
