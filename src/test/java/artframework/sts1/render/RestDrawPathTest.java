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
        assertEquals(Integer.valueOf(2), probe.get("drawCount"));
        assertEquals(Boolean.FALSE, probe.get("suppressNativeRest"));
        // NRO-04 D07 follow-up: native CampfireUI.render draws no campfire site title.
        assertEquals(Boolean.FALSE, probe.get("hasTitle"));
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
        // NRO-04 D07 follow-up: no synthetic title row (native CampfireUI.render draws none).
        assertEquals(7, lines.size());
        assertEquals(ResourceIds.UI_CAMPFIRE_REST_OPTION, lines.get(0).resourceId);
        assertEquals(ResourceIds.UI_CAMPFIRE_SMITH_OPTION, lines.get(1).resourceId);
        assertEquals(ResourceIds.UI_CAMPFIRE_DIG_OPTION, lines.get(2).resourceId);
        assertEquals(ResourceIds.UI_CAMPFIRE_RECALL_OPTION, lines.get(3).resourceId);
        assertEquals(ResourceIds.UI_CAMPFIRE_TOKE_OPTION, lines.get(4).resourceId);
        assertEquals(ResourceIds.UI_CAMPFIRE_OTHER_OPTION, lines.get(5).resourceId);
        assertEquals(ResourceIds.UI_CAMPFIRE_DISABLED_OPTION, lines.get(6).resourceId);
        assertEquals("rest-dig-option", lines.get(2).role);
        assertEquals("rest-recall-option", lines.get(3).role);
        assertEquals("rest-toke-option", lines.get(4).role);
        assertEquals("rest-other-option", lines.get(5).role);
        assertTrue(!lines.get(6).enabled);
        assertEquals(Integer.valueOf(7), RestDrawPath.probeSlice().get("drawCount"));
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
    }

    @Test
    public void optionButtonsUseNativeCampfireGridAtUnitScale() throws Exception {
        // Verified native CampfireUI.renderCampfireButtons geometry at scale=xScale=1,
        // WIDTH=1920, HEIGHT=1080: BUTTON_START_X=WIDTH*0.416f=798.72, BUTTON_SPACING_X=300,
        // BUTTON_START_Y=HEIGHT/2+180=720, BUTTON_SPACING_Y=-200, BUTTON_EXTRA_SPACING_Y=-70.
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int previousHeight = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                    Integer.valueOf(1920));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT",
                    Integer.valueOf(1080));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(1f));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "xScale", Float.valueOf(1f));
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();
            CombatInputRouter.resetForTests();

            publishRestFrame(); // rest + smith -> 2 options

            java.util.List<RestDrawPath.DrawItem> items = RestDrawPath.buildFromProjection();
            assertEquals(2, items.size());

            // button 0: left column, top row
            assertEquals(1920f * 0.416f, items.get(0).centerX, 0.01f); // 798.72
            assertEquals(720f, items.get(0).centerY, 0.01f);
            // Native at-rest icon size is NORM_SCALE = 0.9f * Settings.scale, so 256*0.9 = 230.4.
            assertEquals(230.4f, items.get(0).w, 0.01f);
            assertEquals(230.4f, items.get(0).h, 0.01f);
            assertEquals(1920f * 0.416f - 115.2f, items.get(0).x, 0.01f);
            assertEquals(720f - 115.2f, items.get(0).y, 0.01f);

            // button 1: right column, top row
            assertEquals(1920f * 0.416f + 300f, items.get(1).centerX, 0.01f); // 1098.72
            assertEquals(720f, items.get(1).centerY, 0.01f);

            Map<String, Object> probe = RestDrawPath.probeSlice();
            assertEquals(Integer.valueOf(2), probe.get("buttonCount"));
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> buttons =
                    (java.util.List<Map<String, Object>>) probe.get("buttons");
            assertEquals(2, buttons.size());
            assertEquals("rest", buttons.get(0).get("id"));
            assertEquals(1920f * 0.416f, ((Float) buttons.get(0).get("centerX")).floatValue(),
                    0.01f);
            assertEquals(720f, ((Float) buttons.get(0).get("centerY")).floatValue(), 0.01f);
            assertEquals(230.4f, ((Float) buttons.get(0).get("w")).floatValue(), 0.01f);
        } finally {
            restoreSettings(previousWidth, previousHeight, previousScale, previousXScale);
        }
    }

    @Test
    public void optionButtonUsesNativeCampfireGridForSingleOption() throws Exception {
        // 1 button: index 0 -> left column, top row; native at-rest size 256*0.9=230.4.
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int previousHeight = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                    Integer.valueOf(1920));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT",
                    Integer.valueOf(1080));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(1f));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "xScale", Float.valueOf(1f));
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();
            CombatInputRouter.resetForTests();

            publishRestFrameWith(java.util.Collections.singletonList(
                    artframework.context.RestView.RestOptionView.of("rest", "Rest")));

            java.util.List<RestDrawPath.DrawItem> items = RestDrawPath.buildFromProjection();
            assertEquals(1, items.size());
            assertEquals(RestDrawPath.buttonCenterX(0, 1920f, 1f), items.get(0).centerX, 0.01f);
            assertEquals(1920f * 0.416f, items.get(0).centerX, 0.01f);
            assertEquals(720f, items.get(0).centerY, 0.01f);
            assertEquals(230.4f, items.get(0).w, 0.01f);
            assertEquals(230.4f, items.get(0).h, 0.01f);
            // top-left = center - NORM_ICON/2
            assertEquals(1920f * 0.416f - 115.2f, items.get(0).x, 0.01f);
            assertEquals(720f - 115.2f, items.get(0).y, 0.01f);
            assertEquals(Integer.valueOf(1), RestDrawPath.probeSlice().get("buttonCount"));
        } finally {
            restoreSettings(previousWidth, previousHeight, previousScale, previousXScale);
        }
    }

    @Test
    public void optionButtonsUseNativeCampfireGridForThreeButtons() throws Exception {
        // 3 buttons: index 2 wraps to the SECOND row -> row 1: y = START_Y - 200 - 70 = 450.
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int previousHeight = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                    Integer.valueOf(1920));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT",
                    Integer.valueOf(1080));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(1f));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "xScale", Float.valueOf(1f));
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();
            CombatInputRouter.resetForTests();

            publishRestFrameWith(java.util.Arrays.asList(
                    artframework.context.RestView.RestOptionView.of("rest", "Rest"),
                    artframework.context.RestView.RestOptionView.of("smith", "Smith"),
                    artframework.context.RestView.RestOptionView.of("dig", "Dig")));

            java.util.List<RestDrawPath.DrawItem> items = RestDrawPath.buildFromProjection();
            assertEquals(3, items.size());
            // index 2 -> left column, row 1
            assertEquals(1920f * 0.416f, items.get(2).centerX, 0.01f);
            assertEquals(720f - 200f - 70f, items.get(2).centerY, 0.01f); // 450
            assertEquals("index 2 keeps the native NORM_SCALE icon size", 230.4f,
                    items.get(2).w, 0.01f);
        } finally {
            restoreSettings(previousWidth, previousHeight, previousScale, previousXScale);
        }
    }

    @Test
    public void optionButtonsCatchScaleVersusXScaleMixups() throws Exception {
        // scale=1.25 (icon size + y grid + y spacing) but xScale=1.5 (x spacing only), so a
        // scale/xScale axis swap changes the expected numbers.
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int previousHeight = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                    Integer.valueOf(1920));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT",
                    Integer.valueOf(1080));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(1.25f));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "xScale", Float.valueOf(1.5f));
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();
            CombatInputRouter.resetForTests();

            publishRestFrame();

            java.util.List<RestDrawPath.DrawItem> items = RestDrawPath.buildFromProjection();
            assertEquals("icon size uses NORM_SCALE (0.9) and scale on BOTH axes",
                    230.4f * 1.25f, items.get(0).w, 0.01f);
            assertEquals(230.4f * 1.25f, items.get(0).h, 0.01f);
            // right-column x spacing uses xScale, not scale
            assertEquals(1920f * 0.416f + 300f * 1.5f, items.get(1).centerX, 0.01f);
            // left-column x is START_X, invariant of xScale (a START_X*xScale bug would fail here)
            assertEquals("left-column centerX stays WIDTH*0.416f, independent of xScale",
                    1920f * 0.416f, items.get(0).centerX, 0.01f);
            // top-row y uses scale
            assertEquals(1080f / 2f + 180f * 1.25f, items.get(0).centerY, 0.01f);
        } finally {
            restoreSettings(previousWidth, previousHeight, previousScale, previousXScale);
        }
    }

    @Test
    public void optionLabelsAnchorBelowTheIconCenterPerNativeFormula() throws Exception {
        // NRO-04 D07 follow-up: native AbstractCampfireOption.render draws the label with
        // FontHelper.renderFontCenteredTopAligned(..., hb.cX,
        //     hb.cY - 60f*scale - 50f*scale*(scale/scale), ...) -> at rest
        //     hb.cY - 105f*scale, TOP-ALIGNED, X = hb.cX. So labelAnchorX = icon centerX and
        // labelAnchorY = centerY - 105*scale (BELOW the icon center).
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int previousHeight = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        setUnitScale();
        try {
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();
            CombatInputRouter.resetForTests();

            publishRestFrame(); // rest + smith -> buttons 0 (left) and 1 (right), row 0

            java.util.List<RestDrawPath.DrawItem> items = RestDrawPath.buildFromProjection();
            assertEquals(2, items.size());

            float expectedAnchorY = 720f - 105f; // centerY(720) - 105*scale(1)
            assertEquals("label X is the icon center",
                    items.get(0).centerX, items.get(0).labelAnchorX, 0.01f);
            assertEquals(expectedAnchorY, items.get(0).labelAnchorY, 0.01f);
            assertTrue("label anchor must be BELOW the icon center",
                    items.get(0).labelAnchorY < items.get(0).centerY);
            assertEquals(items.get(1).centerX, items.get(1).labelAnchorX, 0.01f);
            assertEquals(expectedAnchorY, items.get(1).labelAnchorY, 0.01f);

            Map<String, Object> probe = RestDrawPath.probeSlice();
            assertEquals(Boolean.FALSE, probe.get("hasTitle"));
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> buttons =
                    (java.util.List<Map<String, Object>>) probe.get("buttons");
            assertEquals(2, buttons.size());
            assertEquals(expectedAnchorY,
                    ((Float) buttons.get(0).get("labelAnchorY")).floatValue(), 0.01f);
            assertEquals(((Float) buttons.get(0).get("centerX")).floatValue(),
                    ((Float) buttons.get(0).get("labelAnchorX")).floatValue(), 0.01f);
        } finally {
            restoreSettings(previousWidth, previousHeight, previousScale, previousXScale);
        }
    }

    @Test
    public void optionLabelAnchorScalesWithNativeScale() throws Exception {
        // scale=1.25: anchorY = centerY - 105*1.25.
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int previousHeight = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                Integer.valueOf(1920));
        setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT",
                Integer.valueOf(1080));
        setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(1.25f));
        setStaticField(com.megacrit.cardcrawl.core.Settings.class, "xScale", Float.valueOf(1.5f));
        try {
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();
            CombatInputRouter.resetForTests();

            publishRestFrame();

            java.util.List<RestDrawPath.DrawItem> items = RestDrawPath.buildFromProjection();
            float centerY = 1080f / 2f + 180f * 1.25f; // 765
            assertEquals(centerY - 105f * 1.25f, items.get(0).labelAnchorY, 0.01f);
            assertTrue(items.get(0).labelAnchorY < items.get(0).centerY);
        } finally {
            restoreSettings(previousWidth, previousHeight, previousScale, previousXScale);
        }
    }

    private static void setUnitScale() {
        setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                Integer.valueOf(1920));
        setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT",
                Integer.valueOf(1080));
        setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(1f));
        setStaticField(com.megacrit.cardcrawl.core.Settings.class, "xScale", Float.valueOf(1f));
    }

    private void publishRestFrameWith(java.util.List<RestView.RestOptionView> options) {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        artframework.context.RestView rest = artframework.context.RestView.of(options);
        backend.publish(
                ContextFrame.ofFull(
                        1L, 1L, "rest", null, ControlsView.empty(), MapView.empty(),
                        EventView.empty(), SelectView.empty(), RewardView.empty(), rest,
                        TreasureView.empty(), ShopView.empty(), TopPanelView.empty(),
                        MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    private static void restoreSettings(int width, int height, float scale, float xScale) {
        setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH", Integer.valueOf(width));
        setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT", Integer.valueOf(height));
        setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(scale));
        setStaticField(com.megacrit.cardcrawl.core.Settings.class, "xScale", Float.valueOf(xScale));
    }

    private static void setStaticField(Class<?> owner, String name, Object value) {
        try {
            java.lang.reflect.Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (Exception failure) {
            throw new AssertionError("could not set static field " + owner + "." + name, failure);
        }
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
