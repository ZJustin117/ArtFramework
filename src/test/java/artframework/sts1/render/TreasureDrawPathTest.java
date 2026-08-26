package artframework.sts1.render;

import artframework.api.ArtFramework;
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
import org.junit.After;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TreasureDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
    }

    private void publishTreasureFrame() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        TreasureView treasure = TreasureView.closed();
        backend.publish(
                ContextFrame.ofFull(
                        1L, 1L, "treasure", null, ControlsView.empty(), MapView.empty(),
                        EventView.empty(), SelectView.empty(), RewardView.empty(),
                        RestView.empty(), treasure, ShopView.empty(), TopPanelView.empty(),
                        MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    @Test
    public void probeSliceReportsClosedChest() {
        publishTreasureFrame();
        Map<String, Object> probe = TreasureDrawPath.probeSlice();
        assertEquals(Boolean.FALSE, probe.get("chestOpen"));
        assertEquals(Boolean.TRUE, probe.get("canOpen"));
        assertEquals(Boolean.FALSE, probe.get("suppressNativeTreasure"));
    }

    @Test
    public void fullMountedTreasureDrawsAndSuppressesNative() {
        publishTreasureFrame();
        FullPresentMode.setTreasureLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.TREASURE).mount();
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue(plan.shouldDraw(SurfaceIds.TREASURE));
        assertTrue(plan.shouldSuppressNative(SurfaceIds.TREASURE));
        assertTrue(TreasureDrawPath.shouldSuppressNativeTreasure());
        assertEquals(Boolean.TRUE, TreasureDrawPath.probeSlice().get("suppressNativeTreasure"));
    }

    @Test
    public void offDoesNotSuppress() {
        publishTreasureFrame();
        ArtFramework.component(SurfaceIds.TREASURE).mount();
        assertFalse(TreasureDrawPath.shouldSuppressNativeTreasure());
    }
}
