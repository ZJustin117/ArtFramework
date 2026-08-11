package artframework.core;

import artframework.component.UiNode;
import artframework.component.UiTypes;
import artframework.presentation.NodeTree;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class PresentProfileTest {
    @Test public void mountUsesDeclaredPresentProfile() {
        NodeTree tree = NodeTree.mount("tree:profile", UiNode.of(UiTypes.WINDOW).id("root")
                .prop("present_profile", PresentProfiles.LIGHTWAVE).build(), null);
        try { assertEquals(PresentProfiles.LIGHTWAVE, tree.resolvePresent().profileId); }
        finally { tree.close(); }
    }

    @Test public void mountUsesDeclaredThemeName() {
        NodeTree tree = NodeTree.mount("tree:theme", UiNode.of(UiTypes.WINDOW).id("root")
                .prop("theme", "lightwave").build(), null);
        try { assertEquals("lightwave", tree.theme().name()); }
        finally { tree.close(); }
    }
}
