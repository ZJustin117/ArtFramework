package artframework.vfx;

import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import org.junit.Test;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import static org.junit.Assert.*;
import artframework.render.ArtRenderFrame;
import artframework.render.ArtRenderFrameAggregationSystem;
import artframework.render.ArtRenderContributionComponent;
import artframework.render.RenderPhase;
import artframework.render.RenderPlan;

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
        assertEquals(RenderPhase.ART_EFFECTS, first.draws.get(0).renderOrder.phase);
        assertEquals("s/n/0/1", first.draws.get(0).stableKey);
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

    @Test public void aggregateFrameConcatenatesRootsInWorldOrderAndIsImmutable() {
        PresentationWorld world = new PresentationWorld("aggregate-frame");
        VfxResourceRef resource = new VfxResourceRef("tex", "TEXTURE", "a.png", "a.png", "supported");
        ParticleEmitterDefinition emitter = new ParticleEmitterDefinition(1, 1f, 0f,
                null, 0f, null, null, null, null, null, null, null, null, null, 1L, "tex");
        for (String scene : Arrays.asList("first", "second")) {
            VfxNodeDefinition node = new VfxNodeDefinition("node", "node", null, "GPUParticles2D",
                    null, null, null, null, null, null, null, emitter);
            new VfxInstantiateSystem().instantiate(world, new VfxSceneDefinition(scene, 1, 1f,
                    Collections.singletonList(node), Collections.singletonList(resource),
                    VfxCapability.SUPPORTED), 0L);
            EntityId particleEntity = world.query(VfxParticleBufferComponent.class).get(
                    world.query(VfxParticleBufferComponent.class).size() - 1);
            world.put(particleEntity, VfxParticleBufferComponent.class,
                    new VfxParticleBufferComponent(Collections.singletonList(new VfxParticle(0, 0f, 1f,
                            new VfxVec2(0f, 0f), new VfxVec2(0f, 0f), 0f, 0f, 1f, 1f, 1f, null))));
        }

        new ParticleRenderProjectionSystem().run(world, new EcsTick(0f, 0L));
        java.util.List<EntityId> roots = world.query(VfxSceneRuntimeComponent.class);
        assertEquals(Arrays.asList("first", "second"), Arrays.asList(
                drawList(world, roots.get(0)).draws.get(0).sceneId,
                drawList(world, roots.get(1)).draws.get(0).sceneId));
        // Each root owns its own immutable draw list.
        VfxDrawList firstDraws = drawList(world, roots.get(0));
        try {
            firstDraws.draws.add(firstDraws.draws.get(0));
            fail("draw list must be immutable");
        } catch (UnsupportedOperationException expected) { }
        java.util.List<VfxParticleDraw> mutable = new ArrayList<VfxParticleDraw>(firstDraws.draws);
        VfxDrawList copied = new VfxDrawList(mutable);
        mutable.clear();
        assertEquals(1, copied.draws.size());

        // The shared frame concatenates both roots' contributions; destroying the roots clears them.
        new ArtRenderFrameAggregationSystem().run(world, new EcsTick(0f, 1L));
        assertEquals(2, artframework.render.ArtRenderFrameComponent.read(world).size());
        for (EntityId root : world.query(VfxSceneRuntimeComponent.class)) world.destroyEntity(root);
        new ParticleRenderProjectionSystem().run(world, new EcsTick(0f, 2L));
        new ArtRenderFrameAggregationSystem().run(world, new EcsTick(0f, 3L));
        assertEquals(0, world.query(ArtRenderContributionComponent.class).size());
        assertTrue(artframework.render.ArtRenderFrameComponent.read(world).entries().isEmpty());
    }

    private static VfxDrawList drawList(PresentationWorld world, EntityId root) {
        return world.get(root, VfxDrawListComponent.class).value;
    }

    /** Builds a one-node scene with a single particle and returns the world plus its root. */
    private static PresentationWorld worldWithRoot(String sceneId, long epoch) {
        ParticleEmitterDefinition emitter = new ParticleEmitterDefinition(1, 1f, 0f,
                null, 0f, null, null, null, null, null, null, null, null, null, 1L, "tex");
        VfxNodeDefinition node = new VfxNodeDefinition("n", "n", null, "GPUParticles2D",
                null, null, null, null, null, null, null, emitter);
        PresentationWorld world = new PresentationWorld("vfx-contribution");
        new VfxInstantiateSystem().instantiate(world, new VfxSceneDefinition(sceneId, 1, 1f,
                Collections.singletonList(node), Collections.singletonList(
                        new VfxResourceRef("tex", "TEXTURE", "a.png", "a.png", "supported")),
                VfxCapability.SUPPORTED), epoch);
        EntityId entity = world.query(VfxParticleBufferComponent.class).get(0);
        world.put(entity, VfxParticleBufferComponent.class, new VfxParticleBufferComponent(
                Collections.singletonList(new VfxParticle(0, 0f, 1f, new VfxVec2(0f, 0f),
                        new VfxVec2(0f, 0f), 0f, 0f, 1f, 1f, 1f, null))));
        return world;
    }

    private static ArtRenderFrame projectAndAggregate(PresentationWorld world, long sequence) {
        new ParticleRenderProjectionSystem().run(world, new EcsTick(0f, sequence));
        new ArtRenderFrameAggregationSystem().run(world, new EcsTick(0f, sequence + 1000L));
        return artframework.render.ArtRenderFrameComponent.read(world);
    }

    @Test public void publishesStableProducerContributionAndStaysStableAcrossRuns() {
        PresentationWorld world = worldWithRoot("s", 0L);

        ArtRenderFrame first = projectAndAggregate(world, 1L);
        String producerId = ParticleRenderProjectionSystem.producerId(
                world.get(world.query(VfxSceneRuntimeComponent.class).get(0),
                        VfxSceneRuntimeComponent.class));
        assertEquals("vfx:s#0", producerId);
        assertNotNull(first);
        assertEquals(1, first.entriesFor(producerId).size());
        assertEquals(producerId, first.ownerOf("s/n/0/0"));
        // The published entry carries the draw's phase/z/key and a pixel payload.
        RenderPlan.Entry entry = first.entriesFor(producerId).get(0);
        assertEquals(RenderPhase.ART_EFFECTS, entry.phase);
        assertNotNull(entry.payload);
        assertEquals("s/n/0/0", entry.stableKey);

        // Re-running must keep the same producer id and entries in the shared frame.
        ArtRenderFrame second = projectAndAggregate(world, 2L);
        assertEquals(1, second.entriesFor(producerId).size());
        assertEquals(entry.stableKey, second.entriesFor(producerId).get(0).stableKey);
    }

    @Test public void rerunKeepsEntryCountAndKeysUnchanged() {
        PresentationWorld world = worldWithRoot("s", 0L);
        ArtRenderFrame first = projectAndAggregate(world, 1L);
        java.util.List<String> before = stableKeys(first.entries());
        int countBefore = first.size();

        ArtRenderFrame second = projectAndAggregate(world, 2L);
        assertEquals(countBefore, second.size());
        assertEquals(before, stableKeys(second.entries()));
    }

    @Test public void staleContributionClearedWhenRootDestroyed() {
        PresentationWorld world = worldWithRoot("s", 0L);
        ArtRenderFrame live = projectAndAggregate(world, 1L);
        String producerId = "vfx:s#0";
        assertEquals(1, live.entriesFor(producerId).size());

        // Root destroyed (scene completed): the next projection must drop the stale contribution
        // so the shared frame no longer carries the dead root's entries.
        for (EntityId root : world.query(VfxSceneRuntimeComponent.class)) world.destroyEntity(root);

        new ParticleRenderProjectionSystem().run(world, new EcsTick(0f, 2L));
        new ArtRenderFrameAggregationSystem().run(world, new EcsTick(0f, 3L));
        ArtRenderFrame after = artframework.render.ArtRenderFrameComponent.read(world);
        assertEquals(0, world.query(ArtRenderContributionComponent.class).size());
        assertTrue(after.entriesFor(producerId).isEmpty());
    }

    private static java.util.List<String> stableKeys(java.util.List<RenderPlan.Entry> entries) {
        java.util.List<String> keys = new ArrayList<String>();
        for (RenderPlan.Entry entry : entries) keys.add(entry.stableKey);
        return keys;
    }
}
