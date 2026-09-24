package artframework.render;

import artframework.api.ArtFramework;
import artframework.component.Rect;
import artframework.core.HostBackend;
import artframework.core.HostCapabilities;
import artframework.core.PackSystemPhase;
import artframework.core.PackSystems;
import artframework.ecs.ArtEcs;
import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import artframework.presentation.PresentationMount;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Focused tests for the shared payload-frame aggregation point: multi-producer merge determinism,
 * ordering, duplicate-key rejection, empty-input safety, immutability, repeatability, and cleanup.
 */
public class ArtRenderFrameTest {

    @After public void tearDown() {
        ArtRenderFrameAggregationSystem.resetForTests();
        ArtEcs.world().clear();
        ArtFramework.resetForTests();
    }

    private static RenderPlan.Entry entry(String key, RenderPhase phase, float z) {
        RenderPixelPayload payload = new RenderPixelPayload(
                "res:" + key, key, 1f, 2f, 3f, 4f, 0f, 0f, 1f, 1f,
                false, false, 0f, 1f, 1f, 1f, 1f, 1f, 1f, "MIX", 0, 1, 1);
        return RenderPlan.Entry.payloadEntry(key, RenderTargetKind.OVERLAY,
                new Rect(1f, 2f, 3f, 4f), phase, z, key, true, payload);
    }

    @Test public void mergesMultipleProducerContributionsDeterministically() {
        PresentationWorld world = new PresentationWorld("aggregate-merge");
        ArtRenderContributionComponent.publish(world, "room-shell",
                Arrays.asList(entry("room-shell:generic:r1", RenderPhase.ART_EFFECTS, 5f)));
        ArtRenderContributionComponent.publish(world, "other",
                Arrays.asList(entry("other:a", RenderPhase.ART_EFFECTS, 1f),
                        entry("other:b", RenderPhase.ART_EFFECTS, 9f)));

        ArtRenderFrame frame = ArtRenderFrameAggregationSystem.aggregate(world);
        assertEquals(3, frame.size());
        // phase equal; z orders the frame, independent of publish order.
        assertEquals("other:a", frame.entries().get(0).stableKey);
        assertEquals("room-shell:generic:r1", frame.entries().get(1).stableKey);
        assertEquals("other:b", frame.entries().get(2).stableKey);

        // Repeatable: a second aggregation yields the same ordered keys.
        ArtRenderFrame again = ArtRenderFrameAggregationSystem.aggregate(world);
        List<String> first = keys(frame);
        List<String> second = keys(again);
        assertEquals(first, second);
    }

    @Test public void sortsByPhaseThenZThenStableKey() {
        PresentationWorld world = new PresentationWorld("aggregate-order");
        ArtRenderContributionComponent.publish(world, "p", Arrays.asList(
                entry("p:late", RenderPhase.ART_EFFECTS, 0f),
                entry("p:same-z-b", RenderPhase.C2_CONTENT, 2f),
                entry("p:same-z-a", RenderPhase.C2_CONTENT, 2f),
                entry("p:early", RenderPhase.NATIVE_RETAINED, 99f)));

        ArtRenderFrame frame = ArtRenderFrameAggregationSystem.aggregate(world);
        assertEquals(Arrays.asList("p:early", "p:same-z-a", "p:same-z-b", "p:late"), keys(frame));
    }

