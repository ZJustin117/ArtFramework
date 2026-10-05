package artframework.vfx;

import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit definition-to-ECS instantiation boundary. No runtime authority is retained here. */
public final class VfxInstantiateSystem {
    public VfxInstance instantiate(PresentationWorld world, VfxSceneDefinition definition, long epoch) {
        return instantiate(world, definition, epoch, 0f, 0f);
    }

    /** Instantiates a scene with an ECS-owned placement for its synthetic root. */
    public VfxInstance instantiate(PresentationWorld world, VfxSceneDefinition definition, long epoch,
            float originX, float originY) {
        if (world == null || definition == null) throw new IllegalArgumentException("world and definition required");
        if (epoch < 0L) throw new IllegalArgumentException("epoch must be non-negative");
        if (Float.isNaN(originX) || Float.isInfinite(originX)
                || Float.isNaN(originY) || Float.isInfinite(originY)) {
            throw new IllegalArgumentException("origin must be finite");
        }
        validateOrder(definition);

        EntityId root = world.createEntity();
        world.put(root, VfxSceneRuntimeComponent.class,
                new VfxSceneRuntimeComponent(definition.id, epoch, epoch, false));
        world.put(root, VfxLifecycleComponent.class,
                new VfxLifecycleComponent(0f, definition.duration));
        world.put(root, VfxSceneResourcesComponent.class,
                new VfxSceneResourcesComponent(definition.resources));
        world.put(root, VfxTransformComponent.class,
                new VfxTransformComponent(new VfxVec2(originX, originY), new VfxVec2(1f, 1f),
                        0f, 0f, true));

        Map<String, EntityId> entitiesByNode = new LinkedHashMap<String, EntityId>();
        Map<String, Integer> triggerChildrenByParent = new LinkedHashMap<String, Integer>();
        Map<String, VfxNodeDefinition> definitionsByNodeId = new LinkedHashMap<String, VfxNodeDefinition>();
        for (int index = 0; index < definition.nodes.size(); index++) {
            VfxNodeDefinition node = definition.nodes.get(index);
            EntityId parent = node.parentId == null ? root : entitiesByNode.get(node.parentId);
            EntityId entity = world.createEntity();
            entitiesByNode.put(node.id, entity);
            definitionsByNodeId.put(node.id, node);
            world.put(entity, VfxNodeComponent.class, new VfxNodeComponent(node.id, index, root, parent));
            world.put(entity, VfxTransformComponent.class, VfxTransformComponent.from(node));
            if (node.particleEmitter != null) {
                world.put(entity, VfxEmitterComponent.class, VfxEmitterComponent.from(node.particleEmitter));
                // A restricted trigger child is seeded DORMANT; VfxSubEmitterSystem arms it later.
                boolean dormant = VfxNodeDefinition.TRIGGER_ON_PARENT_COMPLETE.equals(node.emissionTrigger);
                world.put(entity, VfxEmitterStateComponent.class, new VfxEmitterStateComponent(0, false, dormant));
                world.put(entity, VfxParticleBufferComponent.class, VfxParticleBufferComponent.empty());
                if (dormant) {
                    seedSubEmitterMarker(world, entity, node, definitionsByNodeId, entitiesByNode,
                            triggerChildrenByParent);
                }
            }
        }
        return new VfxInstance(root, epoch);
    }

    /**
     * Seeds the once-only dormant marker for an eligible trigger child. Enforces the restricted
     * bounds without throwing: the parent itself must not be a trigger child (depth limited to one)
     * and at most {@link VfxSubEmitterSystem#MAX_SUB_EMITTERS_PER_PARENT} trigger children per parent
     * are seeded. An ineligible trigger child is left permanently dormant with no marker.
     */
    private static void seedSubEmitterMarker(PresentationWorld world, EntityId entity, VfxNodeDefinition node,
            Map<String, VfxNodeDefinition> definitionsByNodeId, Map<String, EntityId> entitiesByNode,
            Map<String, Integer> triggerChildrenByParent) {
        if (node.parentId == null) return;
        VfxNodeDefinition parentDefinition = definitionsByNodeId.get(node.parentId);
        if (parentDefinition == null
                || VfxNodeDefinition.TRIGGER_ON_PARENT_COMPLETE.equals(parentDefinition.emissionTrigger)) {
            return;
        }
        int count = triggerChildrenByParent.containsKey(node.parentId)
                ? triggerChildrenByParent.get(node.parentId).intValue() : 0;
        if (count >= VfxSubEmitterSystem.MAX_SUB_EMITTERS_PER_PARENT) return;
        EntityId parentEntity = entitiesByNode.get(node.parentId);
        if (parentEntity == null) return;
        world.put(entity, VfxSubEmitterSystem.VfxSubEmitterComponent.class,
                new VfxSubEmitterSystem.VfxSubEmitterComponent(parentEntity));
        triggerChildrenByParent.put(node.parentId, Integer.valueOf(count + 1));
    }

    private static void validateOrder(VfxSceneDefinition definition) {
        Map<String, Boolean> seen = new LinkedHashMap<String, Boolean>();
        for (VfxNodeDefinition node : definition.nodes) {
            if (node == null || node.id == null || node.id.isEmpty() || seen.containsKey(node.id)) {
                throw new IllegalArgumentException("definition node ids must be non-empty and unique");
            }
            if (node.parentId != null && !seen.containsKey(node.parentId)) {
                throw new IllegalArgumentException("parent must precede child: " + node.id);
            }
            seen.put(node.id, Boolean.TRUE);
        }
    }
}
