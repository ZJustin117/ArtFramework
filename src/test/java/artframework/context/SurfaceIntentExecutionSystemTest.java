package artframework.context;

import artframework.api.ArtFramework;
import artframework.api.FrameworkTransientFixture;
import artframework.core.SignalDecision;
import artframework.core.SignalGroups;
import artframework.core.TransientSignalPaths;
import artframework.core.TransientSignalRuntime;
import artframework.core.UiSignal;
import artframework.ecs.EntityId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class SurfaceIntentExecutionSystemTest {
    private FrameworkTransientFixture transientFixture;
    @Before public void setUp() { ArtFramework.resetForTests(); transientFixture = new FrameworkTransientFixture(); }
    @After public void tearDown() { transientFixture.close(); ArtFramework.resetForTests(); }

    @Test public void scheduleDispatchWritesImmediateResultWithoutExecutionComponent() {
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        SignalGroups.nativeGroup().connect(ContextSignals.action(SurfaceIds.COMBAT_HAND, "custom"),
                signal -> SignalDecision.continueSignal());

        ArtFramework.dispatchSurfaceIntent("custom", SurfaceIds.COMBAT_HAND, "argument");

        EntityId entity = currentHand();
        assertEquals(IntentResult.Status.ACCEPTED,
                PresentSurfaces.world().get(entity, SurfaceResultComponent.class).status);
    }

    @Test public void typedReplacementReachesConsumerAndDenormalizesReferences() {
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        final AtomicReference<UiSignal> observed = new AtomicReference<UiSignal>();
        final AtomicReference<UiIntent> nativeIntent = new AtomicReference<UiIntent>();
         transientFixture.connect(TransientSignalPaths.SURFACE_INTENT, signal -> {
            Map<String, Object> replacement = payload("custom", SurfaceIds.COMBAT_HAND,
                    SurfaceIntentExecutionSystem.normalizeArgs(
                            new CardRef("replacement", "Strike")));
            return SignalDecision.replace(signal.replace(replacement));
        });
         transientFixture.connect(TransientSignalPaths.SURFACE_INTENT,
                signal -> { observed.set(signal); return SignalDecision.continueSignal(); });
        SignalGroups.nativeGroup().connect(ContextSignals.action(SurfaceIds.COMBAT_HAND, "custom"),
                signal -> { nativeIntent.set((UiIntent) signal.payload); return SignalDecision.continueSignal(); });

        ArtFramework.dispatchSurfaceIntent("ignored", SurfaceIds.COMBAT_HAND,
                new CardRef("original", "Defend"));

        assertEquals("c2-surfaces", observed.get().source);
        assertEquals("custom", ((Map<?, ?>) observed.get().payload).get("name"));
        assertEquals(new CardRef("replacement", "Strike"), nativeIntent.get().args[0]);
    }

    @Test public void replacementCannotRetargetIntentToAnotherSurface() {
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        ArtFramework.component(SurfaceIds.COMBAT_CONTROLS).mount();
        final AtomicInteger calls = new AtomicInteger();
        transientFixture.connect(TransientSignalPaths.SURFACE_INTENT, signal ->
                SignalDecision.replace(signal.replace(retargetedPayload())));
        SignalGroups.nativeGroup().connect(ContextSignals.action(SurfaceIds.COMBAT_CONTROLS, "custom"),
                signal -> { calls.incrementAndGet(); return SignalDecision.continueSignal(); });

        artframework.api.ArtFramework.dispatchSurfaceIntent("custom", SurfaceIds.COMBAT_HAND);

        assertEquals(0, calls.get());
        assertFalse(PresentSurfaces.world().has(currentHand(), SurfaceResultComponent.class));
    }

    @Test public void rejectedDispatchRecordsResultAndDoesNotInvokeNativeOperation() {
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        final AtomicInteger calls = new AtomicInteger();
        transientFixture.connect(TransientSignalPaths.SURFACE_INTENT,
                signal -> SignalDecision.stopRejected("blocked:policy"));
        SignalGroups.nativeGroup().connect(ContextSignals.action(SurfaceIds.COMBAT_HAND, "custom"),
                signal -> { calls.incrementAndGet(); return SignalDecision.continueSignal(); });

        ArtFramework.dispatchSurfaceIntent("custom", SurfaceIds.COMBAT_HAND);

        assertEquals(0, calls.get());
        assertFalse(PresentSurfaces.world().has(currentHand(), SurfaceResultComponent.class));
    }

    @Test public void replacementCannotRetargetTheProducerSurface() {
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        ArtFramework.component(SurfaceIds.MAP).mount();
        final AtomicInteger calls = new AtomicInteger();
        transientFixture.connect(TransientSignalPaths.SURFACE_INTENT,
                signal -> SignalDecision.replace(signal.replace(
                        payload("custom", SurfaceIds.MAP,
                                SurfaceIntentExecutionSystem.normalizeArgs()))));
        SignalGroups.nativeGroup().connect(ContextSignals.action(SurfaceIds.MAP, "custom"),
                signal -> { calls.incrementAndGet(); return SignalDecision.continueSignal(); });

        ArtFramework.dispatchSurfaceIntent("custom", SurfaceIds.COMBAT_HAND);

        assertEquals(0, calls.get());
    }

    @Test public void nestedDispatchCannotRetargetOuterIntentOrWriteAcrossSurfaces() {
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        ArtFramework.component(SurfaceIds.COMBAT_CONTROLS).mount();
        final AtomicInteger controlsCalls = new AtomicInteger();
        transientFixture.connect(TransientSignalPaths.SURFACE_INTENT, signal -> {
            Map<?, ?> original = (Map<?, ?>) signal.payload;
            if ("nested".equals(original.get("name"))) {
                return SignalDecision.stopRejected("nested blocked");
            }
            ArtFramework.dispatchSurfaceIntent("nested", SurfaceIds.COMBAT_CONTROLS);
            return SignalDecision.replace(signal.replace(
                    payload("custom", SurfaceIds.COMBAT_CONTROLS,
                            SurfaceIntentExecutionSystem.normalizeArgs())));
        });
        SignalGroups.nativeGroup().connect(ContextSignals.action(SurfaceIds.COMBAT_CONTROLS, "custom"),
                signal -> { controlsCalls.incrementAndGet(); return SignalDecision.continueSignal(); });

        ArtFramework.dispatchSurfaceIntent("outer", SurfaceIds.COMBAT_HAND);

        assertEquals(0, controlsCalls.get());
        assertFalse(PresentSurfaces.world().has(currentControls(), SurfaceResultComponent.class));
    }

    @Test public void lateInterceptorStopsBeforeConsumerSideEffects() {
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        final AtomicInteger calls = new AtomicInteger();
        transientFixture.connect(TransientSignalPaths.SURFACE_INTENT,
                signal -> SignalDecision.stopRejected("blocked:late"));
        SignalGroups.nativeGroup().connect(ContextSignals.action(SurfaceIds.COMBAT_HAND, "custom"),
                signal -> { calls.incrementAndGet(); return SignalDecision.continueSignal(); });

        ArtFramework.dispatchSurfaceIntent("custom", SurfaceIds.COMBAT_HAND);

        assertEquals(0, calls.get());
        assertFalse(PresentSurfaces.world().has(currentHand(), SurfaceResultComponent.class));
    }

    @Test public void stoppedIntentCannotBeConfirmedByLaterAuthorityFrame() {
        ArtFramework.publishFrame(ContextFrame.of(1L, "combat", Arrays.asList(
                CardView.builder(new CardRef("card", "Strike")).build())));
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        transientFixture.connect(TransientSignalPaths.SURFACE_INTENT,
                signal -> SignalDecision.stopRejected("blocked:policy"));

        ArtFramework.component(SurfaceIds.COMBAT_HAND).action("begin_drag", "card");
        EntityId entity = currentHand();
        assertEquals(BusinessConfirmationComponent.State.FAILED,
                PresentSurfaces.world().get(entity, BusinessConfirmationComponent.class).state);

        ArtFramework.publishFrame(ContextFrame.of(9L, "combat", null));

        assertEquals(BusinessConfirmationComponent.State.FAILED,
                PresentSurfaces.world().get(entity, BusinessConfirmationComponent.class).state);
    }

    @Test public void missingSurfaceIsSafeAndDoesNotInvokeNativeOperation() {
        final AtomicInteger calls = new AtomicInteger();
        SignalGroups.nativeGroup().connect(ContextSignals.action("missing", "custom"),
                signal -> { calls.incrementAndGet(); return SignalDecision.continueSignal(); });
        ArtFramework.dispatchSurfaceIntent("custom", "missing");
        assertEquals(0, calls.get());
    }

    @Test public void resetRecreatesTheSingleScheduleOwnedConsumer() {
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        ArtFramework.resetForTests();
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        ArtFramework.dispatchSurfaceIntent("custom", SurfaceIds.COMBAT_HAND);
        assertNotNull(PresentSurfaces.world().get(currentHand(), SurfaceResultComponent.class));
    }

    private static EntityId currentHand() {
        return PresentSurfaces.world().query(SurfaceIdentityComponent.class).get(0);
    }

    private static EntityId currentControls() {
        for (EntityId entity : PresentSurfaces.world().query(SurfaceIdentityComponent.class)) {
            if (SurfaceIds.COMBAT_CONTROLS.equals(
                    PresentSurfaces.world().get(entity, SurfaceIdentityComponent.class).id)) return entity;
        }
        throw new AssertionError("controls surface not mounted");
    }

    private static Map<String, Object> payload(String name, String surfaceId, List<Object> args) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("surfaceId", surfaceId);
        payload.put("name", name);
        payload.put("args", args);
        return payload;
    }

    private static Map<String, Object> retargetedPayload() {
        Map<String, Object> payload = payload("custom", SurfaceIds.COMBAT_CONTROLS,
                SurfaceIntentExecutionSystem.normalizeArgs());
        payload.put("__producerSurfaceId", SurfaceIds.COMBAT_HAND);
        return payload;
    }
}
