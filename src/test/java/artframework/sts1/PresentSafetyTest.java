package artframework.sts1;

import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.render.MapDrawPath;
import artframework.sts1.render.Sts1RenderPipeline;
import artframework.render.RenderHost;
import artframework.render.RenderHosts;
import artframework.render.RenderStateEcs;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PresentSafetyTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void panicClearsLevelsAndSuppress() {
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        FullPresentMode.setMapLevel(PresentLevel.FULL);
        FullPresentMode.setEventLevel(PresentLevel.FULL);
        FullPresentMode.setSelectLevel(PresentLevel.FULL);
        CombatInputRouter.setSuppressNativeInput(true);
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        ArtFramework.component(SurfaceIds.MAP).mount();
        ArtFramework.component(SurfaceIds.EVENT).action("mount_event");
        ArtFramework.component(SurfaceIds.SELECT_GRID).action("mount_select");

        PresentSafety.panic("test");
        assertTrue(PresentSafety.isPanic());
        assertEquals("test", PresentSafety.panicReason());
        assertEquals(PresentLevel.OFF, FullPresentMode.combatHandLevel());
        assertEquals(PresentLevel.OFF, FullPresentMode.mapLevel());
        assertEquals(PresentLevel.OFF, FullPresentMode.eventLevel());
        assertEquals(PresentLevel.OFF, FullPresentMode.selectLevel());
        assertFalse(FullPresentMode.maySuppressNative(SurfaceIds.COMBAT_HAND));
        assertFalse(FullPresentMode.mayOwnInput(SurfaceIds.COMBAT_HAND));
        assertFalse(ArtFramework.component(SurfaceIds.COMBAT_HAND).isMounted());
        assertFalse(ArtFramework.component(SurfaceIds.EVENT).isMounted());
        assertFalse(ArtFramework.component(SurfaceIds.SELECT_GRID).isMounted());
        assertFalse(CombatInputRouter.shouldSuppressNativeInput());
    }

    @Test
    public void clearPanicAllowsLevelsAgain() {
        PresentSafety.panic("x");
        PresentSafety.clearPanic();
        assertFalse(PresentSafety.isPanic());
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        assertFalse(FullPresentMode.mayOwnInput(SurfaceIds.COMBAT_HAND));
    }

    @Test
    public void hostRecreateIncrements() {
        int before = PresentSafety.recreationCount();
        Sts1RenderPipeline.setOverlayObserve(true);
        MapDrawPath.panZoom().setPan(5f, 5f);
        PresentSafety.onHostRecreated();
        assertEquals(before + 1, PresentSafety.recreationCount());
        assertFalse(Sts1RenderPipeline.isOverlayObserve());
        assertEquals(0f, MapDrawPath.panZoom().panX(), 0.01f);
        assertTrue(String.valueOf(PresentSafety.probeSlice().get("c1HostRecreation")).length() > 0);
    }

    @Test
    public void legacyHostRecreatedRetainsVoidJvmDescriptor() throws Exception {
        Method method = PresentSafety.class.getMethod("onHostRecreated");
        assertEquals(Void.TYPE, method.getReturnType());
        assertTrue(Modifier.isStatic(method.getModifiers()));
        final Runnable[] deferred = new Runnable[1];
        PresentSafety.setHostRecreationDispatcherForTests(new PresentSafety.HostRecreationDispatcher() {
            public PresentSafety.HostRecreationAdmission dispatch(Runnable work) {
                deferred[0] = work;
                return PresentSafety.HostRecreationAdmission.SCHEDULED;
            }
        });

        PresentSafety.onHostRecreated();

        assertTrue(deferred[0] != null);
        assertLockstepRecreation("scheduled");
    }

    @Test
    public void hostRecreateRetainsEcsAuthorityAndReprojectsDisposableHostState() {
        RenderStateEcs.surface("sts1.present-safety-recreate", 4f, 5f, 40f, 50f, true);
        RenderStateEcs.fullFrame(800f, 600f, true,
                java.util.Collections.<artframework.presentation.EffectAttachment>emptyList());
        RenderHosts.get().recreateFromEcs();
        assertTrue(RenderHosts.get().getTarget(
                RenderHost.c2SurfaceTargetId("sts1.present-safety-recreate")) != null);

        PresentSafety.onHostRecreated();
        PresentSafety.onHostRecreated();

        assertEquals(2, RenderStateEcs.context().entities().size());
        assertTrue(RenderHosts.get().getTarget(
                RenderHost.c2SurfaceTargetId("sts1.present-safety-recreate")) != null);
        assertTrue(RenderHosts.get().fullFrameTarget() != null);
        assertEquals(2, PresentSafety.recreationCount());
    }

    @Test
    public void hostRecreateContinuesMaterializerAndProjectionAfterHostFailure() {
        RenderStateEcs.surface("sts1.present-safety-failure", 4f, 5f, 40f, 50f, true);
        final java.util.List<String> steps = new java.util.ArrayList<String>();
        PresentSafety.setHostRecreationStepRunnerForTests(new PresentSafety.HostRecreationStepRunner() {
            public void run(String name, Runnable step) {
                steps.add(name);
                if ("renderHost".equals(name)) throw new IllegalStateException("host");
                step.run();
            }
        });

        PresentSafety.onHostRecreated();

        assertEquals(java.util.Arrays.asList("presentationState", "c1Host", "renderHost", "materializer", "projection"), steps);
        assertEquals(1, RenderStateEcs.context().entities().size());
        assertTrue(RenderHosts.get().getTarget(
                RenderHost.c2SurfaceTargetId("sts1.present-safety-failure")) != null);
    }

    @Test
    public void hostRecreateContinuesRenderHostMaterializerAndProjectionAfterC1Failure() {
        RenderStateEcs.surface("sts1.present-safety-c1-failure", 4f, 5f, 40f, 50f, true);
        final java.util.List<String> steps = new java.util.ArrayList<String>();
        PresentSafety.setHostRecreationStepRunnerForTests(new PresentSafety.HostRecreationStepRunner() {
            public void run(String name, Runnable step) {
                steps.add(name);
                if ("c1Host".equals(name)) throw new IllegalStateException("c1");
                step.run();
            }
        });

        PresentSafety.onHostRecreated();

        assertEquals(
                java.util.Arrays.asList(
                        "presentationState", "c1Host", "renderHost", "materializer", "projection"),
                steps);
        assertTrue(RenderHosts.get().getTarget(
                RenderHost.c2SurfaceTargetId("sts1.present-safety-c1-failure")) != null);
    }

    @Test
    public void hostRecreateContinuesMaterializerAndProjectionAfterPresentationStateFailure() {
        RenderStateEcs.surface("sts1.present-safety-preamble-failure", 4f, 5f, 40f, 50f, true);
        final java.util.List<String> steps = new java.util.ArrayList<String>();
        PresentSafety.setHostRecreationStepRunnerForTests(new PresentSafety.HostRecreationStepRunner() {
            public void run(String name, Runnable step) {
                steps.add(name);
                if ("presentationState".equals(name)) throw new IllegalStateException("state");
                step.run();
            }
        });

        PresentSafety.onHostRecreated();

        assertEquals(java.util.Arrays.asList("presentationState", "c1Host", "renderHost", "materializer", "projection"), steps);
        assertEquals(1, RenderStateEcs.context().entities().size());
        assertTrue(RenderHosts.get().getTarget(
                RenderHost.c2SurfaceTargetId("sts1.present-safety-preamble-failure")) != null);
    }

    @Test
    public void hostRecreateContinuesProjectionAfterMaterializerFailure() {
        RenderStateEcs.surface("sts1.present-safety-materializer-failure", 4f, 5f, 40f, 50f, true);
        final java.util.List<String> steps = new java.util.ArrayList<String>();
        PresentSafety.setHostRecreationStepRunnerForTests(new PresentSafety.HostRecreationStepRunner() {
            public void run(String name, Runnable step) {
                steps.add(name);
                if ("materializer".equals(name)) {
                    throw new DistinctMaterializerRecreationException("materializer");
                }
                step.run();
            }
        });

        PresentSafety.onHostRecreated();

        assertEquals(
                java.util.Arrays.asList(
                        "presentationState", "c1Host", "renderHost", "materializer", "projection"),
                steps);
        assertEquals(1, RenderStateEcs.context().entities().size());
        assertTrue(RenderHosts.get().getTarget(
                RenderHost.c2SurfaceTargetId("sts1.present-safety-materializer-failure")) != null);
        Map<String, Object> probe = PresentSafety.probeSlice();
        assertTrue(probe.containsKey("c1HostRecreation"));
        assertTrue(probe.containsKey("materializerRecreation"));
        String materializerStatus = String.valueOf(probe.get("materializerRecreation"));
        assertEquals("failed: DistinctMaterializerRecreationException", materializerStatus);
        assertFalse("rebuilt".equals(String.valueOf(probe.get("materializerRecreation"))));
        assertFalse(
                "later projection must not invent rebuilt success after materializer failure",
                "rebuilt".equals(String.valueOf(probe.get("c1HostRecreation"))));
        assertFalse("rebuilt".equals(materializerStatus));
    }

    @Test
    public void hostRecreateDispatchesLifecycleWorkBeforeReportingC1Result() {
        final Runnable[] deferred = new Runnable[1];
        final java.util.List<String> steps = new java.util.ArrayList<String>();
        PresentSafety.setHostRecreationDispatcherForTests(
                new PresentSafety.HostRecreationDispatcher() {
                    public PresentSafety.HostRecreationAdmission dispatch(Runnable work) {
                        deferred[0] = work;
                        return PresentSafety.HostRecreationAdmission.SCHEDULED;
                    }
                });
        PresentSafety.setHostRecreationStepRunnerForTests(
                new PresentSafety.HostRecreationStepRunner() {
                    public void run(String name, Runnable step) {
                        steps.add(name);
                        step.run();
                    }
                });

        assertEquals(PresentSafety.HostRecreationAdmission.SCHEDULED,
                PresentSafety.requestHostRecreation());

        assertLockstepRecreation("scheduled");
        assertTrue(steps.isEmpty());
        assertTrue(deferred[0] != null);

        deferred[0].run();

        assertEquals(
                java.util.Arrays.asList(
                        "presentationState", "c1Host", "renderHost", "materializer", "projection"),
                steps);
        assertFalse("scheduled".equals(
                PresentSafety.probeSlice().get("c1HostRecreation")));
        assertTrue(PresentSafety.probeSlice().containsKey("c1HostRecreation"));
        assertTrue(PresentSafety.probeSlice().containsKey("materializerRecreation"));
    }

    @Test
    public void hostRecreateReportsDispatcherRejectionWithoutRunningLifecycle() {
        final java.util.List<String> steps = new java.util.ArrayList<String>();
        PresentSafety.setHostRecreationDispatcherForTests(new PresentSafety.HostRecreationDispatcher() {
            public PresentSafety.HostRecreationAdmission dispatch(Runnable work) {
                return PresentSafety.HostRecreationAdmission.DISPATCHER_FAILURE;
            }
        });
        PresentSafety.setHostRecreationStepRunnerForTests(new PresentSafety.HostRecreationStepRunner() {
            public void run(String name, Runnable step) { steps.add(name); step.run(); }
        });

        assertEquals(PresentSafety.HostRecreationAdmission.DISPATCHER_FAILURE,
                PresentSafety.requestHostRecreation());
        assertTrue(steps.isEmpty());
        assertLockstepRecreation("failed: dispatcher rejected");
    }

    @Test
    public void hostRecreateReportsSynchronousFallbackAdmission() {
        assertEquals(PresentSafety.HostRecreationAdmission.SYNCHRONOUS,
                PresentSafety.requestHostRecreation());
        assertFalse("scheduled".equals(PresentSafety.probeSlice().get("c1HostRecreation")));
    }

    @Test
    public void hostRecreateCommandReturnsErrorForDispatcherFailure() throws Exception {
        final java.util.List<String> steps = new java.util.ArrayList<String>();
        PresentSafety.setHostRecreationDispatcherForTests(new PresentSafety.HostRecreationDispatcher() {
            public PresentSafety.HostRecreationAdmission dispatch(Runnable work) {
                throw new IllegalStateException("closed");
            }
        });
        PresentSafety.setHostRecreationStepRunnerForTests(new PresentSafety.HostRecreationStepRunner() {
            public void run(String name, Runnable step) { steps.add(name); step.run(); }
        });

        artframework.api.UiOpResult result = invokeHostRecreateResult();
        assertFalse(result.isOk());
        assertEquals("host cache recreation dispatcher rejected", result.message);
        assertTrue(steps.isEmpty());
        assertLockstepRecreation("failed: dispatcher IllegalStateException");
    }

    @Test
    public void hostRecreateCommandReturnsErrorForExplicitDispatcherRejection() {
        final java.util.List<String> steps = new java.util.ArrayList<String>();
        PresentSafety.setHostRecreationDispatcherForTests(new PresentSafety.HostRecreationDispatcher() {
            public PresentSafety.HostRecreationAdmission dispatch(Runnable work) {
                return PresentSafety.HostRecreationAdmission.DISPATCHER_FAILURE;
            }
        });
        PresentSafety.setHostRecreationStepRunnerForTests(new PresentSafety.HostRecreationStepRunner() {
            public void run(String name, Runnable step) { steps.add(name); step.run(); }
        });

        artframework.api.UiOpResult result = invokeHostRecreateResult();
        assertFalse(result.isOk());
        assertEquals("host cache recreation dispatcher rejected", result.message);
        assertTrue(steps.isEmpty());
        assertLockstepRecreation("failed: dispatcher rejected");
    }

    @Test
    public void hostRecreateCommandReportsRequestedBeforeScheduledWorkRuns() throws Exception {
        final Runnable[] deferred = new Runnable[1];
        final java.util.List<String> steps = new java.util.ArrayList<String>();
        PresentSafety.setHostRecreationDispatcherForTests(new PresentSafety.HostRecreationDispatcher() {
            public PresentSafety.HostRecreationAdmission dispatch(Runnable work) {
                deferred[0] = work;
                return PresentSafety.HostRecreationAdmission.SCHEDULED;
            }
        });
        PresentSafety.setHostRecreationStepRunnerForTests(new PresentSafety.HostRecreationStepRunner() {
            public void run(String name, Runnable step) { steps.add(name); step.run(); }
        });

        artframework.api.UiOpResult result = invokeHostRecreateResult();
        assertTrue(result.isOk());
        assertEquals("host cache recreation requested", result.message);
        assertTrue(steps.isEmpty());
        assertLockstepRecreation("scheduled");
        deferred[0].run();
        assertEquals(5, steps.size());
    }

    @Test
    public void hostRecreateCommandReportsRequestedForSynchronousFallback() {
        final java.util.List<String> steps = new java.util.ArrayList<String>();
        PresentSafety.setHostRecreationStepRunnerForTests(new PresentSafety.HostRecreationStepRunner() {
            public void run(String name, Runnable step) { steps.add(name); step.run(); }
        });

        artframework.api.UiOpResult result = invokeHostRecreateResult();

        assertTrue(result.isOk());
        assertEquals("host cache recreation requested", result.message);
        assertEquals(
                java.util.Arrays.asList(
                        "presentationState", "c1Host", "renderHost", "materializer", "projection"),
                steps);
    }

    private static artframework.api.UiOpResult invokeHostRecreateResult() {
        try {
            Method method = artframework.console.ArtCommand.class.getDeclaredMethod("hostRecreateResult");
            method.setAccessible(true);
            return (artframework.api.UiOpResult) method.invoke(null);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static void assertLockstepRecreation(String expectedStatus) {
        Map<String, Object> probe = PresentSafety.probeSlice();
        assertTrue(probe.containsKey("c1HostRecreation"));
        assertTrue(probe.containsKey("materializerRecreation"));
        assertEquals(expectedStatus, probe.get("c1HostRecreation"));
        assertEquals(expectedStatus, probe.get("materializerRecreation"));
    }

    private static final class DistinctMaterializerRecreationException extends RuntimeException {
        DistinctMaterializerRecreationException(String message) {
            super(message);
        }
    }

    @Test
    public void lifecycleMatrixPanicClearAndCapabilityFallback() {
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new artframework.sts1.input.RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        // Without combat scene, FULL stays fallback-native (19.1/19.4).
        assertFalse(FullPresentMode.mayOwnInput(SurfaceIds.COMBAT_HAND));
        PresentSafety.panic("lifecycle");
        assertTrue(PresentSafety.isPanic());
        assertEquals(PresentLevel.OFF, FullPresentMode.combatHandLevel());
        assertFalse(ArtFramework.component(SurfaceIds.COMBAT_HAND).isMounted());
        PresentSafety.clearPanic();
        assertFalse(PresentSafety.isPanic());
        // clear-panic alone does not re-arm FULL.
        assertEquals(PresentLevel.OFF, FullPresentMode.combatHandLevel());
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        assertFalse(FullPresentMode.maySuppressNative(SurfaceIds.COMBAT_HAND));
    }

    @Test
    public void probeSlice() {
        PresentSafety.panic("p");
        Map<String, Object> m = PresentSafety.probeSlice();
        assertEquals(Boolean.TRUE, m.get("panic"));
        assertEquals("p", m.get("panicReason"));
    }
}
