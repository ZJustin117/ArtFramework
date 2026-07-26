package spireui.render;

import org.junit.After;
import org.junit.Test;
import spireui.api.SpireUI;
import spireui.api.WindowClass;
import spireui.api.WindowDef;
import spireui.component.EffectDecl;
import spireui.component.LayoutSpec;
import spireui.component.UiNode;
import spireui.component.UiTypes;
import spireui.component.WidgetSessions;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RenderHostTest {

    @After
    public void tearDown() {
        SpireUI.resetForTests();
    }

    @Test
    public void builtinsRegistered() {
        RenderHost host = SpireUI.render();
        assertTrue(host.effects().contains(TintEffect.ID));
        assertTrue(host.effects().contains(GlowEffect.ID));
        assertTrue(host.shaders().contains(GlowEffect.SHADER_ID));
        ShaderRegistry.ShaderDef glow = host.shaders().get(GlowEffect.SHADER_ID);
        assertEquals("shaders/glow.vert", glow.vertexClasspath);
        assertEquals("shaders/glow.frag", glow.fragmentClasspath);
    }

    @Test
    public void bindEffectAndProbe() {
        RenderHost host = SpireUI.render();
        host.ensureTarget("t1", RenderTargetKind.OVERLAY);
        host.bindEffect("t1", TintEffect.ID, Collections.<String, Object>singletonMap("alpha", 0.2f));
        assertEquals(1, host.bindingCount());
        Map<String, Object> probe = host.probeMap();
        assertEquals(Integer.valueOf(1), probe.get("targetCount"));
        assertEquals(Integer.valueOf(1), probe.get("bindingCount"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void fullFrameDisabledByDefault() {
        RenderHost host = SpireUI.render();
        host.ensureTarget("ff", RenderTargetKind.FULL_FRAME);
        host.bindEffect("ff", TintEffect.ID, null);
    }

    @Test
    public void syncWidgetSessionFromCompositionSample() {
        SpireUI.register(
                new WindowDef("comp", WindowClass.SYNTHETIC, "layouts/composition_sample.json"));
        SpireUI.open("comp");
        RenderHost host = SpireUI.render();
        assertTrue(host.targetCount() >= 1);
        assertNotNull(host.getTarget("c1:comp"));
        assertNotNull(host.getTarget("c1:comp:intensity"));
        List<EffectBinding> fx = host.effectsOf("c1:comp:intensity");
        assertEquals(1, fx.size());
        assertEquals(GlowEffect.ID, fx.get(0).effectId);

        Map<String, Object> snap = SpireUI.probe().asMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> render = (Map<String, Object>) snap.get("render");
        assertNotNull(render);
        assertTrue(((Number) render.get("targetCount")).intValue() >= 1);

        SpireUI.close("comp");
        assertNullTarget("c1:comp");
    }

    @Test
    public void entitySlotSyncAndDetach() {
        SpireUI.entities().attach("e1", "card", "Strike");
        SpireUI.entities().layout("e1", 100f, 200f, 1f);
        RenderTarget t = SpireUI.render().getTarget("c2:entity:e1");
        assertNotNull(t);
        assertEquals(RenderTargetKind.ENTITY_SLOT, t.kind);
        assertTrue(t.isEnabled());
        SpireUI.entities().detach("e1");
        assertNullTarget("c2:entity:e1");
    }

    @Test
    public void manualBindOnEntityTarget() {
        SpireUI.entities().attach("e2", "monster", "Hexaghost");
        SpireUI.entities().layout("e2", 10f, 20f, 1.5f);
        SpireUI.render().bindEffect(
                "c2:entity:e2",
                GlowEffect.ID,
                Collections.<String, Object>singletonMap("intensity", 0.8f));
        assertEquals(1, SpireUI.render().effectsOf("c2:entity:e2").size());
    }

    @Test
    public void drawFrameNoOpWithoutGl() {
        RenderHost host = SpireUI.render();
        host.ensureTarget("ov", RenderTargetKind.OVERLAY).setBounds(0, 0, 10, 10);
        host.bindEffect("ov", TintEffect.ID, null);
        host.tick(0.016f);
        host.drawFrame(null);
        assertTrue(host.timeSeconds() > 0f);
    }

    @Test
    public void openTreeSyncsEffectsFromNode() {
        UiNode root = UiNode.of(UiTypes.WINDOW)
                .id("w")
                .prop("title", "T")
                .layout(new LayoutSpec(200f, 100f, 0f, 0f, false))
                .effect(new EffectDecl(TintEffect.ID, null))
                .child(UiNode.of(UiTypes.BUTTON).id("b").prop("text", "X").build())
                .build();
        WidgetSessions.openTree("w", root);
        SpireUI.render().syncWidgetSession(WidgetSessions.get("w"));
        assertEquals(1, SpireUI.render().effectsOf("c1:w").size());
        WidgetSessions.close("w");
        SpireUI.render().detachWidgetSession("w");
    }

    private static void assertNullTarget(String id) {
        assertFalse(SpireUI.render().listTargetIds().contains(id));
    }
}
