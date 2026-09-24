package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.component.Rect;
import artframework.context.RoomShellView;
import artframework.core.PackSystemPhase;
import artframework.core.PackSystems;
import artframework.ecs.ArtEcs;
import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import artframework.render.ArtRenderContributionComponent;
import artframework.render.ArtRenderFrame;
import artframework.render.ArtRenderFrameAggregationSystem;
import artframework.render.ArtRenderFrameComponent;
import artframework.render.NativeRenderOwnership;
import artframework.render.RenderPixelPayload;
import artframework.render.RenderPlan;
import artframework.sts1.PresentSafety;
import artframework.sts1.backend.Sts1RoomShellProjection;
import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Focused tests for the ECS-projected room-shell overlay: payload entries migrate into the shared
 * aggregate frame while the label metadata stays on the minimal room-shell frame.
 */
public class RoomShellRenderFrameTest {
    @After
    public void tearDown() {
        PackSystems.resetForTests();
        RoomShellRenderProjectionSystem.resetForTests();
        Sts1RoomShellProjection.clear();
        PresentSafety.resetForTests();
        ArtFramework.resetForTests();
    }

    private static RoomShellView view(String kind, String id, boolean visible, boolean available,
            Rect bounds) {
        return new RoomShellView(kind, id, "Room " + id, "COMPLETE", available, bounds,
                ResourceIds.roomShell(kind), visible);
    }

    /** Runs the room-shell producer then the shared aggregation pass, as the schedule does. */
    private static void runPipeline(PresentationWorld world, long sequence) {
        RoomShellRenderProjectionSystem system = new RoomShellRenderProjectionSystem();
        system.run(world, new EcsTick(0f, sequence));
        new ArtRenderFrameAggregationSystem().run(world, new EcsTick(0f, sequence + 1000L));
    }

    @Test public void payloadEntriesMapShellAndMetadataCarriesTitle() {
        RoomShellView source = view("generic", "r1", true, true, new Rect(10f, 20f, 300f, 40f));

        RoomShellRenderFrame metadata = RoomShellRenderFrame.of(source, false);
        List<RenderPlan.Entry> entries = RoomShellRenderFrame.payloadEntries(source, false);

        assertEquals(1, entries.size());
        RenderPlan.Entry entry = entries.get(0);
        RenderPixelPayload payload = entry.payload;
        assertNotNull(payload);
        assertEquals(ResourceIds.UI_ROOM_SHELL_GENERIC, payload.resourceId());
        assertEquals(10f, payload.x(), 0f);
        assertEquals(20f, payload.y(), 0f);
        assertEquals(300f, payload.width(), 0f);
        assertEquals(40f, payload.height(), 0f);
        assertEquals(0f, payload.sourceX(), 0f);
        assertEquals(1f, payload.sourceWidth(), 0f);
        assertEquals(1f, payload.a(), 0f);
        assertEquals("room-shell:generic:r1", entry.stableKey);
        // Title stays host-side metadata; it must not be encoded as a second pixel payload.
        assertEquals("Room r1", metadata.title());
        assertEquals(boundsCenterX(source), metadata.titleCenterX(), 0f);
        // Same input yields the same key/order/payload geometry.
        assertFalse(RoomShellRenderFrame.payloadEntries(source, false).isEmpty());
        assertEquals(entry.stableKey,
                RoomShellRenderFrame.payloadEntries(source, false).get(0).stableKey);
    }

    private static float boundsCenterX(RoomShellView view) {
        return view.bounds.x + view.bounds.width * 0.5f;
    }

    @Test public void ownershipStaysNonDelegatedOverlay() {
        RoomShellRenderFrame frame =
                RoomShellRenderFrame.of(view("generic", "r1", true, true, new Rect(0f, 0f, 1f, 1f)), false);
        assertEquals(NativeRenderOwnership.NATIVE_WITH_ART_OVERLAY, frame.ownership());
        assertFalse(frame.ownership() == NativeRenderOwnership.DELEGATED_TO_ART);
        assertEquals(NativeRenderOwnership.NATIVE_WITH_ART_OVERLAY, RoomShellRenderFrame.empty().ownership());
    }

