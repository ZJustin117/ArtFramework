package artframework.context;

import artframework.api.ArtFramework;
import artframework.core.SignalGroups;
import artframework.core.TransientSignalPaths;
import artframework.core.TransientSignalRuntime;
import artframework.core.UiSignal;
import artframework.ecs.EntityId;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;
import artframework.sts1.input.NativeInputRecords;
import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NativeIntentLifecycleSystemTest {
    @Before public void setUp() { ArtFramework.resetForTests(); }
    @org.junit.After public void tearDown() { ArtFramework.resetForTests(); }

    @Test public void intentWritesLifecycleStateImmediately() {
        NativeInputRecords.intent(SurfaceIds.COMBAT_HAND, "play_card",
                NativeIntentLifecycleComponent.State.EXECUTED, "accepted");
        assertState(inputEntity(), NativeIntentLifecycleComponent.State.EXECUTED);
    }

    @Test public void requestedAndSentClearOldObservation() {
        NativeInputRecords.observe(SurfaceIds.COMBAT_HAND, 5L, 2L, "combat");
        NativeInputRecords.intent(SurfaceIds.COMBAT_HAND, "play_card",
                NativeIntentLifecycleComponent.State.REQUESTED, "");
        assertNull(PresentationRegistry.world().get(inputEntity(), NativeIntentObservationComponent.class));
        NativeInputRecords.observe(SurfaceIds.COMBAT_HAND, 6L, 2L, "combat");
        NativeInputRecords.intent(SurfaceIds.COMBAT_HAND, "play_card",
                NativeIntentLifecycleComponent.State.SENT, "");
        assertNull(PresentationRegistry.world().get(inputEntity(), NativeIntentObservationComponent.class));
    }

    @Test public void observationsStillConfirmOrFailDurableExecutedLifecycle() {
        NativeInputRecords.intent(SurfaceIds.COMBAT_HAND, "play_card",
                NativeIntentLifecycleComponent.State.EXECUTED, "accepted");
        NativeInputRecords.observe(SurfaceIds.COMBAT_HAND, 7L, 3L, "combat");
        assertState(inputEntity(), NativeIntentLifecycleComponent.State.CONFIRMED);

        NativeInputRecords.intent(SurfaceIds.COMBAT_HAND, "play_card",
                NativeIntentLifecycleComponent.State.EXECUTED, "accepted");
        NativeInputRecords.failed(SurfaceIds.COMBAT_HAND, 8L, "authority frame unavailable");
        assertState(inputEntity(), NativeIntentLifecycleComponent.State.FAILED);
        assertEquals("authority frame unavailable",
                PresentationRegistry.world().get(inputEntity(), NativeIntentLifecycleComponent.class)
                        .message);
    }

    @Test public void invalidPayloadAndMissingContextAreRejectedWithoutStateWrite() {
        PresentationRegistry.close("sts1-input");
        SignalGroups.transientRuntimeRawGroup().dispatch(new UiSignal(
                SignalGroups.TRANSIENT_RUNTIME, null, TransientSignalPaths.NATIVE_INTENT_LIFECYCLE,
                "invalid", java.util.Collections.emptyMap(), null));
        assertNull(PresentationRegistry.existingContext("sts1-input"));
    }

    @Test public void missingKeyedInputEntityIsRejectedWithoutLifecycleWrite() {
        SignalGroups.resetForTests();
        TransientSignalRuntime runtime = new TransientSignalRuntime(
                SignalGroups.isolatedTransientRuntimeGroup(), false);
        new NativeIntentLifecycleSystem(runtime);
        PresentationContext context = PresentationRegistry.context("sts1-input");
        EntityId unrelatedEntity = context.create(
                new PresentationKey("sts1.input", "other-surface"),
                "other-surface", "surface", "test");
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("surfaceId", SurfaceIds.COMBAT_HAND);
        payload.put("name", "play_card");
        payload.put("state", NativeIntentLifecycleComponent.State.EXECUTED.name());
        payload.put("message", "accepted");

        artframework.core.SignalDispatchResult result = runtime.dispatch(
                TransientSignalPaths.NATIVE_INTENT_LIFECYCLE, "sts1-input", payload);

        assertEquals(Boolean.TRUE, Boolean.valueOf(result.isRejected()));
        assertNull(context.world().get(unrelatedEntity, NativeIntentLifecycleComponent.class));
    }

    @Test public void resetRecreatesScheduleOwnedLifecycleConsumer() {
        NativeInputRecords.intent(SurfaceIds.COMBAT_HAND, "play_card",
                NativeIntentLifecycleComponent.State.EXECUTED, "accepted");
        ArtFramework.resetForTests();
        NativeInputRecords.intent(SurfaceIds.COMBAT_HAND, "play_card",
                NativeIntentLifecycleComponent.State.REJECTED, "rejected");
        assertState(inputEntity(), NativeIntentLifecycleComponent.State.REJECTED);
    }

    private static EntityId inputEntity() {
        return PresentationRegistry.context("sts1-input").entity(
                new artframework.presentation.PresentationKey("sts1.input", SurfaceIds.COMBAT_HAND));
    }

    private static void assertState(EntityId entity,
            NativeIntentLifecycleComponent.State expected) {
        assertEquals(expected, PresentationRegistry.world().get(entity,
                NativeIntentLifecycleComponent.class).state);
    }
}
