package artframework.sts1.render;

import artframework.assets.ResourceIds;
import artframework.component.Rect;
import artframework.context.OrbStanceView;
import artframework.core.PackSystemPhase;
import artframework.core.PackSystems;
import artframework.ecs.ArtEcs;
import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.render.ArtRenderFrame;
import artframework.render.ArtRenderFrameAggregationSystem;
import artframework.render.RenderPhase;
import artframework.render.RenderPlan;
import artframework.sts1.backend.Sts1OrbStanceProjection;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Publish-only slice for the stance observation: one payload entry into the shared frame, stale
 * cleanup, visibility/hasImage filtering, and install ordering. Pure logic, no GL/host.
 */
public class StanceRenderProjectionSystemTest {
    private static final String PRODUCER = "stance:Wrath";

    @After
    public void tearDown() {
        ArtEcs.world().clear();
        PackSystems.resetForTests();
        Sts1OrbStanceProjection.resetForTests();
    }

    private static OrbStanceView.Entry stanceEntry(String id, boolean visible, boolean hasImage) {
        return new OrbStanceView.Entry(
                id, "stance", "Wrath", 2, 0, 0, true,
                ResourceIds.stance("Wrath"), new Rect(1f, 2f, 512f, 512f), visible,
                30f, 0.5f, 0.25f, 0.75f, 1f,
                100f, 200f, 512f, 512f, 1.5f, hasImage, 256f, 256f);
    }

    private static void runPipeline() {
        new StanceRenderProjectionSystem().run(ArtEcs.world(), new EcsTick(0f, 1L));
        new ArtRenderFrameAggregationSystem().run(ArtEcs.world(), new EcsTick(0f, 2L));
    }

    @Test
    public void publishesOnePayloadEntryForVisibleImagedStance() {
        Sts1OrbStanceProjection.publish(new OrbStanceView(
                Collections.singletonList(stanceEntry(PRODUCER, true, true)), true));

        runPipeline();

        ArtRenderFrame frame = ArtRenderFrameAggregationSystem.frameFor(ArtEcs.world());
        assertNotNull(frame);
        List<RenderPlan.Entry> entries = frame.entriesFor(PRODUCER);
        assertEquals(1, entries.size());
        RenderPlan.Entry entry = entries.get(0);
        assertEquals(PRODUCER, entry.stableKey);
        assertEquals(RenderPhase.ART_EFFECTS, entry.phase);
        assertEquals(StanceRenderProjectionSystem.STANCE_Z, entry.z, 0f);
        assertNotNull(entry.payload);
        assertEquals(ResourceIds.stance("Wrath"), entry.payload.resourceId());
        assertEquals(100f, entry.payload.x(), 0f);
        assertEquals(200f, entry.payload.y(), 0f);
        assertEquals(512f, entry.payload.width(), 0f);
        assertEquals(512f, entry.payload.height(), 0f);
        assertEquals(1.5f, entry.payload.scaleX(), 0f);
        assertEquals(30f, entry.payload.rotationDegrees(), 0f);
        assertEquals(0.5f, entry.payload.r(), 0f);
        assertEquals(0.25f, entry.payload.g(), 0f);
        assertEquals(0.75f, entry.payload.b(), 0f);
        assertEquals(1f, entry.payload.a(), 0f);
    }

    @Test
    public void unknownStanceResourceFallsBackToUnknown() {
        OrbStanceView.Entry entry = new OrbStanceView.Entry(
                "stance:Missing", "stance", "Missing", 0, 0, 0, true,
                ResourceIds.stance("Missing"), Rect.ZERO, true,
                0f, 1f, 1f, 1f, 1f, 5f, 6f, 512f, 512f, 1f, true, 256f, 256f);
        Sts1OrbStanceProjection.publish(new OrbStanceView(
                Collections.singletonList(entry), true));

        runPipeline();

        ArtRenderFrame frame = ArtRenderFrameAggregationSystem.frameFor(ArtEcs.world());
        List<RenderPlan.Entry> entries = frame.entriesFor("stance:Missing");
        assertEquals(1, entries.size());
        assertEquals(ResourceIds.STANCE_UNKNOWN, entries.get(0).payload.resourceId());
    }

    @Test
    public void clearedProjectionDropsTheStaleContributionNextTick() {
        Sts1OrbStanceProjection.publish(new OrbStanceView(
                Collections.singletonList(stanceEntry(PRODUCER, true, true)), true));
        runPipeline();
        assertEquals(1, ArtRenderFrameAggregationSystem.frameFor(ArtEcs.world())
                .entriesFor(PRODUCER).size());

        Sts1OrbStanceProjection.clear();
        runPipeline();

        ArtRenderFrame frame = ArtRenderFrameAggregationSystem.frameFor(ArtEcs.world());
        assertTrue(frame == null || frame.entriesFor(PRODUCER).isEmpty());
    }

    @Test
    public void invisibleOrImagelessStanceIsNotPublished() {
        Sts1OrbStanceProjection.publish(new OrbStanceView(Arrays.asList(
                stanceEntry(PRODUCER, false, true),
                stanceEntry("stance:Calm", true, false)), true));

        runPipeline();

        ArtRenderFrame frame = ArtRenderFrameAggregationSystem.frameFor(ArtEcs.world());
        assertTrue(frame == null
                || (frame.entriesFor(PRODUCER).isEmpty()
                        && frame.entriesFor("stance:Calm").isEmpty()));
    }

    @Test
    public void installRegistersAfterProducerBeforeAggregator() {
        PackSystems.resetForTests();
        StanceRenderProjectionSystem.install();

        List<EcsSystem> systems = PackSystems.systemsFor(PackSystemPhase.RENDER_PROJECTION);
        int producer = indexOf(systems, StanceRenderProjectionSystem.class);
        int aggregator = indexOf(systems, ArtRenderFrameAggregationSystem.class);
        assertTrue(producer >= 0);
        assertTrue(aggregator >= 0);
        assertTrue("aggregator must run after the stance producer", producer < aggregator);
    }

    private static int indexOf(List<EcsSystem> systems, Class<?> type) {
        for (int i = 0; i < systems.size(); i++) {
            if (type.isInstance(systems.get(i))) return i;
        }
        return -1;
    }
}
