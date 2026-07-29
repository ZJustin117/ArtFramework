package artframework.sts1.assets;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Sts1AssetMaterializerTest {

    @Test
    public void classifiesVanillaFileAndLogicalCardSourcesWithoutGl() {
        assertEquals("images/512/frame_attack_red.png",
                Sts1AssetMaterializer.normalize("sts1:images/512/frame_attack_red.png"));
        assertTrue(Sts1AssetMaterializer.isFileBacked("sts1:images/512/frame_attack_red.png"));
        assertTrue(Sts1AssetMaterializer.isLogicalCardArt("sts1:card/art/Strike_R"));
        assertFalse(Sts1AssetMaterializer.isFileBacked("sts1:card/art/Strike_R"));
        assertTrue(Sts1AssetMaterializer.isFileBacked("sts1:images/ui/topPanel/endTurnButton.png"));
        assertTrue(Sts1AssetMaterializer.isFileBacked("sts1:images/ui/map/monster.png"));
        assertFalse(Sts1AssetMaterializer.isFileBacked("pack://custom/node.png"));
        assertFalse(Sts1AssetMaterializer.isFileBacked(""));
    }
}
