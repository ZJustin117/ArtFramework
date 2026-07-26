package artframework.component;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UiNodeLoaderTest {

    @Test
    public void parseLegacyWindowShape() {
        UiNode root = UiNodeLoader.parse(
                "{\"type\":\"window\",\"title\":\"Demo\",\"width\":400,\"height\":240,"
                        + "\"children\":[{\"type\":\"label\",\"text\":\"Hello\"},"
                        + "{\"type\":\"button\",\"id\":\"close\",\"text\":\"Close\"}]}");
        assertEquals(UiTypes.WINDOW, root.type);
        assertEquals("Demo", root.propString("title", ""));
        assertEquals(400f, root.layout.width, 0.001f);
        assertEquals(2, root.children.size());
        assertEquals(UiTypes.LABEL, root.children.get(0).type);
        assertEquals("Hello", root.children.get(0).propString("text", ""));
        assertEquals("close", root.children.get(1).id);
    }

    @Test
    public void loadCompositionSample() {
        UiNode root = UiNodeLoader.loadClasspath("layouts/composition_sample.json");
        assertEquals("comp_sample", root.id);
        assertEquals(UiTypes.WINDOW, root.type);
        NodeIndex idx = NodeIndex.of(root);
        assertTrue(idx.contains("ok"));
        assertTrue(idx.contains("intensity"));
        assertTrue(idx.contains("card_zone"));
        assertEquals(UiTypes.SLIDER, idx.get("intensity").type);
        assertEquals(1, idx.get("intensity").effects.size());
        assertEquals("glow", idx.get("intensity").effects.get(0).id);
    }

    @Test
    public void loadClasspathDemoCompatible() {
        UiNode root = UiNodeLoader.loadClasspath("layouts/demo.json");
        assertEquals(UiTypes.WINDOW, root.type);
        assertEquals("ArtFramework Demo", root.propString("title", ""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownTypeThrows() {
        UiNodeLoader.parse("{\"type\":\"webview\"}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void windowWithoutTitleThrows() {
        UiNodeLoader.parse("{\"type\":\"window\"}");
    }

    @Test
    public void effectsDoNotRequireLayoutFields() {
        UiNode n = UiNodeLoader.parse(
                "{\"type\":\"button\",\"id\":\"b\",\"text\":\"X\","
                        + "\"effects\":[{\"id\":\"tint\",\"params\":{\"a\":1}}]}");
        assertEquals(1, n.effects.size());
        assertFalse(n.layout.grow);
    }
}
