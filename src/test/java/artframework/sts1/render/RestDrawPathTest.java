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

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RestDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
    }

    private void publishRestFrame() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        artframework.context.RestView rest = artframework.context.RestView.of(
                Arrays.asList(
                        artframework.context.RestView.RestOptionView.of("rest", "Rest"),
                        artframework.context.RestView.RestOptionView.of("smith", "Smith")));
        backend.publish(
                ContextFrame.ofFull(
                        1L, 1L, "rest", null, ControlsView.empty(), MapView.empty(),
                        EventView.empty(), SelectView.empty(), RewardView.empty(), rest,
                        TreasureView.empty(), ShopView.empty(), TopPanelView.empty(),
                        MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    @Test
    public void buildFromProjectionListsOptions() {
        publishRestFrame();
        java.util.List<RestDrawPath.DrawItem> items = RestDrawPath.buildFromProjection();
        assertEquals(2, items.size());
        assertEquals("rest", items.get(0).id);
        assertEquals("Rest", items.get(0).label);
        assertTrue(items.get(0).visible);
        assertTrue(items.get(0).enabled);
        assertEquals("smith", items.get(1).id);
        assertEquals("Smith", items.get(1).label);
        Map<String, Object> probe = RestDrawPath.probeSlice();
        assertEquals(Integer.valueOf(2), probe.get("count"));
        assertEquals("drawCount must come from live projected options, not a constant",
                probe.get("count"), probe.get("drawCount"));
        assertEquals(Boolean.FALSE, probe.get("suppressNativeRest"));
    }

    @Test
    public void fullMountedRestDrawsAndSuppressesNative() {
        publishRestFrame();
        FullPresentMode.setRestLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.REST).mount();
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue(plan.shouldDraw(SurfaceIds.REST));
        // Slice C phase 2: minimal campfire chrome supplies real pixels, so suppression is safe.
        assertTrue(plan.shouldSuppressNative(SurfaceIds.REST));
        assertTrue(RestDrawPath.shouldSuppressNativeRest());
        assertEquals(Boolean.TRUE, RestDrawPath.probeSlice().get("suppressNativeRest"));
    }

    @Test
    public void offDoesNotSuppress() {
        publishRestFrame();
        ArtFramework.component(SurfaceIds.REST).mount();
        assertFalse(RestDrawPath.shouldSuppressNativeRest());
    }
}
