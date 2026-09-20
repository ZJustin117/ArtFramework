package artframework.vfx;

import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class ParticleRenderProjectionSystemTest {
    @Test public void projectsCompositionOrderingAndFlipbookWithoutMutation() {
        ParticleEmitterDefinition emitter = new ParticleEmitterDefinition(2, 2f, 0f,
                new VfxVec2(1f, 0f), 0f, new VfxRange(0f, 0f), new VfxVec2(0f, 0f),
                new VfxRange(2f, 2f), null, null, null, new VfxRange(0f, 0f),
                new VfxFlipbookDefinition(2, 2, true, 2f, 2f), "ADD", 1L, "tex");
        VfxNodeDefinition node = new VfxNodeDefinition("n", "Root/n", null, "GPUParticles2D",
                new VfxVec2(10f, 20f), new VfxVec2(2f, 3f), 0f, 4f, true,
                new VfxColor(.5f, .5f, .5f, .5f), null, emitter);
        VfxResourceRef resource = new VfxResourceRef("tex", "TEXTURE", "a.png", "resources/a.png", "supported");
        PresentationWorld world = new PresentationWorld("projection");
        new VfxInstantiateSystem().instantiate(world, new VfxSceneDefinition("s", 1, 2f,
                Collections.singletonList(node), Collections.singletonList(resource), VfxCapability.SUPPORTED), 0L);
        EntityId entity = world.query(VfxParticleBufferComponent.class).get(0);
        world.put(entity, VfxParticleBufferComponent.class, new VfxParticleBufferComponent(Arrays.asList(
                new VfxParticle(4, 1.25f, 2f, new VfxVec2(1f, 2f), new VfxVec2(0f, 0f), 10f, 0f,
                        1f, 1f, .8f, new VfxColor(1f, 0f, 1f, 1f)),
                new VfxParticle(1, 0f, 2f, new VfxVec2(0f, 0f), new VfxVec2(0f, 0f), 0f, 0f,
                        1f, 1f, 1f, new VfxColor(1f, 1f, 1f, 1f)))));
        ParticleRenderProjectionSystem system = new ParticleRenderProjectionSystem();
        system.run(world, new EcsTick(0f, 0L));
        VfxDrawList first = world.get(world.entities().get(0), VfxDrawListComponent.class).value;
        system.run(world, new EcsTick(0f, 0L));
        VfxDrawList second = world.get(world.entities().get(0), VfxDrawListComponent.class).value;
        assertEquals(2, first.draws.size());
        assertEquals(1, first.draws.get(0).particleIndex);
        assertEquals(2, first.draws.get(1).flipbookFrame);
        assertEquals(12f, first.draws.get(1).x, .001f);
        assertEquals(26f, first.draws.get(1).y, .001f);
        assertEquals(first.draws.get(1).x, second.draws.get(1).x, 0f);
        assertEquals("ADD", first.draws.get(0).blendMode);
    }

    @Test public void missingResourceOmitsOnlyProjection() {
        VfxNodeDefinition node = new VfxNodeDefinition("n", "n", null, "GPUParticles2D", null, null,
                null, null, null, null, null, new ParticleEmitterDefinition(1, 1f, 0f, null, 0f,
                        null, null, null, null, null, null, null, null, null, 1L, "missing"));
        PresentationWorld world = new PresentationWorld("missing");
        new VfxInstantiateSystem().instantiate(world, new VfxSceneDefinition("s", 1, 1f,
                Collections.singletonList(node), Collections.<VfxResourceRef>emptyList(), VfxCapability.DEGRADED), 0L);
        new ParticleRenderProjectionSystem().run(world, new EcsTick(0f, 0L));
        assertTrue(world.get(world.entities().get(0), VfxDrawListComponent.class).value.draws.isEmpty());
    }

    @Test public void rootPlacementIsComposedIntoProjectedCoordinates() {
        ParticleEmitterDefinition emitter = new ParticleEmitterDefinition(1, 1f, 0f,
                null, 0f, null, null, null, null, null, null, null, null, null, 1L, "tex");
        VfxNodeDefinition node = new VfxNodeDefinition("n", "n", null, "GPUParticles2D",
                new VfxVec2(10f, 20f), new VfxVec2(2f, 3f), 0f, 0f, true,
                null, null, emitter);
        PresentationWorld world = new PresentationWorld("root-placement");
        new VfxInstantiateSystem().instantiate(world, new VfxSceneDefinition("s", 1, 2f,
                Collections.singletonList(node), Collections.singletonList(
                        new VfxResourceRef("tex", "TEXTURE", "a.png", "a.png", "supported")),
                VfxCapability.SUPPORTED), 0L, 7f, 9f);
        EntityId entity = world.query(VfxParticleBufferComponent.class).get(0);
        world.put(entity, VfxParticleBufferComponent.class, new VfxParticleBufferComponent(
                Collections.singletonList(new VfxParticle(0, 0f, 1f, new VfxVec2(1f, 2f),
                        new VfxVec2(0f, 0f), 0f, 0f, 1f, 1f, 1f, null))));

        new ParticleRenderProjectionSystem().run(world, new EcsTick(0f, 0L));
        VfxDrawList draws = world.get(world.entities().get(0), VfxDrawListComponent.class).value;
        assertEquals(19f, draws.draws.get(0).x, .001f);
        assertEquals(35f, draws.draws.get(0).y, .001f);
    }
}
