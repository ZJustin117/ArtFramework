package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
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
import artframework.ecs.EntityId;
import artframework.presentation.BoundsComponent;
import artframework.presentation.DrawComponent;
import artframework.presentation.NodeIdentityComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRegistry;
import artframework.presentation.VisibilityComponent;
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
        assertEquals("rest-option", items.get(0).role);
        assertEquals(ResourceIds.UI_CAMPFIRE_REST_OPTION, items.get(0).resourceId);
        assertEquals("smith", items.get(1).id);
        assertEquals("Smith", items.get(1).label);
        assertEquals("rest-smith-option", items.get(1).role);
        assertEquals(ResourceIds.UI_CAMPFIRE_SMITH_OPTION, items.get(1).resourceId);
        Map<String, Object> probe = RestDrawPath.probeSlice();
        assertEquals(Integer.valueOf(2), probe.get("count"));
        assertEquals("drawCount must come from visible chrome rows, not raw options",
                probe.get("chromeLineCount"), probe.get("drawCount"));
        assertEquals(Integer.valueOf(3), probe.get("drawCount"));
        assertEquals(Boolean.FALSE, probe.get("suppressNativeRest"));
    }

    @Test
    public void restRowsUsePanelOptionAndFallbackResources() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        RestView rest = RestView.of(Arrays.asList(
                new RestView.RestOptionView("rest", "Rest", true, true),
                new RestView.RestOptionView("smith", "Smith", true, true),
                new RestView.RestOptionView("dig", "Dig", true, true),
                new RestView.RestOptionView("recall", "Recall", true, true),
                new RestView.RestOptionView("toke", "Toke", true, true),
                new RestView.RestOptionView("lift", "Lift", true, true),
                new RestView.RestOptionView("disabled", "Disabled", false, true),
                new RestView.RestOptionView("hidden", "Hidden", true, false)));
        backend.publish(ContextFrame.ofFull(
                1L, 1L, "rest", null, ControlsView.empty(), MapView.empty(), EventView.empty(),
                SelectView.empty(), RewardView.empty(), rest, TreasureView.empty(), ShopView.empty(),
                TopPanelView.empty(), MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());

        java.util.List<RoomChromeLine> lines = RestDrawPath.chromeLines();
        assertEquals(8, lines.size());
        assertEquals(ResourceIds.UI_CAMPFIRE_PANEL, lines.get(0).resourceId);
        assertEquals(ResourceIds.UI_CAMPFIRE_REST_OPTION, lines.get(1).resourceId);
        assertEquals(ResourceIds.UI_CAMPFIRE_SMITH_OPTION, lines.get(2).resourceId);
        assertEquals(ResourceIds.UI_CAMPFIRE_DIG_OPTION, lines.get(3).resourceId);
        assertEquals(ResourceIds.UI_CAMPFIRE_RECALL_OPTION, lines.get(4).resourceId);
        assertEquals(ResourceIds.UI_CAMPFIRE_TOKE_OPTION, lines.get(5).resourceId);
        assertEquals(ResourceIds.UI_CAMPFIRE_OTHER_OPTION, lines.get(6).resourceId);
        assertEquals(ResourceIds.UI_CAMPFIRE_DISABLED_OPTION, lines.get(7).resourceId);
        assertEquals("rest-dig-option", lines.get(3).role);
        assertEquals("rest-recall-option", lines.get(4).role);
        assertEquals("rest-toke-option", lines.get(5).role);
        assertEquals("rest-other-option", lines.get(6).role);
        assertTrue(!lines.get(7).enabled);
        assertEquals(Integer.valueOf(8), RestDrawPath.probeSlice().get("drawCount"));
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

    @Test
    public void restChromeRowsSyncResourceGeometryAndStateToC2() throws Exception {
        publishRestFrame();
        FullPresentMode.setRestLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.REST).mount();

        java.lang.reflect.Method prepare = Sts1SurfaceRenderer.class.getDeclaredMethod(
                "prepareRestVisuals", SurfaceDrawPlan.class);
        prepare.setAccessible(true);
        prepare.invoke(null, Sts1RenderPipeline.plan());

        java.util.List<RoomChromeLine> lines = RestDrawPath.chromeLines();
        assertC2Line(lines.get(0));
        assertC2Line(lines.get(1));
        assertC2Line(lines.get(2));
    }

    private static void assertC2Line(RoomChromeLine line) {
        PresentationContext context = PresentationRegistry.context("c2-surfaces");
        EntityId entity = null;
        for (EntityId candidate : context.entities()) {
            NodeIdentityComponent identity = context.world().get(candidate, NodeIdentityComponent.class);
            if (identity != null && "sts1.visual.sts1.rest".equals(identity.key.scope)
                    && line.id.equals(identity.key.localId)) {
                entity = candidate;
                break;
            }
        }
        assertTrue("missing C2 visual " + line.id, entity != null);
        DrawComponent draw = context.world().get(entity, DrawComponent.class);
        assertEquals(line.role, draw.role);
        assertEquals(line.resourceId, draw.resourceId);
        assertEquals(line.text, draw.text);
        BoundsComponent bounds = context.world().get(entity, BoundsComponent.class);
        assertEquals(line.x, bounds.rect.x, 0.01f);
        assertEquals(line.y, bounds.rect.y, 0.01f);
        assertEquals(line.w, bounds.rect.width, 0.01f);
        assertEquals(line.h, bounds.rect.height, 0.01f);
        VisibilityComponent visibility = context.world().get(entity, VisibilityComponent.class);
        assertEquals(line.visible, visibility.visible);
    }
}
