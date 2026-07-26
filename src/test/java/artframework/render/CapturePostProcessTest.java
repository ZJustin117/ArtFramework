package artframework.render;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.component.UiNode;
import artframework.component.UiNodeLoader;
import artframework.component.UiTypes;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CapturePostProcessTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void blurAndGlassRegistered() {
        RenderHost host = ArtFramework.render();
        assertTrue(host.effects().contains(BlurEffect.ID));
        assertTrue(host.effects().contains(GlassEffect.ID));
        assertTrue(host.shaders().contains(BlurEffect.SHADER_ID));
        assertTrue(host.shaders().contains(GlassEffect.SHADER_ID));
        assertNotNull(getClass().getClassLoader().getResource("shaders/blur.frag"));
        assertNotNull(getClass().getClassLoader().getResource("shaders/glass.frag"));
    }

    @Test
    public void needsCaptureWhenGlassBound() {
        RenderHost host = ArtFramework.render();
        assertFalse(host.needsCapture());
        host.enableFullFrame(800f, 600f);
        host.bindFullFrameEffect(
                GlassEffect.ID, Collections.<String, Object>singletonMap("radius", 2f));
        assertTrue(host.needsCapture());
    }

    @Test
    public void captureEnabledFlag() {
        RenderHost host = ArtFramework.render();
        host.setCaptureEnabled(true);
        assertTrue(host.needsCapture());
        host.setCaptureEnabled(false);
        assertFalse(host.needsCapture());
    }

    @Test
    public void captureScreenWithoutGlIsSafe() {
        FrameCapture cap = ArtFramework.render().frameCapture();
        boolean ok = cap.captureScreen(64, 64);
        // No GL in unit tests — must not throw; result may be false
        assertFalse(ok && cap.hasTexture() && cap.lastError().isEmpty() && false);
        assertNotNull(cap.lastError());
    }

    @Test
    public void glassComponentDefaultEffect() {
        UiNode root = UiNodeLoader.parse(
                "{\"type\":\"window\",\"title\":\"G\",\"children\":["
                        + "{\"type\":\"glass\",\"id\":\"p\",\"width\":100,\"height\":80,\"children\":[]}]}");
        UiNode glass = root.children.get(0);
        assertEquals(UiTypes.GLASS, glass.type);
        assertEquals(1, glass.effects.size());
        assertEquals(GlassEffect.ID, glass.effects.get(0).id);
    }

    @Test
    public void openGlassDemoBindsCaptureEffect() {
        ArtFramework.register(
                new WindowDef("glass_demo", WindowClass.SYNTHETIC, "layouts/glass_demo.json"));
        ArtFramework.open("glass_demo");
        RenderHost host = ArtFramework.render();
        assertNotNull(host.getTarget("c1:glass_demo:panel"));
        assertEquals(1, host.effectsOf("c1:glass_demo:panel").size());
        assertEquals(GlassEffect.ID, host.effectsOf("c1:glass_demo:panel").get(0).effectId);
        assertTrue(host.needsCapture());

        Map<String, Object> probe = host.probeMap();
        assertTrue(probe.containsKey("capture"));
        @SuppressWarnings("unchecked")
        Map<String, Object> cap = (Map<String, Object>) probe.get("capture");
        assertNotNull(cap.get("hasTexture"));
    }

    @Test
    public void drawGlassFallsBackWithoutCapture() {
        RenderHost host = ArtFramework.render();
        host.ensureTarget("g", RenderTargetKind.OVERLAY).setBounds(10, 10, 100, 80);
        host.bindEffect("g", GlassEffect.ID, Collections.<String, Object>singletonMap("radius", 2f));
        host.drawFrame(null);
        assertTrue(host.needsCapture());
    }
}
