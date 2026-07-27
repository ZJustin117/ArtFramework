package artframework.component;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.component.WidgetSession;
import artframework.component.WidgetSessions;
import artframework.render.RenderHost;
import artframework.render.TintEffect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ShaderEffectNodeTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void typeRegisteredAsVisualWithChildren() {
        UiNodeType t = UiNodeRegistry.global().get(ArtNodeTypes.SHADER_EFFECT);
        assertTrue(t != null);
        assertEquals(NodeKind.VISUAL, t.kind());
        assertTrue(t.allowsChildren());
    }

    @Test
    public void syncWidgetSessionBindsEffectProp() {
        UiNode root =
                UiNode.of(UiTypes.WINDOW)
                        .id("w")
                        .prop("title", "T")
                        .layout(new LayoutSpec(200f, 100f, 0f, 0f, false))
                        .child(
                                UiNode.of(ArtNodeTypes.SHADER_EFFECT)
                                        .id("fx")
                                        .prop("effect", TintEffect.ID)
                                        .prop("alpha", Float.valueOf(0.5f))
                                        .child(
                                                UiNode.of(UiTypes.LABEL)
                                                        .id("l")
                                                        .prop("text", "x")
                                                        .build())
                                        .build())
                        .build();
        WidgetSession session = WidgetSessions.openTree("win", root);
        RenderHost host = ArtFramework.render();
        host.syncWidgetSession(session);
        assertTrue(host.listTargetIds().contains("c1:win:fx"));
        assertEquals(1, host.effectsOf("c1:win:fx").size());
        assertEquals(TintEffect.ID, host.effectsOf("c1:win:fx").get(0).effectId);
    }

    @Test
    public void lmlParsesShaderEffectNode() {
        UiNode n =
                LmlUiNodeLoader.parse(
                        "<window id=\"w\" title=\"T\">"
                                + "<node type=\"art.shader_effect\" id=\"fx\" effect=\"tint\">"
                                + "<label id=\"l\" text=\"hi\"/>"
                                + "</node>"
                                + "</window>");
        assertEquals(ArtNodeTypes.SHADER_EFFECT, n.children.get(0).type);
        assertEquals("tint", n.children.get(0).propString("effect", ""));
    }
}
