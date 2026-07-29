package artframework.context;

import artframework.api.ArtFramework;
import artframework.core.SignalDecision;
import artframework.core.UiSignal;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

    @Test public void frameReplacementReachesLaterProjectionListener() {
        ArtFramework.connect(ContextSignals.FRAME_UPDATED, signal -> SignalDecision.replace(signal.replace(
                ContextFrame.of(((ContextFrame) signal.payload).frameId, "combat", Arrays.asList(
                        CardView.builder(new CardRef("shown", "Bash")).build())))));
        PresentProjections.resetForTests();
        assertTrue(ArtFramework.publishFrame(ContextFrame.of(1L, "combat", null)).applied);
        assertNotNull(ArtFramework.projection().get("shown"));
    }

    @Test public void stopRejectedPreventsLaterProjectionListener() {
        ArtFramework.connect(ContextSignals.FRAME_UPDATED, signal -> SignalDecision.stopRejected("blocked"));
        PresentProjections.resetForTests();
        assertFalse(ArtFramework.publishFrame(ContextFrame.of(1L, "combat", null)).applied);
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
        ArtFramework.projection().setDragInstanceId("card");
        assertTrue(ArtFramework.publishFrame(ContextFrame.of(1L, 2L, "map", null,
                ControlsView.empty(), MapView.empty(), null)).applied);
        assertEquals(0, ArtFramework.projection().size());
        assertNull(ArtFramework.projection().dragInstanceId());
    }
}