    @Test public void unavailableInvisibleNullAndEventOwnedShellsProjectEmpty() {
        assertTrue(RoomShellRenderFrame.of(null, false).isEmpty());
        assertTrue(RoomShellRenderFrame.payloadEntries(null, false).isEmpty());
        assertTrue(RoomShellRenderFrame.of(view("generic", "r", true, false, new Rect(0f, 0f, 5f, 5f)),
                false).isEmpty());
        assertTrue(RoomShellRenderFrame.payloadEntries(view("generic", "r", true, false,
                new Rect(0f, 0f, 5f, 5f)), false).isEmpty());
        assertTrue(RoomShellRenderFrame.of(view("generic", "r", false, true, new Rect(0f, 0f, 5f, 5f)),
                false).isEmpty());
        // Event shell is suppressed only while the delegated event surface is available.
        RoomShellView event = view("event", "e", true, true, new Rect(0f, 0f, 5f, 5f));
        assertTrue(RoomShellRenderFrame.of(event, true).isEmpty());
        assertTrue(RoomShellRenderFrame.payloadEntries(event, true).isEmpty());
        assertFalse(RoomShellRenderFrame.of(event, false).isEmpty());
        assertEquals(1, RoomShellRenderFrame.payloadEntries(event, false).size());
    }

    @Test public void zeroSizedBoundsDropTexturePayloadButKeepTitle() {
        RoomShellView source = view("generic", "r", true, true, Rect.ZERO);
        assertTrue(RoomShellRenderFrame.payloadEntries(source, false).isEmpty());
        RoomShellRenderFrame metadata = RoomShellRenderFrame.of(source, false);
        assertEquals("Room r", metadata.title());
        assertFalse(metadata.isEmpty());
    }

    @Test public void projectionPublishesContributionMetadataAndClearRemovesThem() {
        PresentationWorld world = new PresentationWorld("room-shell-frame");
        RoomShellRenderProjectionSystem system = new RoomShellRenderProjectionSystem();
        Sts1RoomShellProjection.publish(view("generic", "r", true, true, new Rect(1f, 2f, 3f, 4f)));

        assertTrue(world.query(RoomShellRenderFrameComponent.class).isEmpty());
        system.run(world, new EcsTick(0f, 1L));
        // Producer contribution published (payload entries) plus metadata frame.
        assertEquals(1, ArtRenderContributionComponent.contributionFor(
                world, RoomShellRenderProjectionSystem.PRODUCER_ID).size());
        EntityId entity = world.query(RoomShellRenderFrameComponent.class).get(0);
        assertEquals(1, world.query(RoomShellRenderFrameComponent.class).size());
        assertNotNull(RoomShellRenderProjectionSystem.frameFor(world));
        // Aggregation merges the contribution into the shared frame, sorted/deduped.
        new ArtRenderFrameAggregationSystem().run(world, new EcsTick(0f, 2L));
        ArtRenderFrame aggregate = ArtRenderFrameComponent.read(world);
        assertEquals(1, aggregate.size());
        assertEquals("room-shell:generic:r", aggregate.entries().get(0).stableKey);

        // A later projection updates the same entities; a clear removes the components.
        system.run(world, new EcsTick(0f, 3L));
        assertEquals(1, world.query(RoomShellRenderFrameComponent.class).size());
        Sts1RoomShellProjection.clear();
        runPipeline(world, 4L);
        assertTrue(world.query(RoomShellRenderFrameComponent.class).isEmpty());
        assertTrue(world.query(ArtRenderFrameComponent.class).isEmpty());
        assertNull(RoomShellRenderProjectionSystem.frameFor(world));
    }

    @Test public void installRegistersOnlyInRenderProjectionPhase() {
        RoomShellRenderProjectionSystem.install();
        RoomShellRenderProjectionSystem.install();
        int matches = 0;
        for (EcsSystem system : PackSystems.systemsFor(PackSystemPhase.RENDER_PROJECTION)) {
            if (system instanceof RoomShellRenderProjectionSystem) matches++;
        }
        assertEquals(1, matches);
        assertTrue(PackSystems.systemsFor(PackSystemPhase.EFFECTS).isEmpty());
        RoomShellRenderProjectionSystem.uninstall();
        assertTrue(PackSystems.systemsFor(PackSystemPhase.RENDER_PROJECTION).isEmpty());
    }

