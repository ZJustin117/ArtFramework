package artframework.context;

import artframework.api.ArtFramework;
import artframework.api.UiOpResult;
import artframework.core.UiSignalInterceptor;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BackendContextTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void fakeBackendPushFrameAndIntentLog() {
        FakeBackend backend = new FakeBackend("test");
        ArtFramework.bindPresentationBackend(backend);

        CardRef a = new CardRef("i1", "Strike_R");
        CardRef b = new CardRef("i2", "Strike_R");
        ContextFrame frame =
                ContextFrame.of(
                        1L,
                        "combat",
                        Arrays.asList(
                                CardView.builder(a).zone(CardZone.HAND).slot(0).pose(CardPose.at(10, 20)).art(
                                        artframework.assets.ResourceIds.cardArt("Strike_R")).build(),
                                CardView.builder(b).zone(CardZone.HAND).slot(1).pose(CardPose.at(30, 20)).art(
                                        artframework.assets.ResourceIds.cardArt("Strike_R")).build()));
        backend.pushFrame(frame);

        FrameDiff diff = ArtFramework.frames().syncFromBackend();
        assertTrue(diff.applied);
        assertEquals(2, diff.added.size());
        assertEquals(2, ArtFramework.projection().size());
        assertEquals(2, ArtFramework.projection().listZone(CardZone.HAND).size());
        assertNotNull(ArtFramework.projection().get("i1"));
        assertNotNull(ArtFramework.projection().get("i2"));
        assertEquals("Strike_R", ArtFramework.projection().get("i1").cardId);

        IntentResult r =
                ArtFramework.submitIntent(UiIntent.of(IntentNames.PLAY_CARD, SurfaceIds.COMBAT_HAND, a));
        assertTrue(r.isAccepted());
        assertEquals(1, backend.intentLog().size());
        assertEquals(IntentNames.PLAY_CARD, backend.intentLog().get(0).name);
    }

    @Test
    public void staleFrameSkipped() {
        FakeBackend backend = new FakeBackend();
        ArtFramework.bindPresentationBackend(backend);
        backend.pushFrame(ContextFrame.of(5L, "combat", Arrays.asList(
                CardView.builder(new CardRef("x", "Defend_R")).build())));
        assertTrue(ArtFramework.frames().syncFromBackend().applied);

        FrameDiff stale =
                ArtFramework.applyFrame(ContextFrame.of(3L, "combat", Arrays.asList(
                        CardView.builder(new CardRef("y", "Bash")).build())));
        assertFalse(stale.applied);
        assertEquals(1, ArtFramework.projection().size());
        assertNotNull(ArtFramework.projection().get("x"));
    }

    @Test
    public void multiInstanceSameCardId() {
        ContextFrame frame =
                ContextFrame.of(
                        1L,
                        "combat",
                        Arrays.asList(
                                CardView.builder(new CardRef("a", "Strike_R")).slot(0).build(),
                                CardView.builder(new CardRef("b", "Strike_R")).slot(1).build()));
        FrameDiff d = ArtFramework.applyFrame(frame);
        assertTrue(d.applied);
        assertEquals(2, ArtFramework.projection().listZone(CardZone.HAND).size());
        assertFalse(
                ArtFramework.projection()
                        .get("a")
                        .instanceId
                        .equals(ArtFramework.projection().get("b").instanceId));
    }

    @Test
    public void intentInterceptorBlocks() {
        FakeBackend backend = new FakeBackend();
        ArtFramework.bindPresentationBackend(backend);
        final AtomicInteger hits = new AtomicInteger();
        ArtFramework.frames()
                .addIntentInterceptor(
                        new UiSignalInterceptor() {
                            @Override
                            public Result intercept(
                                    String windowId, String controlId, String signal, Object... args) {
                                hits.incrementAndGet();
                                if (IntentNames.BEGIN_DRAG.equals(signal)) {
                                    return Result.BLOCK;
                                }
                                return Result.ALLOW;
                            }
                        });
        IntentResult blocked =
                ArtFramework.submitIntent(
                        UiIntent.of(IntentNames.BEGIN_DRAG, SurfaceIds.COMBAT_HAND, "i1"));
        assertEquals(IntentResult.Status.REJECTED, blocked.status);
        assertTrue(blocked.message.startsWith("blocked:"));
        assertEquals(0, backend.intentLog().size());
        assertEquals(1, hits.get());
    }

    @Test
    public void readOnlyBackendRejectsIntents() {
        FakeBackend backend = new FakeBackend().setMode(BackendMode.READ_ONLY);
        ArtFramework.bindPresentationBackend(backend);
        IntentResult r =
                ArtFramework.submitIntent(UiIntent.of(IntentNames.PRESS_END_TURN, SurfaceIds.COMBAT_CONTROLS));
        assertEquals(IntentResult.Status.REJECTED, r.status);
        assertEquals(1, backend.intentLog().size());
    }

    @Test
    public void removeCardClearsDrag() {
        ArtFramework.applyFrame(
                ContextFrame.of(
                        1L,
                        "combat",
                        Arrays.asList(CardView.builder(new CardRef("d1", "Strike_R")).build())));
        ArtFramework.projection().setDragInstanceId("d1");
        ArtFramework.applyFrame(ContextFrame.of(2L, "combat", Arrays.<CardView>asList()));
        assertNull(ArtFramework.projection().dragInstanceId());
        assertEquals(0, ArtFramework.projection().size());
    }

    @Test
    public void unavailableMarksStale() {
        ArtFramework.applyFrame(
                ContextFrame.of(
                        1L,
                        "combat",
                        Arrays.asList(CardView.builder(new CardRef("k", "Strike_R")).build())));
        FrameDiff d = ArtFramework.applyFrame(ContextFrame.unavailable(2L));
        assertFalse(d.applied);
        assertTrue(ArtFramework.projection().isStale());
        assertEquals(1, ArtFramework.projection().size());
    }

    @Test
    public void sceneEpochClearsPreviousCardsAndDrag() {
        ArtFramework.applyFrame(
                ContextFrame.of(
                        5L,
                        1L,
                        "combat",
                        Arrays.asList(CardView.builder(new CardRef("combat-card", "Strike_R")).build()),
                        ControlsView.empty(),
                        MapView.empty(),
                        new ViewportView(1920, 1080, 1920, 1080)));
        ArtFramework.projection().setDragInstanceId("combat-card");

        FrameDiff diff =
                ArtFramework.applyFrame(
                        ContextFrame.of(
                                1L,
                                2L,
                                "map",
                                Arrays.<CardView>asList(),
                                ControlsView.empty(),
                                MapView.empty(),
                                new ViewportView(1920, 1080, 1920, 1080)));
        assertTrue(diff.applied);
        assertEquals(0, ArtFramework.projection().size());
        assertNull(ArtFramework.projection().dragInstanceId());
        assertEquals(2L, ArtFramework.projection().sceneEpoch());
    }
}
