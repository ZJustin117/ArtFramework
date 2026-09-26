package artframework.vfx;

import artframework.render.RenderPhase;
import artframework.render.RenderPixelPayload;
import artframework.render.RenderPlan;
import artframework.render.RenderTargetKind;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Focused tests for the ART-owned VFX payload-bearing frame contribution. */
public class VfxRenderFramePayloadTest {
    private static VfxParticleDraw draw(String node, int particle, float z, int frame,
            int columns, int rows, String blend) {
        return new VfxParticleDraw("scene", node, particle, 0, "p.png",
                10f, 20f, 45f, 2f, 3f, 1f, 0f, 0.5f, 0.75f, blend, z,
                frame, columns, rows, true, false);
    }

    @Test public void mapsDrawToHostNeutralPayloadDeterministically() {
        VfxParticleDraw source = draw("node", 1, 4f, 3, 4, 2, "ADD");

        RenderPlan.Entry entry = VfxRenderFrame.payloadEntry(source);

        RenderPixelPayload payload = entry.payload;
        assertEquals("p.png", payload.resourceId());
        assertEquals("node", payload.label());
        assertEquals("ADD", payload.blendMode());
        assertEquals(10f, payload.x(), 0f);
        assertEquals(20f, payload.y(), 0f);
        assertEquals(45f, payload.rotationDegrees(), 0f);
        assertEquals(2f, payload.scaleX(), 0f);
        assertEquals(3f, payload.scaleY(), 0f);
        assertEquals(1f, payload.r(), 0f);
        assertEquals(0f, payload.g(), 0f);
        assertEquals(0.5f, payload.b(), 0f);
        assertEquals(0.75f, payload.a(), 0f);
        assertTrue(payload.flipX());
        assertFalse(payload.flipY());
        // Flipbook frame 3 on a 4x2 sheet -> column 3, row 0, normalized 1/4 x 1/2.
        assertEquals(3, payload.flipbookFrame());
        assertEquals(4, payload.flipbookColumns());
        assertEquals(2, payload.flipbookRows());
        assertEquals(0.75f, payload.sourceX(), 0f);
        assertEquals(0f, payload.sourceY(), 0f);
        assertEquals(0.25f, payload.sourceWidth(), 0f);
        assertEquals(0.5f, payload.sourceHeight(), 0f);
        assertEquals(RenderPhase.ART_EFFECTS, entry.phase);
        assertEquals(4f, entry.z, 0f);
        assertEquals("scene/node/0/1", entry.stableKey);
        assertEquals(RenderTargetKind.OVERLAY, entry.kind);
        // Same input maps to the same stable ordering and payload coordinates.
        RenderPlan.Entry again = VfxRenderFrame.payloadEntry(source);
        assertEquals(entry.stableKey, again.stableKey);
        assertEquals(payload.sourceX(), again.payload.sourceX(), 0f);
    }

    @Test public void frameEntriesFollowExistingDrawOrderAndAreImmutable() {
        VfxParticleDraw high = draw("z-node", 0, 9f, 0, 1, 1, "MIX");
        VfxParticleDraw low = draw("a-node", 0, -1f, 0, 1, 1, "MIX");
        List<VfxParticleDraw> draws = new ArrayList<VfxParticleDraw>(Arrays.asList(high, low));

        VfxRenderFrame frame = new VfxRenderFrame(draws, Arrays.asList(2));
        // Payload entries preserve the same (phase, z, stableKey) order as the draw list.
        assertEquals(Arrays.asList("a-node", "z-node"), Arrays.asList(
                frame.planEntries.get(0).payload.label(),
                frame.planEntries.get(1).payload.label()));

        List<RenderPlan.Entry> mutable = new ArrayList<RenderPlan.Entry>(frame.planEntries);
        RenderPlan.Entry replacement = VfxRenderFrame.payloadEntry(high);
        mutable.set(0, replacement);
        assertEquals("plan entries must be copied", "a-node",
                frame.planEntries.get(0).payload.label());

        try {
            frame.planEntries.add(replacement);
            throw new AssertionError("plan entries must be immutable");
        } catch (UnsupportedOperationException expected) { }
    }