    @Test public void consumerDrawsAggregateFrameEntriesNotLiveProjection() {
        // No frame published: the consumer must draw nothing even though the observation is live.
        Sts1RoomShellProjection.publish(view("generic", "live", true, true, new Rect(1f, 2f, 3f, 4f)));
        assertTrue(Sts1RoomShellDrawPath.aggregatedEntries().isEmpty());
        assertNull(Sts1RoomShellDrawPath.publishedMetadata());

        // Publish an aggregate + metadata for a different shell than the live projection; the
        // consumer reads the published frames, so its content follows them, not the observation.
        runPipeline(ArtEcs.world(), 1L);
        ArtRenderContributionComponent.publish(ArtEcs.world(),
                RoomShellRenderProjectionSystem.PRODUCER_ID,
                RoomShellRenderFrame.payloadEntries(
                        view("generic", "framed", true, true, new Rect(9f, 9f, 9f, 9f)), false));
        new ArtRenderFrameAggregationSystem().run(ArtEcs.world(), new EcsTick(0f, 2L));
        RoomShellRenderProjectionSystem.publish(ArtEcs.world(),
                RoomShellRenderFrame.of(view("generic", "framed", true, true, new Rect(9f, 9f, 9f, 9f)),
                        false));

        List<RenderPlan.Entry> entries = Sts1RoomShellDrawPath.aggregatedEntries();
        assertEquals(1, entries.size());
        assertEquals("room-shell:generic:framed", entries.get(0).stableKey);
        RoomShellRenderFrame metadata = Sts1RoomShellDrawPath.publishedMetadata();
        assertNotNull(metadata);
        assertEquals("Room framed", metadata.title());

        // Clear the ECS frames while the observation stays live: the consumer again draws nothing.
        ArtEcs.world().clear();
        assertTrue(Sts1RoomShellDrawPath.aggregatedEntries().isEmpty());
        assertNull(Sts1RoomShellDrawPath.publishedMetadata());
    }

    @Test public void probeKeepsOverlaySemanticsAndReportsFrameEvidence() {
        Sts1RoomShellProjection.publish(view("generic", "r", true, true, new Rect(1f, 2f, 3f, 4f)));
        runPipeline(ArtEcs.world(), 1L);

        Map<String, Object> probe = Sts1RoomShellDrawPath.probeSlice();
        assertEquals(Boolean.TRUE, probe.get("nativeContinuation"));
        assertEquals(Boolean.FALSE, probe.get("nativePixelsSuppressed"));
        assertEquals("overlay-only", probe.get("surface"));
        assertEquals(Integer.valueOf(1), probe.get("frameEntries"));
        assertEquals(Boolean.TRUE, probe.get("frameHasTitle"));
        assertEquals(Boolean.FALSE, probe.get("frameNativePixelsSuppressed"));
    }

    /**
     * ARF-02: a foreign producer that happens to reuse a {@code room-shell:} stable-key prefix must
     * not be consumed by the room-shell draw path; selection follows structured producer ownership.
     */
    @Test public void consumerIgnoresForeignProducerWithRoomShellKeyPrefix() {
        PresentationWorld world = new PresentationWorld("room-shell-owner-isolation");
        ArtRenderContributionComponent.publish(world, RoomShellRenderProjectionSystem.PRODUCER_ID,
                RoomShellRenderFrame.payloadEntries(
                        view("generic", "mine", true, true, new Rect(1f, 2f, 3f, 4f)), false));
        // A foreign producer deliberately chooses a colliding prefix.
        ArtRenderContributionComponent.publish(world, "foreign-producer",
                RoomShellRenderFrame.payloadEntries(
                        view("generic", "theirs", true, true, new Rect(5f, 5f, 5f, 5f)), false));
        new ArtRenderFrameAggregationSystem().run(world, new EcsTick(0f, 2L));

        ArtRenderFrame aggregate = ArtRenderFrameComponent.read(world);
        assertEquals(2, aggregate.size());

        List<RenderPlan.Entry> mine = Sts1RoomShellDrawPath.roomShellEntries(aggregate);
        assertEquals(1, mine.size());
        assertEquals("room-shell:generic:mine", mine.get(0).stableKey);
    }

