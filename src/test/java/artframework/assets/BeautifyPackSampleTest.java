package artframework.assets;

import artframework.api.ArtFramework;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Milestone 27: consumer-style beautify pack register / resolve / probe. */
public class BeautifyPackSampleTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void beautifyPackOverridesVanillaCardArt() {
        HostAssets assets = ArtFramework.assets();
        assets.loadMinimalVanillaCatalog();
        AssetPack pack =
                AssetPack.builder("beautify_sample")
                        .version("1")
                        .provider("artframework-sample")
                        .priority(50)
                        .domain(AssetDomain.CARD)
                        .domain(AssetDomain.UI)
                        .entry(ResourceIds.cardArt("Strike_R"), "pack:beautify_sample/Strike_R.png")
                        .entry(ResourceIds.UI_PANEL_DEFAULT, "pack:beautify_sample/panel.png")
                        .build();
        assets.registerPack(pack);
        assets.enablePack("beautify_sample", true);
        assets.setPackOrder(Collections.singletonList("beautify_sample"));

        AssetResolveResult card = assets.resolve(ResourceIds.cardArt("Strike_R"));
        assertTrue(card.found);
        assertEquals("beautify_sample", card.packId);
        assertEquals("pack:beautify_sample/Strike_R.png", card.source);

        AssetResolveResult panel = assets.resolve(ResourceIds.UI_PANEL_DEFAULT);
        assertTrue(panel.found);
        assertEquals("beautify_sample", panel.packId);

        Map<String, Object> probe = assets.probeAssets();
        assertTrue(probe.size() > 0);
    }
}
