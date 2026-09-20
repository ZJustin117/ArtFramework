package artframework.vfx;

import artframework.ecs.EntityId;

/** Stable source-node identity and hierarchy metadata. */
public final class VfxNodeComponent {
    public final String definitionNodeId;
    public final int definitionOrder;
    public final EntityId rootEntity;
    public final EntityId parentEntity;

    public VfxNodeComponent(String definitionNodeId, int definitionOrder,
            EntityId rootEntity, EntityId parentEntity) {
        if (definitionNodeId == null || definitionNodeId.isEmpty() || definitionOrder < 0
                || rootEntity == null || parentEntity == null) {
            throw new IllegalArgumentException("node identity, order, root, and parent required");
        }
        this.definitionNodeId = definitionNodeId;
        this.definitionOrder = definitionOrder;
        this.rootEntity = rootEntity;
        this.parentEntity = parentEntity;
    }
}
