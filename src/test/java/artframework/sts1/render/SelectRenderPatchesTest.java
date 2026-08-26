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
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import artframework.sts1.patch.SelectRenderPatches;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SelectRenderPatchesTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
    }

    private void mountedGridSelect() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        SelectView select = SelectView.grid(
                Collections.singletonList(
                        CardView.builder(new CardRef("g1", "Strike_R"))
                                .zone(CardZone.SELECT)
                                .slot(0)
                                .selected(true)
                                .build()),
                Collections.singletonList("g1"), true, true);
        backend.publish(
                ContextFrame.ofFull(
                        1L, 1L, "select", null, ControlsView.empty(), MapView.empty(),
                        EventView.empty(), select, RewardView.empty(), RestView.empty(),
                        TreasureView.empty(), ShopView.empty(), TopPanelView.empty(),
                        MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.component(SurfaceIds.SELECT_GRID).mount();
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
}
