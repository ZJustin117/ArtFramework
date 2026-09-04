package artframework.api;

import artframework.context.CardView;
import artframework.context.ContextFrame;
import artframework.context.PresentSurfaces;
import artframework.context.SurfaceIds;
import artframework.context.SurfaceIdentityComponent;
import artframework.context.SurfaceLifecycleComponent;
import artframework.context.SurfaceResultComponent;
import artframework.context.NativeIntentLifecycleComponent;
import artframework.sts1.input.NativeInputRecords;
import artframework.ecs.EntityId;
import artframework.core.HostBackend;
import artframework.core.HostCapabilities;
import artframework.presentation.PresentationRegistry;
import artframework.presentation.PresentationMount;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PresentationScheduleTest {
    private FrameworkTransientFixture transientFixture;

    @Before
    public void setUp() {
        ArtFramework.resetForTests();
        transientFixture = new FrameworkTransientFixture();
    }

    @After public void tearDown() {
        transientFixture.close();
        ArtFramework.resetForTests();
    }

    @Test public void productionPhasesHaveOneDeclaredOrder() {
        assertEquals(Arrays.asList(
                PresentationSchedule.Phase.SURFACE_INTENT_EXECUTION,
                PresentationSchedule.Phase.AUTHORITY_PROJECTION_AND_CONFIRMATION,
                PresentationSchedule.Phase.WORLD_NORMALIZATION,
                PresentationSchedule.Phase.ANIMATION,
                PresentationSchedule.Phase.EFFECTS,
                PresentationSchedule.Phase.HOST_PRESENTATION,
                PresentationSchedule.Phase.RENDER_PROJECTION,
                PresentationSchedule.Phase.RENDER_CLOCK,
                PresentationSchedule.Phase.HOST_BACKEND),
                new PresentationSchedule().phases());
    }

    @Test public void schedulesSerializeSharedAuthorityExecution() throws Exception {
        final PresentationSchedule first = new PresentationSchedule();
        final PresentationSchedule second = new PresentationSchedule();
        final CountDownLatch firstEntered = new CountDownLatch(1);
        final CountDownLatch releaseFirst = new CountDownLatch(1);
        final CountDownLatch secondEntered = new CountDownLatch(1);
        final List<String> order = new ArrayList<String>();
        first.transientSignalGroupForTests().connect(
                artframework.core.TransientSignalPaths.AUTHORITY_FRAME, signal -> {
                    order.add("first-entered");
                    firstEntered.countDown();
                    await(releaseFirst);
                    order.add("first-exited");
                    return artframework.core.SignalDecision.continueSignal();
                });
        second.transientSignalGroupForTests().connect(
                artframework.core.TransientSignalPaths.AUTHORITY_FRAME, signal -> {
                    secondEntered.countDown();
                    order.add("second-entered");
                    return artframework.core.SignalDecision.continueSignal();
                });

        Thread firstThread = new Thread(() -> first.publishFrame(
                ContextFrame.of(21L, "combat", null)));
        Thread secondThread = new Thread(() -> second.publishFrame(
                ContextFrame.of(22L, "map", null)));
        firstThread.start();
        assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
        secondThread.start();
        assertTrue(!secondEntered.await(200, TimeUnit.MILLISECONDS));
        releaseFirst.countDown();
        firstThread.join(2000L);
        secondThread.join(2000L);
        assertTrue(!firstThread.isAlive());
        assertTrue(!secondThread.isAlive());
        assertEquals(Arrays.asList("first-entered", "first-exited", "second-entered"), order);
    }

    @Test public void scheduleDispatchesSurfaceLifecycleSynchronously() {
        PresentationSchedule schedule = new PresentationSchedule();
        artframework.core.UiComponent hand = PresentSurfaces.get(SurfaceIds.COMBAT_HAND);
        hand.mount();
        EntityId entity = PresentSurfaces.world().query(SurfaceIdentityComponent.class).get(0);
        schedule.dispatchSurfaceLifecycle(SurfaceIds.COMBAT_HAND, false);
        assertEquals(Boolean.FALSE, Boolean.valueOf(PresentSurfaces.world()
                .get(entity, SurfaceLifecycleComponent.class).mounted));
    }

    @Test public void closingScheduleDisposesOnlyItsLocalTransientGroup() {
        PresentationSchedule schedule = new PresentationSchedule();
        artframework.core.SignalGroup local = schedule.transientSignalGroupForTests();
         artframework.core.SignalGroup global = artframework.core.SignalGroups.transientRuntimeRawGroup();

        assertTrue(isRegistered(local));
        assertTrue(isRegistered(global));
        schedule.close();
        assertTrue(!isRegistered(local));
        assertTrue(isRegistered(global));
        global.dispatch(new artframework.core.UiSignal(global.id(), null,
                "schedule-close-probe", "test", null, null));
        assertTrue(isRegistered(global));
    }

    @Test public void closeSerializesUntilAnAdmittedScheduleDispatchFinishes()
            throws Exception {
        final PresentationSchedule schedule = new PresentationSchedule();
        final artframework.core.SignalGroup local = schedule.transientSignalGroupForTests();
        final CountDownLatch admitted = new CountDownLatch(1);
        final CountDownLatch releaseDispatch = new CountDownLatch(1);
        final CountDownLatch closeStarted = new CountDownLatch(1);
        final CountDownLatch closeCompleted = new CountDownLatch(1);
        final boolean[] directAdmission = {false};
        local.connect(artframework.core.TransientSignalPaths.AUTHORITY_FRAME, signal -> {
            if (!"presentation-schedule".equals(signal.source)) {
                return artframework.core.SignalDecision.continueSignal();
            }
            directAdmission[0] = true;
            try {
                schedule.transientSignalRuntimeForTests().dispatch(
                        artframework.core.TransientSignalPaths.AUTHORITY_FRAME,
                        "test-direct", ContextFrame.of(8L, "map", null));
            } finally {
                directAdmission[0] = false;
            }
            return artframework.core.SignalDecision.continueSignal();
        });
        schedule.setTransientDispatchAdmissionHookForTests(() -> {
            if (directAdmission[0]) {
                admitted.countDown();
                await(releaseDispatch);
            }
        });

        Thread dispatch = new Thread(() -> schedule.publishFrame(
                ContextFrame.of(7L, "combat", null)));
        dispatch.start();
        assertTrue(admitted.await(2, TimeUnit.SECONDS));
        Thread close = new Thread(() -> {
            closeStarted.countDown();
            schedule.close();
            closeCompleted.countDown();
        });
        close.start();
        assertTrue(closeStarted.await(2, TimeUnit.SECONDS));
        assertTrue(!closeCompleted.await(200, TimeUnit.MILLISECONDS));
        assertTrue(isRegistered(local));
        releaseDispatch.countDown();
        join(dispatch);
        join(close);

        assertTrue(closeCompleted.await(2, TimeUnit.SECONDS));
        assertTrue(!isRegistered(local));
        assertClosed(() -> schedule.publishFrame(ContextFrame.of(9L, "event", null)));
    }

    @Test public void resetSerializesUntilAnAdmittedScheduleDispatchFinishes()
            throws Exception {
        final PresentationSchedule schedule = new PresentationSchedule();
        final artframework.core.SignalGroup oldGroup = schedule.transientSignalGroupForTests();
        final CountDownLatch admitted = new CountDownLatch(1);
        final CountDownLatch releaseDispatch = new CountDownLatch(1);
        final CountDownLatch resetStarted = new CountDownLatch(1);
        final CountDownLatch resetCompleted = new CountDownLatch(1);
        final boolean[] directAdmission = {false};
        oldGroup.connect(artframework.core.TransientSignalPaths.AUTHORITY_FRAME, signal -> {
            if (!"presentation-schedule".equals(signal.source)) {
                return artframework.core.SignalDecision.continueSignal();
            }
            directAdmission[0] = true;
            try {
                schedule.transientSignalRuntimeForTests().dispatch(
                        artframework.core.TransientSignalPaths.AUTHORITY_FRAME,
                        "test-direct", ContextFrame.of(8L, "map", null));
            } finally {
                directAdmission[0] = false;
            }
            return artframework.core.SignalDecision.continueSignal();
        });
        schedule.setTransientDispatchAdmissionHookForTests(() -> {
            if (directAdmission[0]) {
                admitted.countDown();
                await(releaseDispatch);
            }
        });

        Thread dispatch = new Thread(() -> schedule.publishFrame(
                ContextFrame.of(7L, "combat", null)));
        dispatch.start();
        assertTrue(admitted.await(2, TimeUnit.SECONDS));
        Thread reset = new Thread(() -> {
            resetStarted.countDown();
            schedule.resetForTests();
            resetCompleted.countDown();
        });
        reset.start();
        assertTrue(resetStarted.await(2, TimeUnit.SECONDS));
        assertTrue(!resetCompleted.await(200, TimeUnit.MILLISECONDS));
        assertTrue(isRegistered(oldGroup));
        releaseDispatch.countDown();
        join(dispatch);
        join(reset);

        artframework.core.SignalGroup newGroup = schedule.transientSignalGroupForTests();
        assertTrue(resetCompleted.await(2, TimeUnit.SECONDS));
        assertTrue(oldGroup != newGroup);
        assertTrue(!isRegistered(oldGroup));
        assertTrue(isRegistered(newGroup));
        schedule.close();
    }

    @Test public void adHocSchedulesDoNotAddConsumersToFrameworkTransientRuntime() {
        PresentationSchedule first = new PresentationSchedule();
        PresentationSchedule second = new PresentationSchedule();
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        final int[] authoritySignals = {0};
        final int[] nativeLifecycleSignals = {0};
        final int[] nativeIntentExecutions = {0};
        transientFixture.connect(
                artframework.core.TransientSignalPaths.AUTHORITY_FRAME, signal -> {
                    authoritySignals[0]++;
                    return artframework.core.SignalDecision.continueSignal();
                });
        transientFixture.connect(
                artframework.core.TransientSignalPaths.NATIVE_INTENT_LIFECYCLE, signal -> {
                    nativeLifecycleSignals[0]++;
                    return artframework.core.SignalDecision.continueSignal();
                });
        artframework.core.SignalGroups.nativeGroup().connect(
                artframework.context.ContextSignals.action(SurfaceIds.COMBAT_HAND, "custom"),
                signal -> {
                    nativeIntentExecutions[0]++;
                    return artframework.core.SignalDecision.continueSignal();
                });

        first.publishFrame(ContextFrame.of(17L, "combat", null));
        second.publishFrame(ContextFrame.of(18L, "map", null));
        NativeInputRecords.intent(SurfaceIds.COMBAT_HAND, "play_card",
                NativeIntentLifecycleComponent.State.EXECUTED, "accepted");
        first.dispatchSurfaceIntent("custom", SurfaceIds.COMBAT_HAND);

        // Both ad-hoc schedules bypass the process-global runtime. Their construction does not
        // add a hidden global consumer, and one facade publish reaches the global interceptor once.
        assertEquals(0, authoritySignals[0]);
        assertEquals(18L, ArtFramework.projection().lastFrameId());
        assertEquals(1, nativeLifecycleSignals[0]);
        assertEquals(1, nativeIntentExecutions[0]);
        ArtFramework.publishFrame(ContextFrame.of(19L, "event", null));
        assertEquals(1, authoritySignals[0]);
        assertEquals(19L, ArtFramework.projection().lastFrameId());
        first.resetForTests();
        second.resetForTests();
    }

    @Test public void lateAuthorityStopSkipsProjectionAndConfirmation() {
        transientFixture.connect(
                artframework.core.TransientSignalPaths.AUTHORITY_FRAME,
                signal -> artframework.core.SignalDecision.stopRejected("blocked:authority"));

        ArtFramework.publishFrame(ContextFrame.of(19L, "map", null));

        assertEquals(-1L, ArtFramework.projection().lastFrameId());
    }

    @Test public void scheduleDispatchesSurfaceIntentSynchronously() {
        PresentationSchedule schedule = new PresentationSchedule();
        artframework.core.UiComponent hand = PresentSurfaces.get(SurfaceIds.COMBAT_HAND);
        hand.mount();
        EntityId entity = PresentSurfaces.world().query(SurfaceIdentityComponent.class).get(0);

        schedule.dispatchSurfaceIntent("custom", SurfaceIds.COMBAT_HAND, "argument");

        assertTrue(PresentSurfaces.world().get(entity, SurfaceResultComponent.class) != null);
    }

    @Test public void backendObservesProjectedAuthorityAndAdvancedRenderClock() {
        final boolean[] observed = new boolean[1];
        final float before = ArtFramework.render().timeSeconds();
        ArtFramework.setHostBackend(new HostBackend() {
            @Override public boolean isReady() { return true; }
            @Override public void attach(PresentationMount mount) {}
            @Override public void detach(PresentationMount mount) {}
            @Override public void applyLayout(PresentationMount mount) {}
            @Override public HostCapabilities capabilities() { return HostCapabilities.none(); }

            @Override public void tick(float deltaSeconds) {
                observed[0] = ArtFramework.projection().lastFrameId() == 9L
                        && ArtFramework.render().timeSeconds() > before;
            }
        });

        ArtFramework.advanceFrame(0.25f,
                ContextFrame.of(9L, 2L, "combat", null, null, null, null));

        assertTrue(observed[0]);
        assertEquals(before + 0.25f, ArtFramework.render().timeSeconds(), 0.0001f);
    }

    @Test public void authorityProjectionPrecedesNativeConfirmationInPublishFrame() {
        NativeInputRecords.intent(artframework.context.SurfaceIds.COMBAT_HAND, "play_card",
                NativeIntentLifecycleComponent.State.EXECUTED, "accepted");
        NativeInputRecords.observe(artframework.context.SurfaceIds.COMBAT_HAND, 9L, 2L, "combat");

        ArtFramework.publishFrame(ContextFrame.of(9L, 2L, "combat", null, null, null, null));

        EntityId input = artframework.presentation.PresentationRegistry.context("sts1-input").entity(
                new artframework.presentation.PresentationKey("sts1.input",
                        artframework.context.SurfaceIds.COMBAT_HAND));
        assertEquals(NativeIntentLifecycleComponent.State.CONFIRMED,
                artframework.presentation.PresentationRegistry.world().get(input,
                        NativeIntentLifecycleComponent.class).state);
        assertEquals(9L, ArtFramework.projection().lastFrameId());
    }

    @Test public void hostPresentationRunsBeforeRenderClockAndBackend() {
        final List<String> order = new ArrayList<String>();
        final float before = ArtFramework.render().timeSeconds();
        ArtFramework.setHostPresentationSystem(new HostPresentationSystem() {
            @Override public void tick(float deltaSeconds) {
                assertEquals(before, ArtFramework.render().timeSeconds(), 0.0001f);
                order.add("host-presentation");
            }
        });
        ArtFramework.setHostBackend(new HostBackend() {
            @Override public boolean isReady() { return true; }
            @Override public void attach(PresentationMount mount) {}
            @Override public void detach(PresentationMount mount) {}
            @Override public void applyLayout(PresentationMount mount) {}
            @Override public void tick(float deltaSeconds) {
                assertTrue(ArtFramework.render().timeSeconds() > before);
                order.add("host-backend");
            }
        });

        ArtFramework.tick(0.1f);

        assertEquals(Arrays.asList("host-presentation", "host-backend"), order);
    }

    @Test public void failedAuthorityProjectionDoesNotRetainAnEcsWrapper() {
        try {
            ArtFramework.publishFrame(ContextFrame.of(1L, "combat",
                    Arrays.<CardView>asList((CardView) null)));
            fail("expected malformed card view to fail projection");
        } catch (NullPointerException expected) {
            // Synchronous delivery retains no queued ECS command.
        }

        ArtFramework.publishFrame(ContextFrame.of(2L, "map", null));
        assertEquals(2L, ArtFramework.projection().lastFrameId());

        ArtFramework.tick(0f);
        assertEquals(2L, ArtFramework.projection().lastFrameId());
    }

    @Test public void failedAdvanceFrameDoesNotRetainItsPendingAuthorityFrame() {
        try {
            ArtFramework.advanceFrame(0f, ContextFrame.of(1L, "combat",
                    Arrays.<CardView>asList((CardView) null)));
            fail("expected malformed card view to fail projection");
        } catch (NullPointerException expected) {
            // Synchronous delivery retains no queued ECS command.
        }

        ArtFramework.tick(0f);
        assertEquals(-1L, ArtFramework.projection().lastFrameId());
    }

    @Test public void projectionFailureReleasesSharedScheduleGate() {
        PresentationSchedule failed = new PresentationSchedule();
        try {
            failed.publishFrame(ContextFrame.of(41L, "broken",
                    Arrays.<CardView>asList((CardView) null)));
            fail("expected malformed card view to fail projection");
        } catch (NullPointerException expected) {
            // The gate must be released even when synchronous projection fails.
        }

        PresentationSchedule later = new PresentationSchedule();
        later.publishFrame(ContextFrame.of(42L, "later", null));
        assertEquals(42L, ArtFramework.projection().lastFrameId());
    }

    @Test public void facadePublishFrameUsesTheScheduleProjectionBridge() {
        ContextFrame frame = ContextFrame.of(9L, 2L, "combat", null, null, null, null);

        ArtFramework.publishFrame(frame);

        assertEquals(9L, ArtFramework.projection().lastFrameId());
        assertEquals("combat", ArtFramework.projection().scene());
    }

    @Test public void pulseCompatibilityPathUsesScheduleOwnedEffectSystemOnly() {
        final float before = ArtFramework.render().timeSeconds();
        ArtFramework.setHostBackend(new HostBackend() {
            @Override public boolean isReady() { return true; }
            @Override public void attach(PresentationMount mount) {}
            @Override public void detach(PresentationMount mount) {}
            @Override public void applyLayout(PresentationMount mount) {}
            @Override public HostCapabilities capabilities() { return HostCapabilities.none(); }
            @Override public void tick(float deltaSeconds) {
                fail("pulse-only compatibility path must not run the host backend");
            }
        });

        artframework.core.EffectPulse.pulse("missing-window", "panel",
                artframework.render.LightwaveEffect.ID, 1f);
        artframework.render.LightwaveControls.tickPulses(0.25f);

        assertTrue(artframework.core.EffectPulse.isActive("missing-window", "panel"));
        assertEquals(before, ArtFramework.render().timeSeconds(), 0.0001f);
    }

    /** Package-visible registry check does not create a missing local group. */
    private static boolean isRegistered(artframework.core.SignalGroup group) {
        return artframework.core.SignalGroups.isRegistered(group);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(2, TimeUnit.SECONDS)) throw new AssertionError("timed out");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private static void join(Thread thread) {
        try {
            thread.join(2000L);
            if (thread.isAlive()) throw new AssertionError("timed out");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private static void assertClosed(Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException expected) {
            return;
        }
        throw new AssertionError("closed schedule accepted a later dispatch");
    }
}
