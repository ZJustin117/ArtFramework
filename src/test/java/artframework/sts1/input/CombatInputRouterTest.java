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
import artframework.context.NativeInputComponent;
import artframework.context.NativeInterceptComponent;
import artframework.context.NativeIntentLifecycleComponent;
import artframework.context.NativeIntentObservationComponent;
import artframework.context.SurfaceIds;
import artframework.ecs.EntityId;
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
    public void executorResultIsStoredOnItsSurfaceInputEntity() {
        FakeSignalBackend backend = new FakeSignalBackend();
        mountHand(backend);
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        assertTrue(CombatInputRouter.executeIfOwned(artframework.context.UiIntent.of(
                IntentNames.BEGIN_DRAG, SurfaceIds.COMBAT_HAND, "h1")).isAccepted());

        EntityId entity = artframework.presentation.PresentationRegistry.context("sts1-input")
                .entity(new artframework.presentation.PresentationKey("sts1.input", SurfaceIds.COMBAT_HAND));
        NativeInterceptComponent intercept = artframework.presentation.PresentationRegistry.world()
                .get(entity, NativeInterceptComponent.class);
        NativeIntentLifecycleComponent lifecycle = artframework.presentation.PresentationRegistry.world()
                .get(entity, NativeIntentLifecycleComponent.class);
        assertTrue(intercept.processed);
        assertEquals("executed", intercept.reason);
        assertEquals(IntentNames.BEGIN_DRAG, lifecycle.name);
        assertEquals(NativeIntentLifecycleComponent.State.EXECUTED, lifecycle.state);
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
    public void routedInputAndInterceptDecisionAreStoredInEcs() {
        FakeSignalBackend backend = new FakeSignalBackend();
        mountHand(backend);
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        IntentResult result = CombatInputRouter.beginDrag("missing");
        assertEquals(IntentResult.Status.REJECTED, result.status);
        EntityId entity = artframework.presentation.PresentationRegistry.context("sts1-input")
                .world().query(NativeInputComponent.class).get(0);
        NativeInputComponent input = artframework.presentation.PresentationRegistry.world()
                .get(entity, NativeInputComponent.class);
        NativeInterceptComponent intercept = artframework.presentation.PresentationRegistry.world()
                .get(entity, NativeInterceptComponent.class);
        NativeIntentLifecycleComponent lifecycle = artframework.presentation.PresentationRegistry.world()
                .get(entity, NativeIntentLifecycleComponent.class);
        assertEquals(IntentNames.BEGIN_DRAG, input.name);
        assertFalse(intercept.processed);
        assertEquals("unknown_card", intercept.reason);
        assertEquals(NativeIntentLifecycleComponent.State.REJECTED, lifecycle.state);
    }

    @Test
    public void nextAuthorityFrameIsRecordedAfterNativeIntent() {
        FakeSignalBackend backend = new FakeSignalBackend();
        mountHand(backend);
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        CombatInputRouter.beginDrag("h1");
        ArtFramework.publishFrame(ContextFrame.of(2L, 1L, "combat", null,
                ControlsView.empty(), MapView.empty(), null));

        EntityId entity = artframework.presentation.PresentationRegistry.context("sts1-input")
                .entity(new artframework.presentation.PresentationKey("sts1.input", SurfaceIds.COMBAT_HAND));
        NativeIntentObservationComponent observation = artframework.presentation.PresentationRegistry.world()
                .get(entity, NativeIntentObservationComponent.class);
        NativeIntentLifecycleComponent lifecycle = artframework.presentation.PresentationRegistry.world()
                .get(entity, NativeIntentLifecycleComponent.class);
        assertEquals(2L, observation.frameId);
        assertEquals("combat", observation.scene);
        assertEquals(NativeIntentLifecycleComponent.State.CONFIRMED, lifecycle.state);
    }

    @Test
    public void unavailableAuthorityFrameFailsExecutedIntent() {
        FakeSignalBackend backend = new FakeSignalBackend();
        mountHand(backend);
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        CombatInputRouter.beginDrag("h1");
        ArtFramework.publishFrame(ContextFrame.unavailable(2L));

        EntityId entity = artframework.presentation.PresentationRegistry.context("sts1-input")
                .entity(new artframework.presentation.PresentationKey("sts1.input", SurfaceIds.COMBAT_HAND));
        NativeIntentLifecycleComponent lifecycle = artframework.presentation.PresentationRegistry.world()
                .get(entity, NativeIntentLifecycleComponent.class);
        assertEquals(NativeIntentLifecycleComponent.State.FAILED, lifecycle.state);
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
    public void endTurnInputAndInterceptAreStoredOnControlsEntity() {
        FakeSignalBackend backend = new FakeSignalBackend();
        mountHand(backend);
        IntentResult result = CombatInputRouter.pressEndTurn();
        assertEquals(IntentResult.Status.REJECTED, result.status);

        EntityId entity = artframework.presentation.PresentationRegistry.context("sts1-input")
                .entity(new artframework.presentation.PresentationKey("sts1.input", SurfaceIds.COMBAT_CONTROLS));
        NativeInputComponent input = artframework.presentation.PresentationRegistry.world()
                .get(entity, NativeInputComponent.class);
        NativeInterceptComponent intercept = artframework.presentation.PresentationRegistry.world()
                .get(entity, NativeInterceptComponent.class);
        assertEquals(IntentNames.PRESS_END_TURN, input.name);
        assertEquals(SurfaceIds.COMBAT_CONTROLS, input.surfaceId);
        assertEquals("controls_not_owned", intercept.reason);
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