    @Test public void emptyAndNullDrawListsProduceEmptyPayloadFrame() {
        assertTrue(VfxRenderFrame.empty().planEntries.isEmpty());
        VfxRenderFrame empty = new VfxRenderFrame(Collections.<VfxParticleDraw>emptyList());
        assertTrue(empty.planEntries.isEmpty());
        assertTrue(empty.draws.isEmpty());
    }

    @Test public void unifiedFrameMixesPayloadLessNativeEntriesWithVfxPayloadEntries() {
        VfxParticleDraw vfxDraw = draw("vfx", 0, 3f, 0, 1, 1, "ADD");
        VfxRenderFrame frame = new VfxRenderFrame(Collections.singletonList(vfxDraw));
        RenderPlan.Entry nativeRetained = RenderPlan.Entry.payloadEntry("native:x",
                RenderTargetKind.SYNTHETIC_WIDGET, new artframework.component.Rect(0f, 0f, 1f, 1f),
                RenderPhase.NATIVE_RETAINED, 0f, "x", true, null);

        RenderPlan plan = RenderPlan.unifiedFrame(
                Collections.singletonList(nativeRetained), frame.planEntries);

        assertEquals(2, plan.entries().size());
        assertEquals("native:x", plan.entries().get(0).id);
        assertNull("native-retained entry carries no payload", plan.entries().get(0).payload);
        assertEquals("vfx:scene/vfx/0/0", plan.entries().get(1).id);
        assertEquals("ADD", plan.entries().get(1).payload.blendMode());
    }

    @Test public void projectionSystemPublishesPayloadEntriesAlongsideDraws() {
        ParticleEmitterDefinition emitter = new ParticleEmitterDefinition(1, 1f, 0f,
                null, 0f, null, null, null, null, null, null, null, null, null, 1L, "tex");
        VfxNodeDefinition node = new VfxNodeDefinition("n", "n", null, "GPUParticles2D",
                null, null, null, null, null, null, null, emitter);
        artframework.ecs.PresentationWorld world = new artframework.ecs.PresentationWorld("payload-frame");
        new VfxInstantiateSystem().instantiate(world, new VfxSceneDefinition("s", 1, 1f,
                Collections.singletonList(node), Collections.singletonList(
                        new VfxResourceRef("tex", "TEXTURE", "a.png", "a.png", "supported")),
                VfxCapability.SUPPORTED), 0L);
        artframework.ecs.EntityId entity = world.query(VfxParticleBufferComponent.class).get(0);
        world.put(entity, VfxParticleBufferComponent.class, new VfxParticleBufferComponent(
                Collections.singletonList(new VfxParticle(0, 0f, 1f, new VfxVec2(1f, 2f),
                        new VfxVec2(0f, 0f), 0f, 0f, 1f, 1f, 1f, null))));

        new ParticleRenderProjectionSystem().run(world, new artframework.ecs.EcsTick(0f, 0L));

        artframework.ecs.EntityId root = world.query(VfxSceneRuntimeComponent.class).get(0);
        String producerId = ParticleRenderProjectionSystem.producerId(
                world.get(root, VfxSceneRuntimeComponent.class));
        List<RenderPlan.Entry> entries =
                artframework.render.ArtRenderContributionComponent.contributionFor(world, producerId);
        assertEquals(1, entries.size());
        assertEquals("a.png", entries.get(0).payload.resourceId());
        VfxDrawList draws = world.get(root, VfxDrawListComponent.class).value;
        assertEquals(1, draws.draws.size());
        assertEquals(draws.draws.get(0).stableKey, entries.get(0).stableKey);
    }
}
