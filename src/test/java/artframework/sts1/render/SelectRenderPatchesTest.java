package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.CardRef;
import artframework.context.CardView;
import artframework.context.CardZone;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
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
import artframework.sts1.PresentLevel;
import artframework.sts1.PresentSafety;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import artframework.sts1.patch.SelectRenderPatches;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SelectRenderPatchesTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
        PresentSafety.resetForTests();
    }

    private void mountedGridSelect() {
        mountedGridSelect("select");
    }

    private void mountedGridSelect(String scene) {
        mountedSelect(scene, SurfaceIds.SELECT_GRID,
                SelectView.grid(
                        Collections.singletonList(
                                CardView.builder(new CardRef("g1", "Strike_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(0)
                                        .selected(true)
                                        .build()),
                        Collections.singletonList("g1"), true, true));
    }

    private void mountedHandSelect() {
        mountedSelect("select", SurfaceIds.SELECT_HAND,
                SelectView.hand(
                        Collections.singletonList(
                                CardView.builder(new CardRef("h1", "Bash"))
                                        .zone(CardZone.HAND)
                                        .slot(0)
                                        .selected(true)
                                        .build()),
                        Collections.singletonList("h1"), true, true));
    }

    private void mountedSelect(String scene, String surfaceId, SelectView select) {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.ofFull(
                        1L, 1L, scene, null, ControlsView.empty(), MapView.empty(),
                        EventView.empty(), select, RewardView.empty(), RestView.empty(),
                        TreasureView.empty(), ShopView.empty(), TopPanelView.empty(),
                        MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.component(surfaceId).mount();
    }

    @Test
    public void fullReadySuppressesNativeGridSelectRender() {
        mountedGridSelect();
        FullPresentMode.setSelectLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        SpireReturn<Void> result =
                SelectRenderPatches.ObserveNativeGridSelectRender.Prefix(null, null);

        assertTrue("FULL + mounted + select + ready executor must suppress GridCardSelectScreen.render",
                result.isPresent());
    }

    @Test
    public void offContinuesNativeGridSelectRender() {
        mountedGridSelect();

        SpireReturn<Void> result =
                SelectRenderPatches.ObserveNativeGridSelectRender.Prefix(null, null);

        assertFalse("OFF must let GridCardSelectScreen.render continue", result.isPresent());
    }

    @Test
    public void fullWithoutExecutorContinuesNativeGridSelectRender() {
        mountedGridSelect();
        FullPresentMode.setSelectLevel(PresentLevel.FULL);

        SpireReturn<Void> result =
                SelectRenderPatches.ObserveNativeGridSelectRender.Prefix(null, null);

        assertFalse("FULL without a ready executor must fall back to native render",
                result.isPresent());
    }

    @Test
    public void fullReadySuppressesNativeHandSelectRender() {
        mountedHandSelect();
        FullPresentMode.setSelectLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        SpireReturn<Void> result =
                SelectRenderPatches.ObserveNativeHandSelectRender.Prefix(null, null);

        assertTrue("FULL + mounted + select + ready executor must suppress HandCardSelectScreen.render",
                result.isPresent());
    }

    @Test
    public void fullReadyButUnmountedContinuesAndDoesNotDelegateGridSelectRender() {
        mountedGridSelect();
        ArtFramework.component(SurfaceIds.SELECT_GRID).unmount();
        FullPresentMode.setSelectLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        SpireReturn<Void> result =
                SelectRenderPatches.ObserveNativeGridSelectRender.Prefix(null, null);

        assertFalse("unmounted grid select surface must continue native render", result.isPresent());
        assertNoDelegatedCoverage();
    }

    @Test
    public void fullReadyButSceneMismatchContinuesAndDoesNotDelegateGridSelectRender() {
        mountedGridSelect("combat");
        FullPresentMode.setSelectLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        SpireReturn<Void> result =
                SelectRenderPatches.ObserveNativeGridSelectRender.Prefix(null, null);

        assertFalse("select render outside select scene must continue native render", result.isPresent());
        assertNoDelegatedCoverage();
    }

    @Test
    public void panicFailOpenContinuesAndDoesNotDelegateGridSelectRender() {
        mountedGridSelect();
        FullPresentMode.setSelectLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        PresentSafety.panic("select-test");

        SpireReturn<Void> result =
                SelectRenderPatches.ObserveNativeGridSelectRender.Prefix(null, null);

        assertFalse("panic fail-open must continue GridCardSelectScreen.render", result.isPresent());
        Map<String, Object> probe = NativeRenderBridge.probeSlice();
        assertEquals(Integer.valueOf(1), probe.get("failOpen"));
        assertEquals(Integer.valueOf(0), probe.get("delegateToArt"));
    }

    @Test
    public void unknownOwnerFailOpenDoesNotCountAsDelegatedSelectCoverage() {
        mountedGridSelect();
        FullPresentMode.setSelectLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        RenderDisposition unknown = NativeRenderBridge.beginSurface(
                "sts1.select.unknown", "native.Select", "render", "unknown");

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
