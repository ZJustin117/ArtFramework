package spireui.render;

import org.junit.After;
import org.junit.Test;
import spireui.api.SpireUI;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FullFrameTest {

    @After
    public void tearDown() {
        SpireUI.resetForTests();
    }

    @Test
    public void disabledByDefaultBlocksBind() {
        RenderHost host = SpireUI.render();
        assertFalse(host.isFullFrameEnabled());
        host.ensureTarget(RenderHost.FULL_FRAME_ID, RenderTargetKind.FULL_FRAME);
        try {
            host.bindEffect(RenderHost.FULL_FRAME_ID, TintEffect.ID, null);
            throw new AssertionError("expected FULL_FRAME disabled");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("FULL_FRAME"));
        }
    }

    @Test
    public void enableFullFrameAllowsTintBind() {
        RenderHost host = SpireUI.render();
        RenderTarget t = host.enableFullFrame(1280f, 720f);
        assertNotNull(t);
        assertEquals(RenderTargetKind.FULL_FRAME, t.kind);
        assertEquals(1280f, t.width(), 0.001f);
        assertEquals(720f, t.height(), 0.001f);
        assertTrue(t.isEnabled());

        host.bindEffect(
                RenderHost.FULL_FRAME_ID,
                TintEffect.ID,
                Collections.<String, Object>singletonMap("alpha", 0.1f));
        assertEquals(1, host.effectsOf(RenderHost.FULL_FRAME_ID).size());

        Map<String, Object> probe = host.probeMap();
        assertEquals(Boolean.TRUE, probe.get("fullFrameEnabled"));
    }

    @Test
    public void bindFullFrameEffectEnablesAutomatically() {
        RenderHost host = SpireUI.render();
        assertFalse(host.isFullFrameEnabled());
        host.bindFullFrameEffect(
                GlowEffect.ID, Collections.<String, Object>singletonMap("intensity", 0.3f));
        assertTrue(host.isFullFrameEnabled());
        assertNotNull(host.fullFrameTarget());
        assertEquals(1, host.effectsOf(RenderHost.FULL_FRAME_ID).size());
    }

    @Test
    public void syncScreenBoundsUpdatesSize() {
        RenderHost host = SpireUI.render();
        host.enableFullFrame(100f, 100f);
        host.syncScreenBounds(1920f, 1080f);
        assertEquals(1920f, host.fullFrameTarget().width(), 0.001f);
        assertEquals(1080f, host.fullFrameTarget().height(), 0.001f);
    }

    @Test
    public void syncScreenBoundsNoOpWhenDisabled() {
        RenderHost host = SpireUI.render();
        assertNull(host.syncScreenBounds(800f, 600f));
        assertNull(host.fullFrameTarget());
    }

    @Test
    public void disableFullFrameRemovesTargetAndEffects() {
        RenderHost host = SpireUI.render();
        host.bindFullFrameEffect(TintEffect.ID, null);
        host.disableFullFrame();
        assertFalse(host.isFullFrameEnabled());
        assertNull(host.fullFrameTarget());
        assertEquals(0, host.effectsOf(RenderHost.FULL_FRAME_ID).size());
    }

    @Test
    public void drawFrameIncludesFullFrameWhenEnabled() {
        RenderHost host = SpireUI.render();
        host.enableFullFrame(64f, 64f);
        host.bindFullFrameEffect(
                TintEffect.ID, Collections.<String, Object>singletonMap("alpha", 0.05f));
        host.drawFrame(null);
        assertTrue(host.fullFrameTarget().isEnabled());
    }
}
