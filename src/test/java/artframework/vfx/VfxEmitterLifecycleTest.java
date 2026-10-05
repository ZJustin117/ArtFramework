package artframework.vfx;

import artframework.ecs.EcsPipeline;
import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Single-emitter create -> fixed-step/seeded -> stop (no respawn) -> restart (deterministic)
 * -> cleanup lifecycle, exercised purely without GL.
 */
public class VfxEmitterLifecycleTest {
    private static final List<EcsSystem> SIMULATION = Arrays.<EcsSystem>asList(
            new ParticleSpawnSystem(), new ParticleIntegrateSystem(),
            new ParticleCurveSystem(), new VfxLifecycleSystem());

    @Test
    public void fixedStepSeededEmissionIsIdenticalAcrossWorlds() {
        PresentationWorld first = world(emitter(8, 2f, 1L), 0f);
        PresentationWorld second = world(emitter(8, 2f, 1L), 0f);
        for (int i = 0; i < 5; i++) {
            EcsPipeline.run(first, new EcsTick(0.05f, i), SIMULATION);
            EcsPipeline.run(second, new EcsTick(0.05f, i), SIMULATION);
        }
        assertParticleStateEquals(particles(first, emitterEntity(first)),
                particles(second, emitterEntity(second)));
    }

    @Test
    public void stoppedEmitterNeverSpawnsAndGraphCleansUpAfterParticlesExpire() {
        PresentationWorld world = world(emitter(4, 0.1f, 7L), 0f);
        EntityId emitter = emitterEntity(world);
        assertTrue(VfxEmitterControl.stop(world, emitter));
        EcsPipeline.run(world, new EcsTick(0.2f, 1L), SIMULATION);
        assertTrue(world.entities().isEmpty());
    }

    @Test
    public void stopBlocksFurtherSpawningWhileExistingParticlesAgeOut() {
        PresentationWorld world = world(emitter(4, 0.3f, 3L), 5f);
        EntityId emitter = emitterEntity(world);
        EcsPipeline.run(world, new EcsTick(0f, 1L), SIMULATION);
        assertEquals(4, particles(world, emitter).size());
        assertEquals(4, world.get(emitter, VfxEmitterStateComponent.class).spawnedCount);

        assertTrue(VfxEmitterControl.stop(world, emitter));
        // Further ticks must not spawn additional particles; the buffer only shrinks.
        EcsPipeline.run(world, new EcsTick(0.1f, 2L), SIMULATION);
        assertEquals(4, particles(world, emitter).size());
        EcsPipeline.run(world, new EcsTick(0.3f, 3L), SIMULATION);
        assertTrue(particles(world, emitter).isEmpty());
        assertTrue(world.contains(emitter)); // scene duration has not yet elapsed
    }

    @Test
    public void restartResumesEmissionDeterministicallyWithSameSeed() {
        PresentationWorld world = world(emitter(6, 5f, 42L), 10f);
        EntityId emitter = emitterEntity(world);
        EcsPipeline.run(world, new EcsTick(0f, 1L), SIMULATION);
        List<VfxParticle> firstEmission = particles(world, emitter);

        assertTrue(VfxEmitterControl.stop(world, emitter));
        EcsPipeline.run(world, new EcsTick(0.5f, 2L), SIMULATION);
        assertTrue(VfxEmitterControl.restart(world, emitter));
        assertFalse(world.get(emitter, VfxEmitterStateComponent.class).stopped);
        EcsPipeline.run(world, new EcsTick(0f, 3L), SIMULATION);

        assertParticleStateEquals(firstEmission, particles(world, emitter));
    }

    @Test
    public void restartAfterCompletionReEmitsIdentically() {
        PresentationWorld world = world(emitter(3, 1f, 11L), 10f);
        EntityId emitter = emitterEntity(world);
        EcsPipeline.run(world, new EcsTick(0f, 1L), SIMULATION);
        List<VfxParticle> first = particles(world, emitter);
        assertTrue(world.get(emitter, VfxEmitterStateComponent.class).emissionComplete);

        assertTrue(VfxEmitterControl.restart(world, emitter));
        assertFalse(world.get(emitter, VfxEmitterStateComponent.class).emissionComplete);
        EcsPipeline.run(world, new EcsTick(0f, 2L), SIMULATION);
        assertParticleStateEquals(first, particles(world, emitter));
    }

    @Test
    public void stopAndRestartReturnFalseForNonEmitterEntities() {
        PresentationWorld world = new PresentationWorld("no-emitter");
        EntityId plain = world.createEntity();
        assertFalse(VfxEmitterControl.stop(world, plain));
        assertFalse(VfxEmitterControl.restart(world, plain));
    }

    private static PresentationWorld world(ParticleEmitterDefinition definition, float duration) {
        PresentationWorld world = new PresentationWorld("emitter-lifecycle");
        new VfxInstantiateSystem().instantiate(world, scene(duration, definition), 1L);
        return world;
    }

    private static EntityId emitterEntity(PresentationWorld world) {
        return world.query(VfxEmitterComponent.class).get(0);
    }

    private static List<VfxParticle> particles(PresentationWorld world, EntityId emitter) {
        return world.get(emitter, VfxParticleBufferComponent.class).particles;
    }

    private static void assertParticleStateEquals(List<VfxParticle> a, List<VfxParticle> b) {
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            VfxParticle x = a.get(i);
            VfxParticle y = b.get(i);
            assertEquals(x.spawnIndex, y.spawnIndex);
            assertEquals(x.lifetime, y.lifetime, 0f);
            assertEquals(x.age, y.age, 0f);
            assertEquals(x.position.x, y.position.x, 0f);
            assertEquals(x.position.y, y.position.y, 0f);
            assertEquals(x.velocity.x, y.velocity.x, 0f);
            assertEquals(x.velocity.y, y.velocity.y, 0f);
            assertEquals(x.rotationDegrees, y.rotationDegrees, 0f);
            assertEquals(x.scale, y.scale, 0f);
            assertEquals(x.alpha, y.alpha, 0f);
        }
    }

    private static ParticleEmitterDefinition emitter(int amount, float lifetime, long seed) {
        return new ParticleEmitterDefinition(amount, lifetime, 0f, new VfxVec2(1f, 0f), 0f,
                new VfxRange(0f, 0f), new VfxVec2(0f, 0f), new VfxRange(1f, 1f),
                null, null, null, new VfxRange(0f, 0f), null, null, seed);
    }

    private static VfxSceneDefinition scene(float duration, ParticleEmitterDefinition emitter) {
        VfxNodeDefinition node = new VfxNodeDefinition("emitter", "Root/emitter", null,
                "GPUParticles2D", null, null, null, null, null, null, null, emitter);
        return new VfxSceneDefinition("scene", 1, duration,
                Collections.singletonList(node), Collections.<VfxResourceRef>emptyList(),
                VfxCapability.DEGRADED);
    }
}
