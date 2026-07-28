package artframework.sts1.input;

import artframework.api.ArtFramework;
import artframework.context.CardRef;
import artframework.context.CardView;
import artframework.context.CardZone;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.FakeBackend;
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

    private void mountHand(FakeBackend backend) {
        ArtFramework.bindPresentationBackend(backend);
        backend.pushFrame(
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
        ArtFramework.frames().syncFromBackend();
        ArtFramework.ops().invoke(SurfaceIds.COMBAT_SURFACE, "mount_combat");
    }

    @Test
    public void rejectsWhenPresentOff() {
        FakeBackend backend = new FakeBackend();
        mountHand(backend);
        IntentResult r = CombatInputRouter.beginDrag("h1");
        assertEquals(IntentResult.Status.REJECTED, r.status);
        assertTrue(r.message.contains("not enabled"));
    }

    @Test
    public void fullRoutesToRecordingExecutor() {
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
        FakeBackend backend = new FakeBackend();
        mountHand(backend);
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        IntentResult r = CombatInputRouter.beginDrag("h1");
        assertTrue(r.isAccepted());
        assertEquals(1, backend.intentLog().size());
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
        FakeBackend backend = new FakeBackend();
        mountHand(backend);
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        IntentResult r = CombatInputRouter.beginDrag("missing");
        assertEquals(IntentResult.Status.REJECTED, r.status);
        assertTrue(r.message.contains("unknown"));
    }

    @Test
    public void suppressNativeInputRequiresFullMountCombat() {
        FakeBackend backend = new FakeBackend();
        mountHand(backend);
        CombatInputRouter.setSuppressNativeInput(true);
        assertFalse(CombatInputRouter.shouldSuppressNativeInput());
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        assertTrue(CombatInputRouter.shouldSuppressNativeInput());
    }

    @Test
    public void pressEndTurnNeedsControlsFull() {
        FakeBackend backend = new FakeBackend();
        mountHand(backend);
        IntentResult off = CombatInputRouter.pressEndTurn();
        assertEquals(IntentResult.Status.REJECTED, off.status);
        FullPresentMode.setCombatControlsLevel(PresentLevel.FULL);
        IntentResult on = CombatInputRouter.pressEndTurn();
        assertTrue(on.isAccepted());
    }

    @Test
    public void playCardRef() {
        FakeBackend backend = new FakeBackend();
        mountHand(backend);
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        IntentResult r = CombatInputRouter.playCard(new CardRef("h1", "Strike_R"), "m1");
        assertTrue(r.isAccepted());
    }
}
