package artframework.core;

import artframework.component.UiNode;
import artframework.component.UiTypes;
import artframework.presentation.NodeTree;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ThemeTest {
    @Test public void namedThemeResolvesThroughNodeTree() {
        NodeTree tree = NodeTree.mount("tree:theme", UiNode.of(UiTypes.WINDOW).id("root")
                .prop("theme", "lightwave").build(), null);
        try { assertEquals("lightwave", tree.theme().name()); }
        finally { tree.close(); }
    }
}