    @Test public void rejectsDuplicateStableKeyAcrossProducers() {
        PresentationWorld world = new PresentationWorld("aggregate-cross-dup");
        ArtRenderContributionComponent.publish(world, "a",
                Arrays.asList(entry("dup", RenderPhase.ART_EFFECTS, 0f)));
        ArtRenderContributionComponent.publish(world, "b",
                Arrays.asList(entry("dup", RenderPhase.ART_EFFECTS, 1f)));
        try {
            ArtRenderFrameAggregationSystem.aggregate(world);
            fail("expected duplicate stable key rejection across producers");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("dup"));
        }
    }

    @Test public void rejectsDuplicateStableKeyWithinOneProducer() {
        try {
            ArtRenderFrame.of(Arrays.asList(
                    entry("dup", RenderPhase.ART_EFFECTS, 0f),
                    entry("dup", RenderPhase.ART_EFFECTS, 2f)));
            fail("expected duplicate stable key rejection within a producer");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("dup"));
        }
    }

    @Test public void emptyInputIsSafe() {
        assertTrue(ArtRenderFrame.of(null).isEmpty());
        assertTrue(ArtRenderFrame.of(Collections.<RenderPlan.Entry>emptyList()).isEmpty());
        assertTrue(ArtRenderFrame.empty().isEmpty());

        PresentationWorld world = new PresentationWorld("aggregate-empty");
        assertTrue(ArtRenderFrameAggregationSystem.aggregate(world).isEmpty());
    }

    @Test public void aggregatedEntriesAreImmutableAndSortedSnapshot() {
        PresentationWorld world = new PresentationWorld("aggregate-immutable");
        List<RenderPlan.Entry> source = new ArrayList<RenderPlan.Entry>();
        source.add(entry("one", RenderPhase.ART_EFFECTS, 0f));
        ArtRenderContributionComponent.publish(world, "p", source);
        // Mutating the caller's list after publish must not affect the contribution.
        source.add(entry("two", RenderPhase.ART_EFFECTS, 1f));

        ArtRenderFrame frame = ArtRenderFrameAggregationSystem.aggregate(world);
        assertEquals(1, frame.size());
        try {
            frame.entries().add(entry("three", RenderPhase.ART_EFFECTS, 2f));
            fail("aggregate entry list must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test public void systemPublishesAndClearsAggregateComponent() {
        PresentationWorld world = new PresentationWorld("aggregate-system");
        ArtRenderFrameAggregationSystem system = new ArtRenderFrameAggregationSystem();
        assertTrue(world.query(ArtRenderFrameComponent.class).isEmpty());

        ArtRenderContributionComponent.publish(world, "p",
                Arrays.asList(entry("p:a", RenderPhase.ART_EFFECTS, 0f)));
        system.run(world, new EcsTick(0f, 1L));
        assertEquals(1, world.query(ArtRenderFrameComponent.class).size());
        assertNotNull(ArtRenderFrameAggregationSystem.frameFor(world));
        assertEquals(1, ArtRenderFrameComponent.read(world).size());

        // Clearing the producer contribution removes the aggregate component on the next run.
        ArtRenderContributionComponent.clear(world, "p");
        system.run(world, new EcsTick(0f, 2L));
        assertTrue(world.query(ArtRenderFrameComponent.class).isEmpty());
        assertTrue(ArtRenderFrameComponent.read(world).isEmpty());
    }

    @Test public void installRegistersOnlyInRenderProjectionPhaseAndIsIdempotent() {
        artframework.core.PackSystems.resetForTests();
        try {
            ArtRenderFrameAggregationSystem.install();
            ArtRenderFrameAggregationSystem.install();
            int matches = 0;
            for (artframework.ecs.EcsSystem system
                    : artframework.core.PackSystems.systemsFor(
                            artframework.core.PackSystemPhase.RENDER_PROJECTION)) {
                if (system instanceof ArtRenderFrameAggregationSystem) matches++;
            }
            assertEquals(1, matches);
            assertTrue(artframework.core.PackSystems.systemsFor(
                    artframework.core.PackSystemPhase.EFFECTS).isEmpty());
        } finally {
            artframework.core.PackSystems.resetForTests();
        }
    }

    @Test public void contributionComponentsAreDataOnlyAndProducerScoped() {
        PresentationWorld world = new PresentationWorld("aggregate-scoped");
        ArtRenderContributionComponent.publish(world, "a",
                Arrays.asList(entry("a:1", RenderPhase.ART_EFFECTS, 0f)));
        ArtRenderContributionComponent.publish(world, "b",
                Arrays.asList(entry("b:1", RenderPhase.ART_EFFECTS, 0f)));
        assertEquals(1, ArtRenderContributionComponent.contributionFor(world, "a").size());
        assertEquals(1, ArtRenderContributionComponent.contributionFor(world, "b").size());

        // Clearing one producer leaves the other untouched.
        ArtRenderContributionComponent.clear(world, "a");
        assertNotNull(ArtRenderContributionComponent.contributionFor(world, "b"));
        assertEquals(1, world.query(ArtRenderContributionComponent.class).size());

        // Re-publish replaces the same producer's contribution rather than adding a second entity.
        ArtRenderContributionComponent.publish(world, "b",
                Arrays.asList(entry("b:2", RenderPhase.ART_EFFECTS, 0f)));
        assertEquals(1, world.query(ArtRenderContributionComponent.class).size());
        assertEquals("b:2", ArtRenderContributionComponent.contributionFor(world, "b").get(0).stableKey);
    }

    private static List<String> keys(ArtRenderFrame frame) {
        List<String> out = new ArrayList<String>();
        for (RenderPlan.Entry entry : frame.entries()) out.add(entry.stableKey);
        return out;
    }

    @Test public void entriesAreAttributedToTheirProducerOwner() {
        PresentationWorld world = new PresentationWorld("aggregate-owner");
        ArtRenderContributionComponent.publish(world, "room-shell",
                Arrays.asList(entry("room-shell:generic:r1", RenderPhase.ART_EFFECTS, 1f)));
        ArtRenderContributionComponent.publish(world, "other",
                Arrays.asList(entry("other:a", RenderPhase.ART_EFFECTS, 2f)));

        ArtRenderFrame frame = ArtRenderFrameAggregationSystem.aggregate(world);
        assertEquals("other", frame.ownerOf("other:a"));
        assertEquals("room-shell", frame.ownerOf("room-shell:generic:r1"));
        assertEquals(Arrays.asList("room-shell:generic:r1"),
                keyList(frame.entriesFor("room-shell")));
        // Unknown and null owners select nothing; an unattributed frame selects nothing.
        assertTrue(frame.entriesFor("nobody").isEmpty());
        assertTrue(frame.entriesFor(null).isEmpty());
        assertTrue(ArtRenderFrame.of(Arrays.asList(entry("raw", RenderPhase.ART_EFFECTS, 0f)))
                .entriesFor("any").isEmpty());
    }

    @Test public void runDoesNotThrowOnDuplicateKeyAndAdvancesRenderClockAndBackend() {
        final PresentationWorld world = ArtEcs.world();
        ArtRenderContributionComponent.publish(world, "a",
                Arrays.asList(entry("dup", RenderPhase.ART_EFFECTS, 0f)));
        ArtRenderContributionComponent.publish(world, "b",
                Arrays.asList(entry("dup", RenderPhase.ART_EFFECTS, 1f)));

        ArtRenderFrameAggregationSystem.install();
        final float[] clockAfterRenderProjection = new float[1];
        final boolean[] backendRan = new boolean[1];
        ArtFramework.setHostBackend(new HostBackend() {
            @Override public boolean isReady() { return true; }
            @Override public void attach(PresentationMount mount) {}
            @Override public void detach(PresentationMount mount) {}
            @Override public void applyLayout(PresentationMount mount) {}
            @Override public HostCapabilities capabilities() { return HostCapabilities.none(); }

            @Override public void tick(float deltaSeconds) {
                backendRan[0] = true;
                clockAfterRenderProjection[0] = ArtFramework.render().timeSeconds();
            }
        });

        float before = ArtFramework.render().timeSeconds();
        // Must not throw even though the merged contributions contain a duplicate stable key.
        ArtFramework.advanceFrame(0.25f, null);

        // Later schedule phases still ran: render clock advanced and the host backend ticked.
        assertTrue(backendRan[0]);
        assertEquals(before + 0.25f, ArtFramework.render().timeSeconds(), 0.0001f);
        assertEquals(before + 0.25f, clockAfterRenderProjection[0], 0.0001f);

        // No stale frame: the published aggregate is the safe empty frame, not the prior content.
        assertTrue(ArtRenderFrameComponent.read(world).isEmpty());
        // Rejection is queryable through the frame component.
        assertTrue(ArtRenderFrameComponent.rejectedCount(world) >= 1L);
        assertTrue(ArtRenderFrameComponent.rejectionReason(world).contains("dup"));
    }

    @Test public void duplicateKeyDoesNotLeaveAPreviousAggregateAsThisFramesContent() {
        PresentationWorld world = new PresentationWorld("aggregate-dup-stale");
        ArtRenderFrameAggregationSystem system = new ArtRenderFrameAggregationSystem();
        ArtRenderContributionComponent.publish(world, "p",
                Arrays.asList(entry("good", RenderPhase.ART_EFFECTS, 0f)));
        system.run(world, new EcsTick(0f, 1L));
        assertEquals(1, ArtRenderFrameComponent.read(world).size());
        assertEquals(0L, ArtRenderFrameComponent.rejectedCount(world));

        // Introduce a duplicate: the run must publish empty (not the stale "good" frame).
        ArtRenderContributionComponent.publish(world, "q",
                Arrays.asList(entry("good", RenderPhase.ART_EFFECTS, 5f)));
        system.run(world, new EcsTick(0f, 2L)); // must not throw
        assertTrue(ArtRenderFrameComponent.read(world).isEmpty());
        assertEquals(1L, ArtRenderFrameComponent.rejectedCount(world));

        // Recovering to a unique key clears the reason and keeps the cumulative count.
        ArtRenderContributionComponent.clear(world, "q");
        system.run(world, new EcsTick(0f, 3L));
        assertEquals(1, ArtRenderFrameComponent.read(world).size());
        assertEquals(1L, ArtRenderFrameComponent.rejectedCount(world));
        assertEquals("", ArtRenderFrameComponent.rejectionReason(world));
    }

    /**
     * ARF-R-01: the cumulative rejection count must survive an empty tick. A reject, then a run
     * with no contributions (the frame component itself is removed), then another reject must count
     * two, not reset to one or zero.
     */
    @Test public void cumulativeRejectionCountSurvivesEmptyTicks() {
        PresentationWorld world = new PresentationWorld("aggregate-reject-empty-reject");
        ArtRenderFrameAggregationSystem system = new ArtRenderFrameAggregationSystem();

        // Reject #1: duplicate key.
        ArtRenderContributionComponent.publish(world, "a",
                Arrays.asList(entry("dup", RenderPhase.ART_EFFECTS, 0f)));
        ArtRenderContributionComponent.publish(world, "b",
                Arrays.asList(entry("dup", RenderPhase.ART_EFFECTS, 1f)));
        system.run(world, new EcsTick(0f, 1L));
        assertEquals(1L, ArtRenderFrameComponent.rejectedCount(world));
        assertTrue(ArtRenderFrameComponent.rejectionReason(world).contains("dup"));

        // Empty tick: contributions cleared, the aggregate is removed. The diagnostic must survive.
        ArtRenderContributionComponent.clear(world, "a");
        ArtRenderContributionComponent.clear(world, "b");
        system.run(world, new EcsTick(0f, 2L));
        assertTrue(world.query(ArtRenderFrameComponent.class).isEmpty());
        assertEquals(1L, ArtRenderFrameComponent.rejectedCount(world));
        assertTrue(ArtRenderFrameComponent.rejectionReason(world).contains("dup"));

        // Reject #2: must accumulate to 2, not reset.
        ArtRenderContributionComponent.publish(world, "a",
                Arrays.asList(entry("dup2", RenderPhase.ART_EFFECTS, 0f)));
        ArtRenderContributionComponent.publish(world, "b",
                Arrays.asList(entry("dup2", RenderPhase.ART_EFFECTS, 1f)));
        system.run(world, new EcsTick(0f, 3L));
        assertEquals(2L, ArtRenderFrameComponent.rejectedCount(world));
        assertTrue(ArtRenderFrameComponent.rejectionReason(world).contains("dup2"));
        // The published content stays the safe empty frame, never a stale aggregate.
        assertTrue(ArtRenderFrameComponent.read(world).isEmpty());

        // A later successful aggregation clears the reason but keeps the cumulative count.
        ArtRenderContributionComponent.clear(world, "b");
        system.run(world, new EcsTick(0f, 4L));
        assertEquals(1, ArtRenderFrameComponent.read(world).size());
        assertEquals(2L, ArtRenderFrameComponent.rejectedCount(world));
        assertEquals("", ArtRenderFrameComponent.rejectionReason(world));
    }

    /**
     * ARF-R2-01: once a non-empty frame is published, a diagnostics read/write failure while
     * clearing the last rejection reason must not discard the committed frame or advance the
     * rejection count. The diagnostics clear is a best-effort side channel and must be isolated
     * from the publish boundary.
     */
    @Test public void diagnosticsClearFailureAfterPublishKeepsPublishedFrameAndCount() {
        PresentationWorld world = new PresentationWorld("aggregate-diag-clear-failure");
        ArtRenderFrameAggregationSystem system = new ArtRenderFrameAggregationSystem();

        // Seed a real rejection so a successful publish would normally attempt a reason clear.
        ArtRenderContributionComponent.publish(world, "a",
                Arrays.asList(entry("dup", RenderPhase.ART_EFFECTS, 0f)));
        ArtRenderContributionComponent.publish(world, "b",
                Arrays.asList(entry("dup", RenderPhase.ART_EFFECTS, 1f)));
        system.run(world, new EcsTick(0f, 1L));
        assertEquals(1L, ArtRenderFrameComponent.rejectedCount(world));

        // Now make the aggregation succeed while the diagnostics read/write fails.
        ArtRenderContributionComponent.clear(world, "b");
        ArtRenderFrameAggregationSystem.setFailDiagnosticsClearForTests(true);
        try {
            system.run(world, new EcsTick(0f, 2L)); // must not throw and must not failSafely
        } finally {
            ArtRenderFrameAggregationSystem.resetForTests();
        }

        // The committed frame survives the diagnostics failure instead of being replaced by empty.
        ArtRenderFrame published = ArtRenderFrameComponent.read(world);
        assertEquals(1, published.size());
        assertEquals("dup", published.entries().get(0).stableKey);
        // The rejection count was not incremented by the diagnostics failure.
        assertEquals(1L, ArtRenderFrameComponent.rejectedCount(world));
    }

    /**
     * ARF-R-03: a failure outside {@code aggregate} (here a closed world whose {@code query} throws)
     * must not escape {@link ArtRenderFrameAggregationSystem#run}. The aggregation boundary must
     * swallow it so later schedule phases still run.
     */
    @Test public void runDoesNotThrowWhenWorldBecomesUnusable() {
        PresentationWorld world = new PresentationWorld("aggregate-closed-world");
        ArtRenderContributionComponent.publish(world, "p",
                Arrays.asList(entry("p:a", RenderPhase.ART_EFFECTS, 0f)));
        world.close();

        // Must not throw: the failure is contained at the system boundary.
        new ArtRenderFrameAggregationSystem().run(world, new EcsTick(0f, 1L));
    }

    @Test public void producerRegistersBeforeAggregatorInRenderProjectionPhase() {
        PackSystems.resetForTests();
        try {
            // Mirrors Sts1SurfaceRenderer install order: producer contribution first, then merge.
            artframework.sts1.render.RoomShellRenderProjectionSystem.install();
            ArtRenderFrameAggregationSystem.install();

            List<EcsSystem> systems =
                    PackSystems.systemsFor(PackSystemPhase.RENDER_PROJECTION);
            int producer = indexOf(systems, artframework.sts1.render.RoomShellRenderProjectionSystem.class);
            int aggregator = indexOf(systems, ArtRenderFrameAggregationSystem.class);
            assertTrue(producer >= 0);
            assertTrue(aggregator >= 0);
            assertTrue("producer must be registered before the aggregator", producer < aggregator);
        } finally {
            PackSystems.resetForTests();
            artframework.sts1.render.RoomShellRenderProjectionSystem.uninstall();
        }
    }

    private static int indexOf(List<EcsSystem> systems, Class<?> type) {
        for (int i = 0; i < systems.size(); i++) {
            if (type.isInstance(systems.get(i))) return i;
        }
        return -1;
    }

    private static List<String> keyList(List<RenderPlan.Entry> entries) {
        List<String> out = new ArrayList<String>();
        for (RenderPlan.Entry entry : entries) out.add(entry.stableKey);
        return out;
    }
}
