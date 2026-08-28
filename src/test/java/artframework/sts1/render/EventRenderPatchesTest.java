package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.EventOptionView;
import artframework.context.EventView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapView;
import artframework.context.MonsterIntentView;
import artframework.context.RestView;
import artframework.context.RewardView;
import artframework.context.SelectView;
import artframework.context.ShopView;
import artframework.context.SurfaceIds;
import artframework.context.TopPanelView;
import artframework.context.TreasureView;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentSafety;
import artframework.sts1.PresentLevel;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import artframework.sts1.patch.EventRenderPatches;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventRenderPatchesTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
        PresentSafety.resetForTests();
    }

    private void mountedEvent() {
        mountedEvent("event");
    }

    private void mountedEvent(String scene) {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        EventView event = EventView.of("Neow",
                Arrays.asList(EventOptionView.of(0, "Talk", true), EventOptionView.of(1, "Leave", true)));
        backend.publish(
                ContextFrame.ofFull(
                        1L, 1L, scene, null, ControlsView.empty(), MapView.empty(),
                        event, SelectView.empty(), RewardView.empty(), RestView.empty(),
                        TreasureView.empty(), ShopView.empty(), TopPanelView.empty(),
                        MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.component(SurfaceIds.EVENT).mount();
    }

    @Test
    public void fullReadySuppressesNativeEventRender() {
        mountedEvent();
        FullPresentMode.setEventLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        SpireReturn<Void> result = EventRenderPatches.ObserveNativeEventRender.Prefix(null, null);

        assertTrue("FULL + mounted + event + ready executor must suppress GenericEventDialog.render",
                result.isPresent());
    }

    @Test
    public void offContinuesNativeEventRender() {
        mountedEvent();

        SpireReturn<Void> result = EventRenderPatches.ObserveNativeEventRender.Prefix(null, null);

        assertFalse("OFF must let GenericEventDialog.render continue", result.isPresent());
    }

    @Test
    public void fullWithoutExecutorContinuesNativeEventRender() {
        mountedEvent();
        FullPresentMode.setEventLevel(PresentLevel.FULL);

        SpireReturn<Void> result = EventRenderPatches.ObserveNativeEventRender.Prefix(null, null);

        assertFalse("FULL without a ready executor must fall back to native render",
                result.isPresent());
    }

    @Test
    public void fullReadyButUnmountedContinuesAndDoesNotDelegateEventRender() {
        mountedEvent();
        ArtFramework.component(SurfaceIds.EVENT).unmount();
        FullPresentMode.setEventLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        SpireReturn<Void> result = EventRenderPatches.ObserveNativeEventRender.Prefix(null, null);

        assertFalse("unmounted event surface must continue native render", result.isPresent());
        assertNoDelegatedCoverage();
    }

    @Test
    public void fullReadyButSceneMismatchContinuesAndDoesNotDelegateEventRender() {
        mountedEvent("combat");
        FullPresentMode.setEventLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        SpireReturn<Void> result = EventRenderPatches.ObserveNativeEventRender.Prefix(null, null);

        assertFalse("event render outside event scene must continue native render", result.isPresent());
        assertNoDelegatedCoverage();
    }

    @Test
    public void panicFailOpenContinuesAndDoesNotDelegateEventRender() {
        mountedEvent();
        FullPresentMode.setEventLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        PresentSafety.panic("event-test");

        SpireReturn<Void> result = EventRenderPatches.ObserveNativeEventRender.Prefix(null, null);

        assertFalse("panic fail-open must continue GenericEventDialog.render", result.isPresent());
        Map<String, Object> probe = NativeRenderBridge.probeSlice();
        assertEquals(Integer.valueOf(1), probe.get("failOpen"));
        assertEquals(Integer.valueOf(0), probe.get("delegateToArt"));
    }

    @Test
    public void unknownOwnerFailOpenDoesNotCountAsDelegatedEventCoverage() {
        mountedEvent();
        FullPresentMode.setEventLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        RenderDisposition unknown = NativeRenderBridge.beginSurface(
                "sts1.event.unknown", "native.Event", "render", "unknown");

        assertEquals(RenderDisposition.Mode.FAIL_OPEN, unknown.mode);
        assertTrue(unknown.nativeContinuation);
        assertEquals(Integer.valueOf(1), NativeRenderBridge.strictReport().get("runtimeUNKNOWN"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.probeSlice().get("delegateToArt"));
    }

    private static void assertNoDelegatedCoverage() {
        Map<String, Object> probe = NativeRenderBridge.probeSlice();
        assertEquals(Integer.valueOf(0), probe.get("delegateToArt"));
        assertEquals(Integer.valueOf(0), probe.get("evidenceCount"));
        assertEquals(Integer.valueOf(0), probe.get("delegatedWithoutEvidence"));
    }
}
