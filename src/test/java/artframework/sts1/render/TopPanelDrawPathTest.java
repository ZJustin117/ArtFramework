package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapView;
import artframework.context.SurfaceIds;
import artframework.context.TopPanelView;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.assets.Sts1VanillaCatalog;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TopPanelDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
    }

    private void mountedCombat() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(ContextFrame.of(1L, 1L, "combat", Arrays.asList(),
                ControlsView.combat(3, 1, 0, 0, 0, true, true), MapView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.component(SurfaceIds.TOP_PANEL).mount();
    }

    @Test
    public void suppressesNativeTopPanelOnlyWhenFullReady() {
        mountedCombat();
        assertFalse("OFF keeps native renderer", TopPanelDrawPath.shouldSuppressNativeTopPanel());

        FullPresentMode.setTopPanelLevel(PresentLevel.FULL);
        assertFalse("FULL without executor stays native", TopPanelDrawPath.shouldSuppressNativeTopPanel());

        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        assertTrue("FULL + mounted + combat + ready executor suppresses native",
                TopPanelDrawPath.shouldSuppressNativeTopPanel());

        Sts1RenderPipeline.setOverlayObserve(true);
        assertFalse("overlay-observe forces native visibility", TopPanelDrawPath.shouldSuppressNativeTopPanel());
    }

    @Test
    public void surfaceDrawPlanSuppressesNativeTopPanelWhenFullMountedCombat() {
        mountedCombat();
        FullPresentMode.setTopPanelLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue(plan.shouldSuppressNative(SurfaceIds.TOP_PANEL));
    }

    @Test
    public void probeSlice() {
        mountedCombat();
        FullPresentMode.setTopPanelLevel(PresentLevel.OBSERVE);
        Map<String, Object> m = TopPanelDrawPath.probeSlice();
        assertEquals("OBSERVE", m.get("presentLevel"));
        assertEquals(Boolean.FALSE, m.get("suppressNativeTopPanel"));
    }

    @Test
    public void buildsStableDrawableHudItemsWithExplicitResourcesAndGeometry() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(ContextFrame.ofFull(1L, 1L, "combat", Arrays.asList(),
                ControlsView.combat(3, 1, 0, 0, 0, true, true), MapView.empty(),
                artframework.context.EventView.empty(), artframework.context.SelectView.empty(),
                artframework.context.RewardView.empty(), artframework.context.RestView.empty(),
                artframework.context.TreasureView.empty(), artframework.context.ShopView.empty(),
                TopPanelView.of(66, 80, 123, 9, 17, "Ironclad", "Vulnerable"),
                artframework.context.MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());

        List<TopPanelDrawPath.DrawItem> items = TopPanelDrawPath.buildFromProjection();

        assertEquals(7, items.size());
        assertEquals("top_panel.bar", items.get(0).id);
        assertEquals(ResourceIds.UI_TOP_PANEL_BAR, items.get(0).resourceId);
        assertEquals("top_panel.hp", items.get(1).id);
        assertEquals("HP 66/80", items.get(1).text);
        assertEquals(ResourceIds.UI_TOP_PANEL_HP, items.get(1).resourceId);
        assertEquals("top_panel.status", items.get(5).id);
        assertEquals("Vulnerable", items.get(5).text);
        assertEquals(ResourceIds.UI_TOP_PANEL_STATUS, items.get(5).resourceId);
        assertEquals("top_panel.settings", items.get(6).id);
        assertEquals(ResourceIds.UI_TOP_PANEL_SETTINGS, items.get(6).resourceId);
        assertEquals("settings gear carries no label text", "", items.get(6).text);
        assertTrue(items.get(1).bounds(1920f, 1080f).width > 0f);
        assertEquals(items.size(), TopPanelDrawPath.materializedDrawCount());
    }

    @Test
    public void settingsGearUsesNativeTopPanelSettingsIconGeometry() {
        // Native TopPanel.renderSettingsIcon (verified bytecode):
        //   SETTINGS_X = Settings.WIDTH - (ICON_W + TOP_RIGHT_PAD_X)
        //   ICON_W = 64f*Settings.scale, TOP_RIGHT_PAD_X = 10f*Settings.scale
        //   ICON_Y = Settings.HEIGHT - ICON_W
        //   draw at (SETTINGS_X - 32f + 32f*scale, ICON_Y - 32f + 32f*scale), w = h = 64f*scale.
        // Settings.scale is a static mutable float; pin it to a non-1 value to guard the formula
        // end-to-end (mirrors Sts1VfxArtRendererTest).
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float scale = 1.25f;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(scale));
            // Guard against a prior test class leaving a higher sceneEpoch that would make our
            // frame 1/1 stale when this method runs first in the class.
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();
            CombatInputRouter.resetForTests();
            float width = com.megacrit.cardcrawl.core.Settings.WIDTH;
            float height = com.megacrit.cardcrawl.core.Settings.HEIGHT;

            FakeSignalBackend backend = new FakeSignalBackend();
            backend.installSignals();
            backend.publish(ContextFrame.ofFull(1L, 1L, "combat", Arrays.asList(),
                    ControlsView.combat(3, 1, 0, 0, 0, true, true), MapView.empty(),
                    artframework.context.EventView.empty(), artframework.context.SelectView.empty(),
                    artframework.context.RewardView.empty(), artframework.context.RestView.empty(),
                    artframework.context.TreasureView.empty(), artframework.context.ShopView.empty(),
                    TopPanelView.of(66, 80, 123, 9, 17, "Ironclad", "Vulnerable"),
                    artframework.context.MonsterIntentView.empty(), null));
            ArtFramework.publishFrame(backend.currentFrame());

            List<TopPanelDrawPath.DrawItem> items = TopPanelDrawPath.buildFromProjection();
            TopPanelDrawPath.DrawItem settings = null;
            for (TopPanelDrawPath.DrawItem item : items) {
                if ("top_panel.settings".equals(item.id)) {
                    settings = item;
                }
            }
            assertTrue("settings item is supplied for an available top panel",
                    settings != null);
            assertEquals(ResourceIds.UI_TOP_PANEL_SETTINGS, settings.resourceId);

            float iconW = 64f * scale;
            float topRightPadX = 10f * scale;
            float settingsX = width - (iconW + topRightPadX);
            float expectedX = settingsX - 32f + 32f * scale;
            float expectedY = height - 64f * scale - 32f + 32f * scale;
            artframework.component.Rect bounds = settings.bounds(width, height);
            assertEquals("x = WIDTH - (64*scale + 10*scale) - 32 + 32*scale", expectedX, bounds.x, 0.01f);
            assertEquals("y = HEIGHT - 64*scale - 32 + 32*scale", expectedY, bounds.y, 0.01f);
            assertEquals("w = 64*scale", iconW, bounds.width, 0.01f);
            assertEquals("h = 64*scale", iconW, bounds.height, 0.01f);
        } finally {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale",
                    Float.valueOf(previousScale));
        }
    }

    @Test
    public void settingsGearResourceIsMappedToNativeIconTexture() {
        java.util.Map<String, String> catalog =
                artframework.sts1.assets.Sts1VanillaCatalog.catalog();
        assertEquals("sts1:images/ui/topPanel/settings.png",
                catalog.get(ResourceIds.UI_TOP_PANEL_SETTINGS));
        assertTrue(Sts1VanillaCatalog.isKnown(ResourceIds.UI_TOP_PANEL_SETTINGS));
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
}
