package artframework.core;

import artframework.component.UiNode;
import artframework.component.UiTypes;
import artframework.presentation.NodeTree;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PresentProductionTest {
    @Test public void projectPresentResolvesForTreeAndRefreshes() {
        UiNode root = UiNode.of(UiTypes.WINDOW).id("plain").build();
        NodeTree tree = NodeTree.mount("tree:plain", root, null);
        try {
            assertTrue(tree.resolvePresent().fromProject);
            ProjectPresent.set(PresentProfiles.LIGHTWAVE);
            tree.refreshPresent();
            assertEquals(PresentProfiles.LIGHTWAVE, tree.resolvePresent().profileId);
        } finally { tree.close(); ProjectPresent.resetForTests(); }
    }

    @Test public void declaredProfileBeatsProjectPresent() {
        UiNode root = UiNode.of(UiTypes.WINDOW).id("decl")
                .prop("present_profile", PresentProfiles.LIGHTWAVE).build();
        NodeTree tree = NodeTree.mount("tree:decl", root, null);
        try { assertEquals(PresentProfiles.LIGHTWAVE, tree.resolvePresent().profileId); }
        finally { tree.close(); }
    }
}
