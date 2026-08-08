package artframework.sts1.assets;

import artframework.assets.ResourceIds;
import artframework.component.ArtNodeTypes;
import artframework.component.UiNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Sts1NodeResourcesTest {

    @Test
    public void semanticNodesResolveStableResourceIds() {
        assertEquals(ResourceIds.UI_BUTTON_END_TURN,
                Sts1NodeResources.primaryResource(UiNode.of(ArtNodeTypes.STS_BUTTON)
                        .prop("variant", "end_turn").build()));
        assertEquals(ResourceIds.energyOrb("blue"),
                Sts1NodeResources.primaryResource(UiNode.of(ArtNodeTypes.STS_ENERGY_ORB)
                        .prop("color", "blue").build()));
        assertEquals(ResourceIds.MAP_NODE_ELITE,
                Sts1NodeResources.primaryResource(UiNode.of(ArtNodeTypes.STS_MAP_NODE)
                        .prop("kind", "elite").build()));
        assertEquals(ResourceIds.UI_REWARD_CARD,
                Sts1NodeResources.primaryResource(UiNode.of(ArtNodeTypes.STS_CARD).build()));
    }

    @Test
    public void explicitResourceIdIsTheOnlyOverrideEscapeHatch() {
        assertEquals("ui.button.modded",
                Sts1NodeResources.primaryResource(UiNode.of(ArtNodeTypes.STS_BUTTON)
                        .prop("resource_id", "ui.button.modded").build()));
    }
}
