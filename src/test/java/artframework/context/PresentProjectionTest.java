package artframework.context;

import artframework.api.ArtFramework;
import artframework.ecs.EntityId;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;
import artframework.sts1.input.NativeInputRecords;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * R1: PresentProjection hard-sync behaviour across scene epochs.
 */
public class PresentProjectionTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void epochChangeClearsVisualItemsButKeepsSurfaces() {
        PresentSurfaces.get(SurfaceIds.MAP).mount();
        PresentationRegistry.context("c2-surfaces").create(
                new PresentationKey("sts1.visual.test", "item"), "item", "visual", "c2");

        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.of(
                        1L,
                        1L,
                        "combat",
                        Collections.<CardView>emptyList(),
                        ControlsView.empty(),
                        MapView.empty(),
                        new ViewportView(1920, 1080, 1920, 1080)));
        ArtFramework.publishFrame(backend.currentFrame());

        assertEquals(2, PresentationRegistry.context("c2-surfaces").entities().size());

        backend.publish(
                ContextFrame.of(
                        2L,
                        2L,
                        "combat",
                        Collections.<CardView>emptyList(),
                        ControlsView.empty(),
                        MapView.empty(),
                        new ViewportView(1920, 1080, 1920, 1080)));
        ArtFramework.publishFrame(backend.currentFrame());

        assertEquals(1, PresentationRegistry.context("c2-surfaces").entities().size());
        EntityId remaining = PresentationRegistry.context("c2-surfaces").entities().get(0);
        assertNotNull(
                "surface mount/lifecycle entity must survive the epoch change",
                PresentationRegistry.context("c2-surfaces")
                        .world()
                        .get(remaining, SurfaceIdentityComponent.class));
    }

    @Test
    public void staleFrameFromPriorEpochDoesNotClearPersistentProjectionState() {
        CardView card = new CardView(
                new CardRef("card-1", "strike"), CardZone.HAND, 0, CardPose.at(10f, 20f),
                true, false, false, false, "card-art", "card-frame");
        ArtFramework.publishFrame(ContextFrame.of(
                10L, 2L, "combat", Collections.singletonList(card),
                ControlsView.empty(), MapView.empty(), new ViewportView(1920, 1080, 1920, 1080)));
        ArtFramework.projection().setDragInstanceId("card-1");

        FrameDiff diff = ArtFramework.publishFrame(ContextFrame.of(
                11L, 1L, "map", Collections.<CardView>emptyList(),
                ControlsView.empty(), MapView.empty(), new ViewportView(1920, 1080, 1920, 1080)));

        assertFalse(diff.applied);
        assertEquals(10L, ArtFramework.projection().lastFrameId());
        assertEquals(2L, ArtFramework.projection().sceneEpoch());
        assertEquals("combat", ArtFramework.projection().scene());
        assertNotNull(ArtFramework.projection().get("card-1"));
        assertEquals("card-1", ArtFramework.projection().dragInstanceId());
    }

    @Test
    public void staleUnavailableFramePreservesProjectionAndObservation() {
        CardView card = new CardView(
                new CardRef("card-1", "strike"), CardZone.HAND, 0, CardPose.at(10f, 20f),
                true, false, false, false, "card-art", "card-frame");
        ArtFramework.publishFrame(ContextFrame.of(
                10L, 2L, "combat", Collections.singletonList(card),
                ControlsView.empty(), MapView.empty(), new ViewportView(1920, 1080, 1920, 1080)));
        NativeInputRecords.input("play_card", SurfaceIds.COMBAT_HAND);
        NativeInputRecords.intent(SurfaceIds.COMBAT_HAND, "play_card",
                NativeIntentLifecycleComponent.State.EXECUTED, "accepted");
        NativeInputRecords.observe(SurfaceIds.COMBAT_HAND, 10L, 2L, "combat");

        FrameDiff diff = ArtFramework.publishFrame(unavailable(9L, 1L, "map"));

        assertFalse(diff.applied);
        assertEquals(10L, ArtFramework.projection().lastFrameId());
        assertEquals(2L, ArtFramework.projection().sceneEpoch());
        assertTrue(ArtFramework.projection().isAvailable());
        assertNotNull(ArtFramework.projection().get("card-1"));
        NativeIntentObservationComponent observation = observation();
        assertTrue(observation.available);
        assertEquals(10L, observation.frameId);
        assertEquals(2L, observation.sceneEpoch);
    }

    @Test
    public void lowerFrameIdUnavailableInSameEpochPreservesProjectionAndObservation() {
        CardView card = new CardView(
                new CardRef("card-1", "strike"), CardZone.HAND, 0, CardPose.at(10f, 20f),
                true, false, false, false, "card-art", "card-frame");
        ArtFramework.publishFrame(ContextFrame.of(
                10L, 2L, "combat", Collections.singletonList(card),
                ControlsView.empty(), MapView.empty(), new ViewportView(1920, 1080, 1920, 1080)));
        ArtFramework.projection().setDragInstanceId("card-1");
        NativeInputRecords.input("play_card", SurfaceIds.COMBAT_HAND);
        NativeInputRecords.observe(SurfaceIds.COMBAT_HAND, 10L, 2L, "combat");

        FrameDiff diff = ArtFramework.publishFrame(unavailable(9L, 2L, "map"));

        assertFalse(diff.applied);
        assertEquals(10L, ArtFramework.projection().lastFrameId());
        assertEquals(2L, ArtFramework.projection().sceneEpoch());
        assertEquals("combat", ArtFramework.projection().scene());
        assertTrue(ArtFramework.projection().isAvailable());
        assertNotNull(ArtFramework.projection().get("card-1"));
        assertEquals("card-1", ArtFramework.projection().dragInstanceId());
        NativeIntentObservationComponent observation = observation();
        assertTrue(observation.available);
        assertEquals(10L, observation.frameId);
        assertEquals(2L, observation.sceneEpoch);
    }

    @Test
    public void newerUnavailableEpochClearsCardsDragAndVisualItems() {
        CardView card = new CardView(
                new CardRef("card-1", "strike"), CardZone.HAND, 0, CardPose.at(10f, 20f),
                true, false, false, false, "card-art", "card-frame");
        ArtFramework.publishFrame(ContextFrame.of(
                10L, 2L, "combat", Collections.singletonList(card),
                ControlsView.empty(), MapView.empty(), new ViewportView(1920, 1080, 1920, 1080)));
        ArtFramework.projection().setDragInstanceId("card-1");
        PresentationRegistry.context("c2-surfaces").create(
                new PresentationKey("sts1.visual.test", "item"), "item", "visual", "c2");

        FrameDiff diff = ArtFramework.publishFrame(unavailable(1L, 3L, "map"));

        assertFalse(diff.applied);
        assertEquals(0, ArtFramework.projection().size());
        assertNull(ArtFramework.projection().dragInstanceId());
        assertEquals(0, PresentationRegistry.context("c2-surfaces").entities().size());
        assertEquals(1L, ArtFramework.projection().lastFrameId());
        assertEquals(3L, ArtFramework.projection().sceneEpoch());
        assertEquals("map", ArtFramework.projection().scene());
        assertFalse(ArtFramework.projection().isAvailable());
        assertTrue(ArtFramework.projection().isStale());
    }

    @Test
    public void newerUnavailableFrameAdvancesMetadataAndFailsObservation() {
        ArtFramework.publishFrame(ContextFrame.of(
                10L, 2L, "combat", Collections.<CardView>emptyList(),
                ControlsView.empty(), MapView.empty(), new ViewportView(1920, 1080, 1920, 1080)));
        NativeInputRecords.input("play_card", SurfaceIds.COMBAT_HAND);
        NativeInputRecords.intent(SurfaceIds.COMBAT_HAND, "play_card",
                NativeIntentLifecycleComponent.State.EXECUTED, "accepted");

        FrameDiff diff = ArtFramework.publishFrame(unavailable(1L, 3L, "map"));

        assertFalse(diff.applied);
        assertEquals(1L, ArtFramework.projection().lastFrameId());
        assertEquals(3L, ArtFramework.projection().sceneEpoch());
        assertEquals("map", ArtFramework.projection().scene());
        assertFalse(ArtFramework.projection().isAvailable());
        assertTrue(ArtFramework.projection().isStale());
        NativeIntentObservationComponent observation = observation();
        assertFalse(observation.available);
        assertEquals(1L, observation.frameId);
    }

    private static ContextFrame unavailable(long frameId, long sceneEpoch, String scene) {
        return new ContextFrame(frameId, sceneEpoch, scene, null,
                ControlsView.empty(), MapView.empty(), EventView.empty(), SelectView.empty(),
                false, null);
    }

    private static NativeIntentObservationComponent observation() {
        EntityId entity = PresentationRegistry.context("sts1-input").entity(
                new PresentationKey("sts1.input", SurfaceIds.COMBAT_HAND));
        return PresentationRegistry.world().get(entity, NativeIntentObservationComponent.class);
    }
}
