package artframework.c1.layout;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LayoutLoaderTest {

    @Test
    public void parseMinimalWindow() {
        LayoutNode root = LayoutLoader.parse(
                "{\"type\":\"window\",\"title\":\"Demo\",\"width\":400,\"height\":240,"
                        + "\"children\":[{\"type\":\"label\",\"text\":\"Hello\"},"
                        + "{\"type\":\"button\",\"id\":\"close\",\"text\":\"Close\"}]}");
        assertEquals(LayoutNode.Type.WINDOW, root.type);
        assertEquals("Demo", root.title);
        assertEquals(400f, root.width, 0.001f);
        assertEquals(240f, root.height, 0.001f);
        assertEquals(2, root.children.size());
        assertEquals(LayoutNode.Type.LABEL, root.children.get(0).type);
        assertEquals("Hello", root.children.get(0).text);
        assertEquals(LayoutNode.Type.BUTTON, root.children.get(1).type);
        assertEquals("close", root.children.get(1).id);
        assertEquals("Close", root.children.get(1).text);
    }

    @Test
    public void loadClasspathDemo() {
        LayoutNode root = LayoutLoader.loadClasspath("layouts/demo.json");
        assertEquals(LayoutNode.Type.WINDOW, root.type);
        assertEquals("ArtFramework Demo", root.title);
        assertEquals("demo", root.id);
        assertTrue(root.children.size() >= 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyJsonThrows() {
        LayoutLoader.parse("  ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownTypeThrows() {
        LayoutLoader.parse("{\"type\":\"panel\"}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void windowWithoutTitleThrows() {
        LayoutLoader.parse("{\"type\":\"window\"}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingClasspathThrows() {
        LayoutLoader.loadClasspath("layouts/does-not-exist.json");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullResourceThrows() {
        LayoutLoader.loadClasspath(null);
    }
}
