package artframework.render;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.component.UiNode;
import artframework.component.UiNodeLoader;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LightwaveEffectTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void registeredAsBuiltin() {
        RenderHost host = new RenderHost();
        assertTrue(host.effects().contains(LightwaveEffect.ID));
        assertTrue(host.shaders().contains(LightwaveEffect.SHADER_ID));
        ShaderRegistry.ShaderDef def = host.shaders().get(LightwaveEffect.SHADER_ID);
        assertEquals("shaders/lightwave.vert", def.vertexClasspath);
        assertEquals("shaders/lightwave.frag", def.fragmentClasspath);
    }

    @Test
    public void sourcesExistOnClasspath() {
        assertNotNull(getClass().getClassLoader().getResource("shaders/lightwave.vert"));
        assertNotNull(getClass().getClassLoader().getResource("shaders/lightwave.frag"));
    }

    @Test
    public void validateAcceptsDefaults() {
        LightwaveEffect fx = new LightwaveEffect();
        Map<String, Object> p = new LinkedHashMap<String, Object>();
        p.put("intensity", Float.valueOf(0.5f));
        p.put("width", Float.valueOf(0.2f));
        fx.validate(new EffectBinding(LightwaveEffect.ID, p));
    }

    @Test
    public void validateRejectsBadIntensity() {
        LightwaveEffect fx = new LightwaveEffect();
        Map<String, Object> p = new LinkedHashMap<String, Object>();
        p.put("intensity", Float.valueOf(3f));
        try {
            fx.validate(new EffectBinding(LightwaveEffect.ID, p));
            fail("expected");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("intensity"));
        }
    }

    @Test
    public void fallbackDrawNoopsWithoutBatch() {
        LightwaveEffect fx = new LightwaveEffect();
        RenderTarget t = new RenderTarget("t", RenderTargetKind.SYNTHETIC_WIDGET);
        t.setBounds(0, 0, 100, 80);
        Map<String, Object> p = new LinkedHashMap<String, Object>();
        p.put("intensity", Float.valueOf(0.6f));
        EffectBinding b = new EffectBinding(LightwaveEffect.ID, p);
        RenderContext ctx = new RenderContext(null, 1.2f, null);
        fx.draw(t, b, ctx);
    }

    @Test
    public void demoBindsLightwaveEffect() {
        ArtFramework.register(
                new WindowDef("lightwave_demo", WindowClass.SYNTHETIC, "layouts/lightwave_demo.json"));
        ArtFramework.open("lightwave_demo");
        RenderHost host = ArtFramework.render();
        assertNotNull(host.getTarget("c1:lightwave_demo:panel"));
        assertEquals(1, host.effectsOf("c1:lightwave_demo:panel").size());
        assertEquals(LightwaveEffect.ID, host.effectsOf("c1:lightwave_demo:panel").get(0).effectId);
    }

    @Test
    public void layoutParsesLightwaveEffect() {
        UiNode root = UiNodeLoader.loadClasspath("layouts/lightwave_demo.json");
        UiNode panel = null;
        for (UiNode c : root.children) {
            if ("panel".equals(c.id)) {
                panel = c;
                break;
            }
        }
        assertNotNull(panel);
        assertEquals(1, panel.effects.size());
        assertEquals("lightwave", panel.effects.get(0).id);
    }
}
