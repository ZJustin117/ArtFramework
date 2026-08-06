package artframework.sts1.input;

import artframework.api.ArtFramework;
import artframework.context.CardRef;
import artframework.context.CardView;
import artframework.context.CardZone;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.FakeSignalBackend;
import artframework.context.IntentNames;
import artframework.context.IntentResult;
import artframework.context.MapView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CombatInputRouterTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        CombatInputRouter.resetForTests();
    }

    private void mountHand(FakeSignalBackend backend) {
        backend.installSignals();
        backend.publish(
                ContextFrame.of(
                        1L,
                        1L,
                        "combat",
                        Arrays.asList(
                                CardView.builder(new CardRef("h1", "Strike_R"))
                                        .zone(CardZone.HAND)
                                        .slot(0)
                                        .build()),
                        ControlsView.combat(3, 1, 0, 0, 0, true, true),
                        MapView.empty(),
                        null));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.ops().invoke(SurfaceIds.COMBAT_SURFACE, "mount_combat");
    }

    @Test
    public void rejectsWhenPresentOff() {
        FakeSignalBackend backend = new FakeSignalBackend();
        mountHand(backend);
        IntentResult r = CombatInputRouter.beginDrag("h1");
        assertEquals(IntentResult.Status.REJECTED, r.status);
        assertTrue(r.message.contains("not enabled"));
    }

    @Test
    public void fullRoutesToRecordingExecutor() {
        FakeSignalBackend backend = new FakeSignalBackend();
        mountHand(backend);
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        RecordingIntentExecutor rec = new RecordingIntentExecutor();
        CombatInputRouter.setExecutor(rec);
        IntentResult direct =
                CombatInputRouter.executeIfOwned(
                        artframework.context.UiIntent.of(
                                IntentNames.BEGIN_DRAG, SurfaceIds.COMBAT_HAND, "h1"));
        assertTrue(direct.isAccepted());
        assertEquals(1, rec.log().size());
        assertEquals(IntentNames.BEGIN_DRAG, rec.log().get(0).name);
    }

    @Test
    public void beginDragViaRouterWithFakeBackend() {
        FakeSignalBackend backend = new FakeSignalBackend();
        mountHand(backend);
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        IntentResult r = CombatInputRouter.beginDrag("h1");
        assertTrue(r.isAccepted());
        assertEquals(1, backend.signalLog().size());
    }

    @Test
    public void executeIfOwnedRejectsWithoutFull() {
        IntentResult r =
                CombatInputRouter.executeIfOwned(
                        artframework.context.UiIntent.of(IntentNames.PLAY_CARD, SurfaceIds.COMBAT_HAND, "h1"));
        assertEquals(IntentResult.Status.REJECTED, r.status);
    }

    @Test
    public void unknownInstanceRejected() {
        FakeSignalBackend backend = new FakeSignalBackend();
        mountHand(backend);
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        IntentResult r = CombatInputRouter.beginDrag("missing");
        assertEquals(IntentResult.Status.REJECTED, r.status);
        assertTrue(r.message.contains("unknown"));
    }

    @Test
    public void suppressNativeInputRequiresFullMountCombat() {
        FakeSignalBackend backend = new FakeSignalBackend();
        mountHand(backend);
        CombatInputRouter.setSuppressNativeInput(true);
        assertFalse(CombatInputRouter.shouldSuppressNativeInput());
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        assertFalse(CombatInputRouter.shouldSuppressNativeInput());
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        assertTrue(CombatInputRouter.shouldSuppressNativeInput());
    }

    @Test
    public void pressEndTurnNeedsControlsFull() {
        FakeSignalBackend backend = new FakeSignalBackend();
        mountHand(backend);
        IntentResult off = CombatInputRouter.pressEndTurn();
        assertEquals(IntentResult.Status.REJECTED, off.status);
        FullPresentMode.setCombatControlsLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        IntentResult on = CombatInputRouter.pressEndTurn();
        assertTrue(on.isAccepted());
    }

    @Test
    public void playCardRef() {
        FakeSignalBackend backend = new FakeSignalBackend();
        mountHand(backend);
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        IntentResult r = CombatInputRouter.playCard(new CardRef("h1", "Strike_R"), "m1");
        assertTrue(r.isAccepted());
    }

    @Test
    public void eventSurfaceRequiresEventProjectionScene() {
        assertFalse(CombatInputRouter.sceneReadyFor(SurfaceIds.EVENT, "", true));
        assertFalse(CombatInputRouter.sceneReadyFor(SurfaceIds.EVENT, "map", true));
        assertTrue(CombatInputRouter.sceneReadyFor(SurfaceIds.EVENT, "event", true));
    }
}
