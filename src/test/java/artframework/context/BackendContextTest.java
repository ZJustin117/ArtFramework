package artframework.context;

import artframework.api.ArtFramework;
import artframework.core.SignalDecision;
import artframework.core.UiSignal;
import artframework.ecs.EntityId;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BackendContextTest {
    @After public void tearDown() { ArtFramework.resetForTests(); }

    @Test public void signalBackendObservesActionAndFramesPublishProjection() {
        FakeSignalBackend backend = new FakeSignalBackend("test");
        backend.installSignals();
        ContextFrame frame = ContextFrame.of(1L, "combat", Arrays.asList(
                CardView.builder(new CardRef("i1", "Strike_R")).build(),
                CardView.builder(new CardRef("i2", "Strike_R")).build()));
        backend.publish(frame);
        assertTrue(ArtFramework.publishFrame(backend.currentFrame()).applied);
        ArtFramework.emit(new UiSignal(ContextSignals.action(SurfaceIds.COMBAT_HAND, IntentNames.PLAY_CARD),
                "test", UiIntent.of(IntentNames.PLAY_CARD, SurfaceIds.COMBAT_HAND, new CardRef("i1", "Strike_R"))));
        assertEquals(1, backend.signalLog().size());
        assertEquals(IntentNames.PLAY_CARD, ((UiIntent) backend.signalLog().get(0).payload).name);
    }

    @Test public void framePublishDirectlyUpdatesProjectionWithoutSignalDispatch() {
        final int[] calls = new int[1];
        ArtFramework.connect(ContextSignals.FRAME_UPDATED, signal -> {
            calls[0]++;
            return SignalDecision.continueSignal();
        });
        PresentProjections.resetForTests();
        assertTrue(ArtFramework.publishFrame(ContextFrame.of(1L, "combat", null)).applied);
        assertEquals(0, ArtFramework.projection().size());
        org.junit.Assert.assertEquals(0, calls[0]);
    }

    @Test public void framePublishCannotBeBlockedBySignalListener() {
        ArtFramework.connect(ContextSignals.FRAME_UPDATED, signal -> SignalDecision.stopRejected("blocked"));
        PresentProjections.resetForTests();
        assertTrue(ArtFramework.publishFrame(ContextFrame.of(1L, "combat", null)).applied);
    }

    @Test public void staleAndUnavailableFramesPreserveProjectionRules() {
        assertTrue(ArtFramework.publishFrame(ContextFrame.of(5L, "combat", Arrays.asList(
                CardView.builder(new CardRef("x", "Defend_R")).build()))).applied);
        assertFalse(ArtFramework.publishFrame(ContextFrame.of(3L, "combat", null)).applied);
        assertNotNull(ArtFramework.projection().get("x"));
        assertFalse(ArtFramework.publishFrame(ContextFrame.unavailable(6L)).applied);
        assertTrue(ArtFramework.projection().isStale());
    }

    @Test public void sceneEpochClearsCardsAndDrag() {
        ArtFramework.publishFrame(ContextFrame.of(5L, 1L, "combat", Arrays.asList(
                CardView.builder(new CardRef("card", "Strike_R")).build()), ControlsView.empty(), MapView.empty(), null));
        EntityId entity = ArtFramework.projection().entityId("card");
        ArtFramework.projection().setDragInstanceId("card");
        assertTrue(ArtFramework.publishFrame(ContextFrame.of(1L, 2L, "map", null,
                ControlsView.empty(), MapView.empty(), null)).applied);
        assertEquals(0, ArtFramework.projection().size());
        assertNull(ArtFramework.projection().dragInstanceId());
        assertNull(ArtFramework.projection().entityId("card"));
        assertFalse(ArtFramework.projection().world().contains(entity));
    }

    @Test public void staleFrameWithinTheCurrentSceneEpochDoesNotMutateProjection() {
        assertTrue(ArtFramework.publishFrame(ContextFrame.of(5L, 2L, "map", Arrays.asList(
                CardView.builder(new CardRef("current", "Defend_R")).build()),
                ControlsView.empty(), MapView.empty(), null)).applied);
        ArtFramework.projection().setDragInstanceId("current");

        assertFalse(ArtFramework.publishFrame(ContextFrame.of(4L, 2L, "map", null,
                ControlsView.empty(), MapView.empty(), null)).applied);
        assertNotNull(ArtFramework.projection().get("current"));
        assertEquals("current", ArtFramework.projection().dragInstanceId());
    }

    @Test public void staleCrossEpochFrameDoesNotMutateProjection() {
        assertTrue(ArtFramework.publishFrame(ContextFrame.of(5L, 2L, "map", Arrays.asList(
                CardView.builder(new CardRef("current", "Defend_R")).build()),
                ControlsView.empty(), MapView.empty(), null)).applied);
        ArtFramework.projection().setDragInstanceId("current");

        assertFalse(ArtFramework.publishFrame(ContextFrame.of(99L, 1L, "combat", null,
                ControlsView.empty(), MapView.empty(), null)).applied);
        assertNotNull(ArtFramework.projection().get("current"));
        assertEquals("current", ArtFramework.projection().dragInstanceId());
        assertEquals(2L, ArtFramework.projection().sceneEpoch());
    }

    @Test public void cardProjectionStoresLongLivedStateInPresentationComponents() {
        CardView card = CardView.builder(new CardRef("instance", "Strike_R"))
                .zone(CardZone.HAND).slot(2).selected(true).art("card-art").frame("card-frame").build();
        assertTrue(ArtFramework.publishFrame(ContextFrame.of(1L, "combat", Arrays.asList(card))).applied);

        EntityId entity = ArtFramework.projection().entityId("instance");
        assertNotNull(entity);
        assertEquals(2, ArtFramework.projection().world().entities().size());
        assertEquals("Strike_R", ArtFramework.projection().world()
                .get(entity, CardIdentityComponent.class).cardId);
        assertEquals(2, ArtFramework.projection().world()
                .get(entity, CardPlacementComponent.class).slotIndex);
        assertTrue(ArtFramework.projection().world()
                .get(entity, CardInteractionComponent.class).selected);
        assertEquals("card-art", ArtFramework.projection().world()
                .get(entity, CardAssetsComponent.class).artResourceId);
    }

    @Test public void projectionFrameLifecycleIsStoredOnItsRootEntity() {
        assertTrue(ArtFramework.publishFrame(ContextFrame.of(7L, 3L, "combat", null,
                ControlsView.empty(), MapView.empty(), null)).applied);

        EntityId root = ArtFramework.projection().world().query(ProjectionFrameComponent.class).get(0);
        ProjectionFrameComponent metadata = ArtFramework.projection().world()
                .get(root, ProjectionFrameComponent.class);
        assertEquals(7L, metadata.frameId);
        assertEquals(3L, metadata.sceneEpoch);
        assertEquals("combat", metadata.scene);
        assertTrue(metadata.available);
        assertFalse(metadata.stale);
    }

    @Test public void projectionDragLifecycleIsStoredOnItsRootEntity() {
        ArtFramework.projection().setDragInstanceId("card");

        EntityId root = ArtFramework.projection().world().query(ProjectionInteractionComponent.class).get(0);
        assertEquals("card", ArtFramework.projection().world()
                .get(root, ProjectionInteractionComponent.class).dragInstanceId);

        ArtFramework.projection().clearDrag();
        assertNull(ArtFramework.projection().world()
                .get(root, ProjectionInteractionComponent.class).dragInstanceId);
    }

    @Test public void projectionFrameSnapshotIsStoredOnItsRootEntity() {
        ContextFrame frame = ContextFrame.of(8L, 4L, "event", null,
                ControlsView.empty(), MapView.empty(), null);
        assertTrue(ArtFramework.publishFrame(frame).applied);

        EntityId root = ArtFramework.projection().world()
                .query(ProjectionFrameSnapshotComponent.class).get(0);
        assertSame(frame, ArtFramework.projection().world()
                .get(root, ProjectionFrameSnapshotComponent.class).frame);
        assertSame(frame, ArtFramework.projection().lastFrame());
    }

    @Test public void removedCardDestroysItsPresentationEntity() {
        assertTrue(ArtFramework.publishFrame(ContextFrame.of(1L, "combat", Arrays.asList(
                CardView.builder(new CardRef("instance", "Strike_R")).build()))).applied);
        EntityId entity = ArtFramework.projection().entityId("instance");
        assertNotNull(entity);

        assertTrue(ArtFramework.publishFrame(ContextFrame.of(2L, "combat", null)).applied);
        assertNull(ArtFramework.projection().entityId("instance"));
        assertFalse(ArtFramework.projection().world().contains(entity));
        assertEquals(1, ArtFramework.projection().world().entities().size());
    }

    @Test public void cardEntityLookupUsesPresentationIdentityAfterProjectionSync() {
        assertTrue(ArtFramework.publishFrame(ContextFrame.of(1L, "combat", Arrays.asList(
                CardView.builder(new CardRef("lookup", "Strike_R")).build()))).applied);

        EntityId entity = ArtFramework.projection().entityId("lookup");
        assertNotNull(entity);
        assertEquals(entity, ArtFramework.projection().world().query(CardIdentityComponent.class).get(0));
    }
}