    /**
     * ARF-03: metadata and payload entries must come from the same observation within one tick.
     * A source that visibly changes between reads would expose double sampling; here the test pins
     * one observation and asserts metadata and entries agree.
     */
    @Test public void metadataAndEntriesComeFromOneSamplePerTick() {
        PresentationWorld world = new PresentationWorld("room-shell-one-sample");
        final int[] samples = new int[1];

        // Source flips to empty on its second read; a double-sampling system would publish a title
        // with no entries (or vice versa) instead of a consistent pair.
        RoomShellRenderProjectionSystem.setObservationSourceForTests(
                new RoomShellRenderProjectionSystem.ObservationSource() {
                    @Override public RoomShellRenderProjectionSystem.Observation sample() {
                        samples[0]++;
                        if (samples[0] <= 1) {
                            return new RoomShellRenderProjectionSystem.Observation(
                                    view("generic", "r", true, true, new Rect(1f, 2f, 3f, 4f)), false);
                        }
                        return new RoomShellRenderProjectionSystem.Observation(null, false);
                    }
                });

        RoomShellRenderProjectionSystem system = new RoomShellRenderProjectionSystem();
        system.run(world, new EcsTick(0f, 1L));
        assertEquals("one observation per tick", 1, samples[0]);
        new ArtRenderFrameAggregationSystem().run(world, new EcsTick(0f, 2L));

        // Both halves agree: the metadata carries a title and the payload has exactly one entry.
        RoomShellRenderFrame metadata = RoomShellRenderProjectionSystem.frameFor(world);
        assertNotNull(metadata);
        assertEquals("Room r", metadata.title());
        assertEquals(1, ArtRenderFrameComponent.read(world).entriesFor(
                RoomShellRenderProjectionSystem.PRODUCER_ID).size());
    }

    @Test public void emptyObservationYieldsEmptyMetadataAndEmptyAggregate() {
        PresentationWorld world = new PresentationWorld("room-shell-empty-sample");
        RoomShellRenderProjectionSystem.setObservationSourceForTests(
                new RoomShellRenderProjectionSystem.ObservationSource() {
                    @Override public RoomShellRenderProjectionSystem.Observation sample() {
                        return new RoomShellRenderProjectionSystem.Observation(null, false);
                    }
                });

        new RoomShellRenderProjectionSystem().run(world, new EcsTick(0f, 1L));
        new ArtRenderFrameAggregationSystem().run(world, new EcsTick(0f, 2L));
        assertTrue(world.query(RoomShellRenderFrameComponent.class).isEmpty());
        assertTrue(ArtRenderFrameComponent.read(world).isEmpty());
        assertNull(Sts1RoomShellDrawPath.publishedMetadata());
        assertTrue(Sts1RoomShellDrawPath.aggregatedEntries().isEmpty());
    }

    /**
     * ARF-05(d): the consumer entry reads only published frames; no frame means no draw, even when
     * the live observation is visible.
     */
    @Test public void consumerEntryDoesNotReadObservationWhenNoFrameIsPublished() {
        PresentationWorld world = new PresentationWorld("room-shell-consumer-frame-only");
        Sts1RoomShellProjection.publish(
                view("generic", "live", true, true, new Rect(1f, 2f, 3f, 4f)));

        assertTrue(Sts1RoomShellDrawPath.aggregatedEntries().isEmpty());
        assertNull(Sts1RoomShellDrawPath.publishedMetadata());

        // Publish only a contribution, no aggregate yet: the consumer still sees no frame.
        ArtRenderContributionComponent.publish(world, RoomShellRenderProjectionSystem.PRODUCER_ID,
                RoomShellRenderFrame.payloadEntries(
                        view("generic", "live", true, true, new Rect(1f, 2f, 3f, 4f)), false));
        assertTrue(Sts1RoomShellDrawPath.aggregatedEntries().isEmpty());
    }
}
