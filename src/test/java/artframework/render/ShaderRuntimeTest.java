package artframework.render;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ShaderRuntimeTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void glowSourcesExistOnClasspath() {
        ShaderRegistry.ShaderDef def = ArtFramework.render().shaders().get(GlowEffect.SHADER_ID);
        assertNotNull(def);
        assertNotNull(getClass().getClassLoader().getResource(def.vertexClasspath));
        assertNotNull(getClass().getClassLoader().getResource(def.fragmentClasspath));
    }

    @Test
    public void markCompiledForTestsUpdatesDefAndProbe() {
        RenderHost host = ArtFramework.render();
        ShaderRegistry.ShaderDef def = host.shaders().get(GlowEffect.SHADER_ID);
        assertFalse(def.isCompiled());
        host.shaderRuntime().markCompiledForTests(host.shaders(), GlowEffect.SHADER_ID);
        assertTrue(def.isCompiled());
        // no real GL program without compileAll
        assertFalse(host.shaderRuntime().has(GlowEffect.SHADER_ID));
    }

    @Test
    public void compileMissingResourceMarksFailedWithoutThrowing() {
        RenderHost host = ArtFramework.render();
        host.shaders().register("missing", "shaders/nope.vert", "shaders/nope.frag");
        // compileAll may throw inside ShaderProgram if GL missing — ShaderRuntime catches
        try {
            host.shaderRuntime().compileOne(host.shaders(), "missing");
        } catch (Throwable t) {
            throw new AssertionError("compileOne must not throw", t);
        }
        ShaderRegistry.ShaderDef def = host.shaders().get("missing");
        assertTrue(def.isCompileFailed() || !def.isCompiled());
        assertFalse(host.shaderRuntime().has("missing"));
    }

    @Test
    public void probeIncludesShaderStatus() {
        Map<String, Object> render = ArtFramework.render().probeMap();
        assertTrue(render.containsKey("shaders"));
        assertTrue(render.containsKey("shaderProgramCount"));
        assertTrue(render.containsKey("shadersReady"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) render.get("shaders");
        assertTrue(list.size() >= 1);
        boolean foundGlow = false;
        for (Map<String, Object> one : list) {
            if (GlowEffect.SHADER_ID.equals(one.get("id"))) {
                foundGlow = true;
                assertEquals(Boolean.FALSE, one.get("hasProgram"));
            }
        }
        assertTrue(foundGlow);
    }

    @Test
    public void glowEffectFallsBackWithoutProgram() {
        RenderHost host = ArtFramework.render();
        host.ensureTarget("g", RenderTargetKind.OVERLAY).setBounds(0, 0, 20, 20);
        host.bindEffect(
                "g",
                GlowEffect.ID,
                java.util.Collections.<String, Object>singletonMap("intensity", 0.7f));
        host.drawFrame(null);
        assertEquals(1, host.effectsOf("g").size());
    }
}
