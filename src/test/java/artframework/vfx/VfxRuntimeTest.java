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

import static org.junit.Assert.*;

public class VfxRuntimeTest {
    private static final List<EcsSystem> SIMULATION = Arrays.<EcsSystem>asList(
            new ParticleSpawnSystem(), new ParticleIntegrateSystem(),
            new ParticleCurveSystem(), new VfxLifecycleSystem());

    @Test
    public void instantiatesDefinitionOrderAndHierarchy() {
        VfxNodeDefinition parent = node("parent", null, null);
        VfxNodeDefinition child = node("child", "parent", emitter(2, 1f, new VfxVec2(0f, 0f)));
        PresentationWorld world = new PresentationWorld("hierarchy");
        VfxInstance instance = new VfxInstantiateSystem().instantiate(world,
                scene(3f, Arrays.asList(parent, child), Collections.<VfxResourceRef>emptyList()), 4L);

        List<EntityId> entities = world.entities();
        assertEquals(3, entities.size());
        assertEquals(instance.rootEntity, entities.get(0));
        VfxNodeComponent first = world.get(entities.get(1), VfxNodeComponent.class);
        VfxNodeComponent second = world.get(entities.get(2), VfxNodeComponent.class);
        assertEquals("parent", first.definitionNodeId);
        assertEquals(0, first.definitionOrder);
        assertEquals(instance.rootEntity, first.parentEntity);
        assertEquals(entities.get(1), second.parentEntity);
        assertEquals(1, second.definitionOrder);
        assertTrue(world.has(entities.get(2), VfxEmitterStateComponent.class));
        assertTrue(world.get(entities.get(1), VfxTransformComponent.class).visible);
    }

    @Test
    public void legacyInstantiationUsesIdentityRootPlacementAndOverloadStoresOrigin() {
        VfxSceneDefinition definition = scene(1f,
                Collections.singletonList(node("n", null, emitter(1, 1f, new VfxVec2(0f, 0f)))),
                Collections.<VfxResourceRef>emptyList());
        PresentationWorld defaultWorld = new PresentationWorld("default-origin");
        EntityId defaultRoot = new VfxInstantiateSystem().instantiate(defaultWorld, definition, 0L).rootEntity;
        assertEquals(0f, defaultWorld.get(defaultRoot, VfxTransformComponent.class).position.x, 0f);
        assertEquals(0f, defaultWorld.get(defaultRoot, VfxTransformComponent.class).position.y, 0f);

        PresentationWorld placedWorld = new PresentationWorld("placed-origin");
        EntityId placedRoot = new VfxInstantiateSystem().instantiate(placedWorld, definition, 0L,
                960f, 540f).rootEntity;
        assertEquals(960f, placedWorld.get(placedRoot, VfxTransformComponent.class).position.x, 0f);
        assertEquals(540f, placedWorld.get(placedRoot, VfxTransformComponent.class).position.y, 0f);
    }

