package artframework.core;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.api.UiOpResult;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.component.WidgetSessions;
import artframework.render.LightwaveEffect;
import artframework.render.RenderHost;
import artframework.render.RenderTarget;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Automated contract for lightwave_demo: theme, effect bind, controls, anim, ops.
 * Does not assert pixels; probe/geometry/behavior only.
 */
public class LightwaveDemoContractTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void openBindsLightwaveOnPanelWithLayoutSize() {
        openDemo();
        RenderHost host = ArtFramework.render();
        RenderTarget panel = host.getTarget("c1:lightwave_demo:panel");
        assertNotNull(panel);
        // LayoutEngine still reports declared/content size; stage may grow width.
        assertTrue(panel.width() >= 100f);
        assertTrue(panel.height() >= 80f);
        assertEquals(1, host.effectsOf("c1:lightwave_demo:panel").size());
        assertEquals(LightwaveEffect.ID, host.effectsOf("c1:lightwave_demo:panel").get(0).effectId);
        assertTrue(
                LightwaveEffect.shouldDrawBorder(host.effectsOf("c1:lightwave_demo:panel").get(0)));
    }

    @Test
    public void probeDemoEffectsContract() {
        openDemo();
        Map<String, Object> render = ArtFramework.render().probeMap();
        assertTrue(render.containsKey("demoEffects"));
        @SuppressWarnings("unchecked")
        Map<String, Object> demo = (Map<String, Object>) render.get("demoEffects");
        @SuppressWarnings("unchecked")
        Map<String, Object> lw = (Map<String, Object>) demo.get("lightwave_demo");
        assertNotNull(lw);
        assertEquals(Boolean.TRUE, lw.get("bound"));
        assertEquals(Boolean.TRUE, lw.get("hasLightwave"));
        assertEquals(Boolean.TRUE, lw.get("borderDrawn"));
        assertTrue(((Number) lw.get("w")).floatValue() >= 100f);
        assertTrue(((Number) lw.get("h")).floatValue() >= 80f);
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) lw.get("effectIds");
        assertTrue(ids.contains(LightwaveEffect.ID));

        assertTrue(render.containsKey("targetsById"));
        @SuppressWarnings("unchecked")
        Map<String, Object> byId = (Map<String, Object>) render.get("targetsById");
        assertTrue(byId.containsKey("c1_lightwave_demo_panel"));
    }

    @Test
    public void treeThemeAndControls() {
        openDemo();
        UiTree tree = ArtFramework.tree("lightwave_demo");
        assertNotNull(tree);
        assertEquals("lightwave", tree.theme().name());
        assertNotNull(WidgetSessions.get("lightwave_demo"));
        assertTrue(WidgetSessions.get("lightwave_demo").buttonIds().contains("ok"));
        assertTrue(WidgetSessions.get("lightwave_demo").buttonIds().contains("close"));
        assertTrue(WidgetSessions.get("lightwave_demo").sliderIds().contains("wave"));

        Map<String, Object> snap = ArtFramework.probe().asMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> windows = (Map<String, Object>) snap.get("windows");
        @SuppressWarnings("unchecked")
        Map<String, Object> byId = (Map<String, Object>) windows.get("byId");
        @SuppressWarnings("unchecked")
        Map<String, Object> one = (Map<String, Object>) byId.get("lightwave_demo");
        @SuppressWarnings("unchecked")
        Map<String, Object> theme = (Map<String, Object>) one.get("theme");
        assertEquals("lightwave", theme.get("name"));
        @SuppressWarnings("unchecked")
        List<String> buttons = (List<String>) one.get("buttonIds");
        assertTrue(buttons.contains("ok"));
    }

    @Test
    public void tickAdvancesEnterFxIntensity() {
        openDemo();
        UiTree tree = ArtFramework.tree("lightwave_demo");
        float start = ((Number) tree.get("panel").prop("fx_intensity")).floatValue();
        assertTrue(start < 0.55f);
        ArtFramework.tick(0.4f);
        float end = ((Number) tree.get("panel").prop("fx_intensity")).floatValue();
        assertEquals(0.55f, end, 0.05f);
    }

    @Test
    public void pulseSweepsPhaseOnceWithoutChangingSpeed() {
        openDemo();
        ArtFramework.tick(0.4f);
        ArtFramework.ops().setSlider("lightwave_demo", "wave", 0.7f);
        float speedBefore =
                ArtFramework.render()
                        .effectsOf("c1:lightwave_demo:panel")
                        .get(0)
                        .paramFloat("speed", -1f);
        artframework.render.LightwaveControls.pulse("lightwave_demo");
        assertEquals(
                1f,
                ArtFramework.render()
                        .effectsOf("c1:lightwave_demo:panel")
                        .get(0)
                        .paramFloat("freeze", 0f),
                0.01f);
        float phase0 =
                ArtFramework.render()
                        .effectsOf("c1:lightwave_demo:panel")
                        .get(0)
                        .paramFloat("phase", -1f);
        assertEquals(0f, phase0, 0.05f);
        artframework.render.LightwaveControls.tickPulses(0.2f);
        float phaseMid =
                ArtFramework.render()
                        .effectsOf("c1:lightwave_demo:panel")
                        .get(0)
                        .paramFloat("phase", -1f);
        assertTrue(phaseMid > phase0);
        assertEquals(
                speedBefore,
                ArtFramework.render()
                        .effectsOf("c1:lightwave_demo:panel")
                        .get(0)
                        .paramFloat("speed", -1f),
                0.01f);
        artframework.render.LightwaveControls.tickPulses(0.4f);
        assertEquals(
                0f,
                ArtFramework.render()
                        .effectsOf("c1:lightwave_demo:panel")
                        .get(0)
                        .paramFloat("freeze", 1f),
                0.01f);
        assertEquals(
                0.7f,
                ArtFramework.render()
                        .effectsOf("c1:lightwave_demo:panel")
                        .get(0)
                        .paramFloat("intensity", -1f),
                0.08f);
    }

    @Test
    public void setSliderUpdatesSessionAndLightwaveIntensity() {
        openDemo();
        UiOpResult r = ArtFramework.ops().setSlider("lightwave_demo", "wave", 0.8f);
        assertEquals(UiOpResult.Status.OK, r.status);
        assertEquals(0.8f, WidgetSessions.get("lightwave_demo").getSlider("wave"), 0.001f);
        float intensity =
                ArtFramework.render()
                        .effectsOf("c1:lightwave_demo:panel")
                        .get(0)
                        .paramFloat("intensity", -1f);
        assertEquals(0.8f, intensity, 0.001f);
    }

    @Test
    public void chromeBorderTokensPresent() {
        openDemo();
        PresentResolved r = ArtFramework.tree("lightwave_demo").resolvePresent();
        assertTrue(r.chrome.borderWidth >= 1f);
        assertTrue(r.chrome.borderA > 0.5f);
        assertEquals("lightwave", r.profileId);
    }

    private static void openDemo() {
        ArtFramework.register(
                new WindowDef("lightwave_demo", WindowClass.SYNTHETIC, "layouts/lightwave_demo.json"));
        ArtFramework.open("lightwave_demo");
    }
}
