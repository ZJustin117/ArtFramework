package artframework.context;

import java.util.Arrays;
import artframework.ecs.EntityId;
import artframework.api.ArtFramework;
import artframework.api.FrameworkTransientFixture;
import artframework.core.TransientSignalPaths;
import artframework.core.TransientSignalRuntime;
import artframework.presentation.PresentationRegistry;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertEquals;

public class BusinessConfirmationSystemTest {
    private FrameworkTransientFixture transientFixture;
    @Before public void setUp() { ArtFramework.resetForTests(); transientFixture = new FrameworkTransientFixture(); }
    @org.junit.After public void tearDown() { transientFixture.close(); ArtFramework.resetForTests(); }
    @Test public void cardChangeConfirmsPlay() {
        CardView one = CardView.builder(new CardRef("a", "strike"))
                .zone(CardZone.HAND).slot(0).build();
        CardView two = CardView.builder(new CardRef("b", "defend"))
                .zone(CardZone.HAND).slot(0).build();
        ContextFrame before = new ContextFrame(1L, 2L, "combat", Arrays.asList(one),
                ControlsView.empty(), MapView.empty(), true, null);
        ContextFrame after = new ContextFrame(2L, 2L, "combat", Arrays.asList(two),
                ControlsView.empty(), MapView.empty(), true, null);
        BusinessConfirmationComponent request = new BusinessConfirmationComponent("play_card",
                BusinessConfirmationComponent.Domain.CARD, BusinessConfirmationComponent.State.PENDING,
                1L, 2L, -1L, -1L, "");
        assertEquals(BusinessConfirmationComponent.State.CONFIRMED,
                BusinessConfirmationSystem.evaluate(request, before, after).state);
    }

    @Test public void unchangedSnapshotRemainsPending() {
        ContextFrame frame = new ContextFrame(1L, 2L, "event", null,
                ControlsView.empty(), MapView.empty(), EventView.empty(), SelectView.empty(),
                true, ViewportView.unavailable());
        BusinessConfirmationComponent request = new BusinessConfirmationComponent("choose_event_option",
                BusinessConfirmationComponent.Domain.EVENT, BusinessConfirmationComponent.State.PENDING,
                1L, 2L, -1L, -1L, "");
        assertEquals(BusinessConfirmationComponent.State.PENDING,
                BusinessConfirmationSystem.evaluate(request, frame, frame).state);
    }

    @Test public void signalConsumesFramePairAndFailsPendingRequestWhenAuthorityIsUnavailable() {
        EntityId requestEntity = PresentationRegistry.world().createEntity();
        PresentationRegistry.world().put(requestEntity, BusinessConfirmationComponent.class,
                new BusinessConfirmationComponent("play_card",
                        BusinessConfirmationComponent.Domain.CARD,
                        BusinessConfirmationComponent.State.PENDING,
                        1L, 2L, -1L, -1L, ""));
        TransientSignalRuntime runtime = new TransientSignalRuntime(
                artframework.core.SignalGroups.isolatedTransientRuntimeGroup(), false);
        new BusinessConfirmationSystem(runtime);
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("before", null);
        payload.put("after", ContextFrame.unavailable(3L));

        runtime.dispatch(TransientSignalPaths.AUTHORITY_BUSINESS_CONFIRMATION,
                "presentation-schedule", payload);

        assertEquals(BusinessConfirmationComponent.State.FAILED,
                PresentationRegistry.world().get(requestEntity, BusinessConfirmationComponent.class).state);
    }

    @Test public void scheduleConfirmationSignalConfirmsAndLeavesUnchangedRequestPending() {
        ArtFramework.publishFrame(ContextFrame.of(1L, "combat", java.util.Arrays.asList(
                CardView.builder(new CardRef("before", "Strike")).build())));
        EntityId confirmed = PresentationRegistry.world().createEntity();
        PresentationRegistry.world().put(confirmed, BusinessConfirmationComponent.class,
                pending(1L, 0L));
        ArtFramework.publishFrame(ContextFrame.of(2L, "combat", java.util.Arrays.asList(
                CardView.builder(new CardRef("after", "Defend")).build())));
        assertEquals(BusinessConfirmationComponent.State.CONFIRMED,
                PresentationRegistry.world().get(confirmed, BusinessConfirmationComponent.class).state);

        EntityId pending = PresentationRegistry.world().createEntity();
        PresentationRegistry.world().put(pending, BusinessConfirmationComponent.class,
                pending(2L, 0L));
        ArtFramework.publishFrame(ContextFrame.of(3L, "combat", java.util.Arrays.asList(
                CardView.builder(new CardRef("after", "Defend")).build())));
        assertEquals(BusinessConfirmationComponent.State.PENDING,
                PresentationRegistry.world().get(pending, BusinessConfirmationComponent.class).state);
    }

    @Test public void invalidPayloadAndResetAreSafe() {
        TransientSignalRuntime runtime = new TransientSignalRuntime(
                artframework.core.SignalGroups.isolatedTransientRuntimeGroup(), false);
        new BusinessConfirmationSystem(runtime);
        runtime.dispatch(TransientSignalPaths.AUTHORITY_BUSINESS_CONFIRMATION,
                "presentation-schedule", java.util.Collections.emptyMap());
        ArtFramework.resetForTests();
        ArtFramework.publishFrame(ContextFrame.of(4L, "combat", null));
        assertEquals(4L, ArtFramework.projection().lastFrameId());
    }

    @Test public void rejectedAuthorityDoesNotDeliverConfirmationPair() {
        EntityId requestEntity = PresentationRegistry.world().createEntity();
        PresentationRegistry.world().put(requestEntity, BusinessConfirmationComponent.class, pending());
         transientFixture.connect(
                TransientSignalPaths.AUTHORITY_FRAME,
                signal -> artframework.core.SignalDecision.stopRejected("blocked"));

        ArtFramework.publishFrame(ContextFrame.of(4L, "combat", null));

        assertEquals(BusinessConfirmationComponent.State.PENDING,
                PresentationRegistry.world().get(requestEntity, BusinessConfirmationComponent.class).state);
    }

    private static BusinessConfirmationComponent pending() {
        return pending(1L, 0L);
    }

    private static BusinessConfirmationComponent pending(long requestedFrame, long requestedEpoch) {
        return new BusinessConfirmationComponent("play_card", BusinessConfirmationComponent.Domain.CARD,
                BusinessConfirmationComponent.State.PENDING, requestedFrame, requestedEpoch,
                -1L, -1L, "");
    }
}