    @Test
    public void simulationIsExactlyRepeatableAcrossTicks() {
        PresentationWorld first = instantiatedWorld(emitter(8, 2f, new VfxVec2(0f, -3f)));
        PresentationWorld second = instantiatedWorld(emitter(8, 2f, new VfxVec2(0f, -3f)));
        for (int i = 0; i < 4; i++) {
            EcsPipeline.run(first, new EcsTick(0.125f, i), SIMULATION);
            EcsPipeline.run(second, new EcsTick(0.125f, i), SIMULATION);
        }
        List<VfxParticle> a = particles(first);
        List<VfxParticle> b = particles(second);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).lifetime, b.get(i).lifetime, 0f);
            assertEquals(a.get(i).position.x, b.get(i).position.x, 0f);
            assertEquals(a.get(i).position.y, b.get(i).position.y, 0f);
            assertEquals(a.get(i).rotationDegrees, b.get(i).rotationDegrees, 0f);
        }
    }

    @Test
    public void integratesGravityMotionAndAngularVelocity() {
        ParticleEmitterDefinition definition = new ParticleEmitterDefinition(1, 2f, 0f,
                new VfxVec2(1f, 0f), 0f, new VfxRange(4f, 4f), new VfxVec2(0f, 8f),
                new VfxRange(1f, 1f), null, null, null, new VfxRange(10f, 10f),
                null, null, 1L);
        PresentationWorld world = instantiatedWorld(definition);
        EcsPipeline.run(world, new EcsTick(0.25f, 1L), SIMULATION);
        VfxParticle particle = particles(world).get(0);
        assertEquals(4f, particle.velocity.x, 0.0001f);
        assertEquals(2f, particle.velocity.y, 0.0001f);
        assertEquals(1f, particle.position.x, 0.0001f);
        assertEquals(0.5f, particle.position.y, 0.0001f);
        assertEquals(2.5f, particle.rotationDegrees, 0.0001f);
    }

    @Test
    public void evaluatesScaleAlphaAndColorAtNormalizedAge() {
        VfxCurveDefinition scale = curve(1f, 3f);
        VfxCurveDefinition alpha = curve(1f, 0f);
        VfxGradientDefinition gradient = new VfxGradientDefinition(Arrays.asList(
                new VfxGradientDefinition.VfxGradientStop(0f, new VfxColor(1f, 0f, 0f, 1f)),
                new VfxGradientDefinition.VfxGradientStop(1f, new VfxColor(0f, 0f, 1f, 0.5f))));
        ParticleEmitterDefinition definition = new ParticleEmitterDefinition(1, 2f, 0f,
                new VfxVec2(1f, 0f), 0f, new VfxRange(0f, 0f), new VfxVec2(0f, 0f),
                new VfxRange(2f, 2f), scale, alpha, gradient, new VfxRange(0f, 0f),
                null, null, 2L);
        PresentationWorld world = instantiatedWorld(definition);
        EcsPipeline.run(world, new EcsTick(1f, 1L), SIMULATION);
        VfxParticle particle = particles(world).get(0);
        assertEquals(4f, particle.scale, 0.0001f);
        assertEquals(0.375f, particle.alpha, 0.0001f);
        assertEquals(0.5f, particle.color.r, 0.0001f);
        assertEquals(0.5f, particle.color.b, 0.0001f);
    }

    @Test
    public void clampsCapacityAndExpiresParticlesThenWholeGraph() {
        PresentationWorld bounded = instantiatedWorld(emitter(Integer.MAX_VALUE, 1f, new VfxVec2(0f, 0f)));
        EcsPipeline.run(bounded, new EcsTick(0f, 1L), SIMULATION);
        assertEquals(VfxEmitterComponent.MAX_PARTICLES, particles(bounded).size());

        PresentationWorld expiring = instantiatedWorld(emitter(2, 0.1f, new VfxVec2(0f, 0f)));
        EcsPipeline.run(expiring, new EcsTick(0.2f, 1L), SIMULATION);
        assertTrue(expiring.entities().isEmpty());
    }

    @Test
    public void changedEpochAndExplicitCleanupDestroyGraphsIdempotently() {
        PresentationWorld world = new PresentationWorld("epochs");
        VfxInstantiateSystem instantiate = new VfxInstantiateSystem();
        VfxSceneDefinition definition = scene(10f, Collections.singletonList(node("e", null,
                emitter(1, 10f, new VfxVec2(0f, 0f)))), Collections.<VfxResourceRef>emptyList());
        VfxInstance epochInstance = instantiate.instantiate(world, definition, 3L);
        assertTrue(VfxLifecycle.invalidateEpoch(world, epochInstance, 4L));
        new VfxLifecycleSystem().run(world, new EcsTick(0f, 1L));
        assertFalse(world.contains(epochInstance.rootEntity));
        assertFalse(VfxLifecycle.invalidateEpoch(world, epochInstance, 5L));

        VfxInstance cleanupInstance = instantiate.instantiate(world, definition, 6L);
        assertTrue(VfxLifecycle.requestCleanup(world, cleanupInstance));
        assertTrue(VfxLifecycle.requestCleanup(world, cleanupInstance));
        new VfxLifecycleSystem().run(world, new EcsTick(0f, 2L));
        assertFalse(VfxLifecycle.requestCleanup(world, cleanupInstance));
        new VfxLifecycleSystem().run(world, new EcsTick(0f, 3L));
        assertTrue(world.entities().isEmpty());
    }

    @Test
    public void missingResourcesDoNotBlockSimulationOrCleanup() {
        VfxResourceRef missing = new VfxResourceRef("texture", "TEXTURE", "missing.png", null,
                "missing-resource");
        PresentationWorld world = new PresentationWorld("missing-resource");
        new VfxInstantiateSystem().instantiate(world, scene(0f,
                Collections.singletonList(node("e", null, emitter(1, 0.1f, new VfxVec2(0f, 0f)))),
                Collections.singletonList(missing)), 0L);
        EcsPipeline.run(world, new EcsTick(0.2f, 1L), SIMULATION);
        assertTrue(world.entities().isEmpty());
    }

    private static PresentationWorld instantiatedWorld(ParticleEmitterDefinition emitter) {
        PresentationWorld world = new PresentationWorld("simulation");
        new VfxInstantiateSystem().instantiate(world, scene(0f,
                Collections.singletonList(node("emitter", null, emitter)),
                Collections.<VfxResourceRef>emptyList()), 0L);
        return world;
    }

    private static List<VfxParticle> particles(PresentationWorld world) {
        EntityId emitter = world.query(VfxParticleBufferComponent.class).get(0);
        return world.get(emitter, VfxParticleBufferComponent.class).particles;
    }

    private static ParticleEmitterDefinition emitter(int amount, float lifetime, VfxVec2 gravity) {
        return new ParticleEmitterDefinition(amount, lifetime, 0.25f, new VfxVec2(1f, 0f),
                30f, new VfxRange(2f, 4f), gravity, new VfxRange(0.5f, 1.5f),
                null, null, null, new VfxRange(-5f, 5f), null, null, 99L);
    }

    private static VfxCurveDefinition curve(float start, float end) {
        return new VfxCurveDefinition(Arrays.asList(
                new VfxCurveDefinition.VfxCurvePoint(0f, start, 0f, 0f, 0, 0),
                new VfxCurveDefinition.VfxCurvePoint(1f, end, 0f, 0f, 0, 0)),
                Collections.<Float>emptyList());
    }

    private static VfxNodeDefinition node(String id, String parent, ParticleEmitterDefinition emitter) {
        return new VfxNodeDefinition(id, "Root/" + id, parent, "Node2D", null, null,
                null, null, null, null, null, emitter);
    }

    private static VfxSceneDefinition scene(float duration, List<VfxNodeDefinition> nodes,
            List<VfxResourceRef> resources) {
        return new VfxSceneDefinition("scene", 1, duration, nodes, resources, VfxCapability.DEGRADED);
    }
}
