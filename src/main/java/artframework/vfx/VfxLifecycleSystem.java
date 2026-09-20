package artframework.vfx;

import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

import java.util.ArrayList;
import java.util.List;

/** Removes expired particles and complete, invalidated, or explicitly cleaned VFX graphs. */
public final class VfxLifecycleSystem implements EcsSystem {
    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        for (EntityId entity : world.query(VfxParticleBufferComponent.class)) {
            List<VfxParticle> alive = new ArrayList<VfxParticle>();
            for (VfxParticle particle : world.get(entity, VfxParticleBufferComponent.class).particles) {
                if (particle.age < particle.lifetime) alive.add(particle);
            }
            world.put(entity, VfxParticleBufferComponent.class, new VfxParticleBufferComponent(alive));
        }

        for (EntityId root : world.query(VfxSceneRuntimeComponent.class, VfxLifecycleComponent.class)) {
            if (!world.contains(root)) continue;
            VfxSceneRuntimeComponent runtime = world.get(root, VfxSceneRuntimeComponent.class);
            VfxLifecycleComponent lifecycle = world.get(root, VfxLifecycleComponent.class).advance(tick.deltaSeconds);
            world.put(root, VfxLifecycleComponent.class, lifecycle);
            boolean invalid = runtime.cleanupRequested || runtime.currentEpoch != runtime.instantiatedEpoch;
            if (invalid || (lifecycle.ageSeconds >= lifecycle.sceneDuration && allEmittersComplete(world, root))) {
                destroyGraph(world, root);
            }
        }
    }

    private static boolean allEmittersComplete(PresentationWorld world, EntityId root) {
        for (EntityId entity : world.query(VfxNodeComponent.class)) {
            VfxNodeComponent node = world.get(entity, VfxNodeComponent.class);
            if (!node.rootEntity.equals(root) || !world.has(entity, VfxEmitterStateComponent.class)) continue;
            VfxEmitterStateComponent state = world.get(entity, VfxEmitterStateComponent.class);
            VfxParticleBufferComponent particles = world.get(entity, VfxParticleBufferComponent.class);
            if (!state.emissionComplete || particles == null || !particles.particles.isEmpty()) return false;
        }
        return true;
    }

    private static void destroyGraph(PresentationWorld world, EntityId root) {
        List<EntityId> children = new ArrayList<EntityId>();
        for (EntityId entity : world.query(VfxNodeComponent.class)) {
            if (world.get(entity, VfxNodeComponent.class).rootEntity.equals(root)) children.add(entity);
        }
        for (EntityId child : children) world.destroyEntity(child);
        world.destroyEntity(root);
    }
}
