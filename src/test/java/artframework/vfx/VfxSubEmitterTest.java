package artframework.vfx;

import artframework.ecs.EcsPipeline;
import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Restricted sub-emitter arming: one trigger level ({@code onParentComplete}), depth-1, fixed child
 * cap, pure (no GL). Unit-verified only — no D1 sub-emitter bundle exists (documented gap).
 */
public class VfxSubEmitterTest {
    private static final List<EcsSystem> SIMULATION = Arrays.<EcsSystem>asList(
            new VfxSubEmitterSystem(), new ParticleSpawnSystem(), new ParticleIntegrateSystem(),
            new ParticleCurveSystem(), new VfxLifecycleSystem());

    @Test
    public void dormantChildArmsExactlyOnceWhenParentCompletesAndDrains() {
        PresentationWorld world = subEmitterWorld(3f, 1, 2, 6L);
        EntityId child = emitterEntity(world, "child");
        assertTrue(world.get(child, VfxEmitterStateComponent.class).stopped);
        assertTrue(world.has(child, VfxSubEmitterSystem.VfxSubEmitterComponent.class));

        // Tick 1: the parent spawns and drains the same tick; the child must still be dormant.
        run(world, 0.2f, 1L);
        assertTrue(particles(world, child).isEmpty());
        assertEquals(0, world.get(child, VfxEmitterStateComponent.class).spawnedCount);

        // Tick 2: the parent has completed (emissionComplete) AND drained (buffer empty) -> arm once.
        run(world, 0.2f, 2L);
        assertFalse(world.get(child, VfxEmitterStateComponent.class).stopped);
        assertFalse(world.has(child, VfxSubEmitterSystem.VfxSubEmitterComponent.class));
        assertEquals(2, particles(world, child).size());
        assertEquals(2, world.get(child, VfxEmitterStateComponent.class).spawnedCount);

        // Continuing must never re-arm or re-spawn the one-shot child emitter.
        run(world, 0.2f, 3L);
        run(world, 0.2f, 4L);
        run(world, 0.2f, 5L);
        assertEquals(2, world.get(child, VfxEmitterStateComponent.class).spawnedCount);
        assertTrue(world.get(child, VfxEmitterStateComponent.class).emissionComplete);
    }

