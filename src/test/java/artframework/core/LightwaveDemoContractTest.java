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
        assertEquals(2, host.effectsOf("c1:lightwave_demo:panel").size());
        assertEquals(LightwaveEffect.ID, host.effectsOf("c1:lightwave_demo:panel").get(0).effectId);
        assertEquals(LightwaveEffect.ID, host.effectsOf("c1:lightwave_demo:panel").get(1).effectId);
        assertTrue(
                LightwaveEffect.shouldDrawBorder(
                        host.findEffect(
                                "c1:lightwave_demo:panel",
                                LightwaveEffect.ID,
                                artframework.render.EffectBinding.LAYER_AMBIENT)));
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
        assertTrue(WidgetSessions.get("lightwave_demo").sliderIds().contains("wave_slider"));

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
        // After enter, wave player loops ripple.
        AnimationPlayer wave = AnimationPlayers.get("lightwave_demo", "wave");
        assertNotNull(wave);
        assertTrue(wave.isPlaying());
        assertEquals("ripple", wave.playing());
    }

    @Test
    public void pulseAddsSecondBandWithoutStoppingWave() {
        openDemo();
        ArtFramework.tick(0.5f);
        ArtFramework.ops().setSlider("lightwave_demo", "wave_slider", 0.7f);
        AnimationPlayer wave = AnimationPlayers.get("lightwave_demo", "wave");
        AnimationPlayer pulse = AnimationPlayers.get("lightwave_demo", "pulse");
        assertNotNull(wave);
        assertNotNull(pulse);
        assertTrue(wave.isPlaying());
        assertEquals("ripple", wave.playing());
        assertEquals(
                2,
                ArtFramework.render().effectsOf("c1:lightwave_demo:panel").size());
        artframework.render.EffectBinding ambient =
                ArtFramework.render()
                        .findEffect(
                                "c1:lightwave_demo:panel",
                                LightwaveEffect.ID,
                                artframework.render.EffectBinding.LAYER_AMBIENT);
        artframework.render.EffectBinding pulseBind =
                ArtFramework.render()
                        .findEffect(
                                "c1:lightwave_demo:panel",
                                LightwaveEffect.ID,
                                artframework.render.EffectBinding.LAYER_PULSE);
        assertNotNull(ambient);
        assertNotNull(pulseBind);
        float ambientPhase0 = ambient.paramFloat("phase", -1f);
        // PULSE → overlay band only; ambient wave keeps looping
        ArtFramework.ops().clickButton("lightwave_demo", "ok");
        assertTrue(pulse.isPlaying());
        assertEquals("flash", pulse.playing());
        assertTrue(wave.isPlaying());
        assertEquals("ripple", wave.playing());
        assertTrue(pulseBind.isEnabled());
        float pulsePhase0 = pulseBind.paramFloat("phase", -1f);
        ArtFramework.tick(0.15f);
        float pulsePhaseMid = pulseBind.paramFloat("phase", -1f);
        assertTrue(pulsePhaseMid > pulsePhase0);
        // Ambient still advancing independently
        ArtFramework.tick(0.1f);
        float ambientPhase1 = ambient.paramFloat("phase", -1f);
        assertTrue(
                ambientPhase1 != ambientPhase0
                        || ambient.paramFloat("intensity", 0f) > 0.5f);
        // Finish flash + fade
        ArtFramework.tick(0.3f);
        if (pulse.isPlaying()) {
            ArtFramework.tick(0.2f);
        }
        assertTrue(!pulse.isPlaying());
        assertTrue(wave.isPlaying());
        assertEquals("ripple", wave.playing());
        assertEquals(
                0.7f,
                ambient.paramFloat("intensity", -1f),
                0.12f);
        assertTrue(pulseBind.paramFloat("intensity", 1f) < 0.05f || !pulseBind.isEnabled());
    }

    @Test
    public void setSliderUpdatesSessionAndLightwaveIntensity() {
        openDemo();
        ArtFramework.tick(0.5f);
        UiOpResult r = ArtFramework.ops().setSlider("lightwave_demo", "wave_slider", 0.8f);
        assertEquals(UiOpResult.Status.OK, r.status);
        assertEquals(0.8f, WidgetSessions.get("lightwave_demo").getSlider("wave_slider"), 0.001f);
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