    @Test
    public void armedChildEmitsDeterministically() {
        PresentationWorld first = subEmitterWorld(3f, 1, 4, 21L);
        PresentationWorld second = subEmitterWorld(3f, 1, 4, 21L);
        for (int i = 1; i <= 4; i++) {
            run(first, 0.2f, i);
            run(second, 0.2f, i);
        }
        List<VfxParticle> a = particles(first, emitterEntity(first, "child"));
        List<VfxParticle> b = particles(second, emitterEntity(second, "child"));
        assertEquals(4, a.size());
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).lifetime, b.get(i).lifetime, 0f);
            assertEquals(a.get(i).position.x, b.get(i).position.x, 0f);
            assertEquals(a.get(i).position.y, b.get(i).position.y, 0f);
        }
    }

    @Test
    public void parentAndChildAreCleanedUpOnceWholeGraphDrains() {
        PresentationWorld world = subEmitterWorld(3f, 1, 2, 8L);
        for (int i = 0; i < 40 && !world.entities().isEmpty(); i++) run(world, 0.2f, i);
        assertTrue(world.entities().isEmpty());
    }

    @Test
    public void grandchildTriggerIsNotArmedDepthIsOne() {
        PresentationWorld world = new PresentationWorld("depth-1");
        new VfxInstantiateSystem().instantiate(world, new VfxSceneDefinition("scene", 1, 3f,
                Arrays.asList(emitterNode("parent", null, false),
                        emitterNode("child", "parent", true),
                        emitterNode("grandchild", "child", true)),
                Collections.<VfxResourceRef>emptyList(), VfxCapability.DEGRADED), 0L);

        EntityId child = emitterEntity(world, "child");
        EntityId grandchild = emitterEntity(world, "grandchild");
        // Depth bound: a trigger child under a trigger parent gets no dormant marker and stays stopped.
        assertFalse(world.has(grandchild, VfxSubEmitterSystem.VfxSubEmitterComponent.class));

        for (int i = 1; i <= 8; i++) run(world, 0.2f, i);
        assertFalse(world.get(child, VfxEmitterStateComponent.class).stopped);
        assertTrue(world.get(grandchild, VfxEmitterStateComponent.class).stopped);
        assertTrue(particles(world, grandchild).isEmpty());
        assertEquals(0, world.get(grandchild, VfxEmitterStateComponent.class).spawnedCount);
    }

    @Test
    public void childCapArmsOnlyTheFixedNumberOfTriggerChildren() {
        int triggerChildren = 7;
        List<VfxNodeDefinition> nodes = new ArrayList<VfxNodeDefinition>();
        nodes.add(emitterNode("parent", null, false));
        for (int i = 0; i < triggerChildren; i++) nodes.add(emitterNode("child" + i, "parent", true));
        PresentationWorld world = new PresentationWorld("cap");
        new VfxInstantiateSystem().instantiate(world, new VfxSceneDefinition("scene", 1, 3f, nodes,
                Collections.<VfxResourceRef>emptyList(), VfxCapability.DEGRADED), 0L);

        for (int i = 1; i <= 8; i++) run(world, 0.2f, i);

        int emitting = 0;
        int stillDormant = 0;
        for (int i = 0; i < triggerChildren; i++) {
            EntityId child = emitterEntity(world, "child" + i);
            VfxEmitterStateComponent state = world.get(child, VfxEmitterStateComponent.class);
            if (state.stopped) stillDormant++;
            else if (state.spawnedCount > 0) emitting++;
        }
        assertEquals(VfxSubEmitterSystem.MAX_SUB_EMITTERS_PER_PARENT, emitting);
        assertEquals(triggerChildren - VfxSubEmitterSystem.MAX_SUB_EMITTERS_PER_PARENT, stillDormant);
    }

    private static void run(PresentationWorld world, float delta, long sequence) {
        EcsPipeline.run(world, new EcsTick(delta, sequence), SIMULATION);
    }

    private static PresentationWorld subEmitterWorld(float duration, int parentAmount, int childAmount, long seed) {
        PresentationWorld world = new PresentationWorld("sub-emitter");
        new VfxInstantiateSystem().instantiate(world, new VfxSceneDefinition("scene", 1, duration,
                Arrays.asList(emitterNode("parent", null, false, parentAmount, 0.05f, seed),
                        emitterNode("child", "parent", true, childAmount, 1f, seed + 1L)),
                Collections.<VfxResourceRef>emptyList(), VfxCapability.DEGRADED), 0L);
        return world;
    }

    private static VfxNodeDefinition emitterNode(String id, String parent, boolean trigger) {
        return emitterNode(id, parent, trigger, 1, 1f, 3L);
    }

    private static VfxNodeDefinition emitterNode(String id, String parent, boolean trigger,
            int amount, float lifetime, long seed) {
        ParticleEmitterDefinition emitter = new ParticleEmitterDefinition(amount, lifetime, 0f,
                new VfxVec2(1f, 0f), 0f, new VfxRange(0f, 0f), new VfxVec2(0f, 0f),
                new VfxRange(1f, 1f), null, null, null, new VfxRange(0f, 0f), null, null, seed);
        return new VfxNodeDefinition(id, "Root/" + id, parent, "GPUParticles2D", null, null,
                null, null, null, null, null, emitter,
                trigger ? VfxNodeDefinition.TRIGGER_ON_PARENT_COMPLETE : "");
    }

    private static EntityId emitterEntity(PresentationWorld world, String definitionNodeId) {
        for (EntityId entity : world.query(VfxNodeComponent.class, VfxEmitterComponent.class)) {
            if (definitionNodeId.equals(world.get(entity, VfxNodeComponent.class).definitionNodeId)) {
                return entity;
            }
        }
        throw new AssertionError("emitter node not found: " + definitionNodeId);
    }

    private static List<VfxParticle> particles(PresentationWorld world, EntityId emitter) {
        return world.get(emitter, VfxParticleBufferComponent.class).particles;
    }
}
