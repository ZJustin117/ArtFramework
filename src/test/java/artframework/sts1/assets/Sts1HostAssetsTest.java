package artframework.sts1.assets;

import artframework.api.ArtFramework;
import artframework.assets.AssetResolveResult;
import artframework.assets.HostAssets;
import artframework.assets.ResourceIds;
import org.junit.After;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Sts1HostAssetsTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1HostAssets.resetForTests();
    }

    @Test
    public void catalogCoversFramesMapUi() {
        Map<String, String> cat = Sts1VanillaCatalog.catalog();
        assertTrue(cat.containsKey(ResourceIds.CARD_FRAME_RED));
        assertEquals("sts1:cardui/frame", cat.get(ResourceIds.CARD_FRAME_RED));
        assertTrue(cat.containsKey(ResourceIds.MAP_NODE_ELITE));
        assertTrue(cat.containsKey(ResourceIds.UI_BUTTON_DEFAULT));
        assertTrue(cat.get(ResourceIds.MAP_NODE_MONSTER).startsWith(Sts1VanillaCatalog.SOURCE_PREFIX));
        assertTrue(cat.get(ResourceIds.cardArt("Strike_R")).contains("Strike_R"));
    }

    @Test
    public void installReplacesMinimalVanillaSources() {
        HostAssets assets = ArtFramework.assets();
        AssetResolveResult before = assets.resolve(ResourceIds.UI_BUTTON_DEFAULT);
        assertTrue(before.found);
        assertTrue(before.source.startsWith("vanilla:"));

        Sts1HostAssets.install();
        AssetResolveResult after = assets.resolve(ResourceIds.UI_BUTTON_DEFAULT);
        assertTrue(after.found);
        assertEquals(HostAssets.VANILLA_PACK_ID, after.packId);
        assertTrue(after.source.startsWith(Sts1VanillaCatalog.SOURCE_PREFIX));
        assertTrue(after.source.contains("images/"));
    }

    @Test
    public void mapAndCardArtResolveAfterInstall() {
        Sts1HostAssets.install();
        AssetResolveResult map = Sts1HostAssets.resolve(ResourceIds.MAP_NODE_BOSS);
        assertTrue(map.found);
        assertTrue(map.source.contains("boss"));

        AssetResolveResult art = Sts1HostAssets.resolve(ResourceIds.cardArt("Defend_R"));
        assertTrue(art.found);
        assertTrue(art.source.contains("Defend_R"));
    }

    @Test
    public void unknownCardArtStillHitsViaDynamicHelper() {
        Sts1HostAssets.install();
        AssetResolveResult r = Sts1HostAssets.resolveCardArt("CustomModCard_99");
        assertTrue(r.found);
        assertEquals(HostAssets.VANILLA_PACK_ID, r.packId);
        assertTrue(r.source.startsWith(Sts1VanillaCatalog.SOURCE_PREFIX));
    }

    @Test
    public void packStillOverridesSts1Vanilla() {
        Sts1HostAssets.install();
        HostAssets assets = ArtFramework.assets();
        assets.registerPack(
                artframework.assets.AssetPack.builder("beauty")
                        .priority(50)
                        .domain(artframework.assets.AssetDomain.CARD)
                        .entry(ResourceIds.cardArt("Strike_R"), "pack:pretty-strike")
                        .build());
        AssetResolveResult r = assets.resolve(ResourceIds.cardArt("Strike_R"));
        assertEquals("beauty", r.packId);
        assertEquals("pack:pretty-strike", r.source);
    }

    @Test
    public void installIdempotent() {
        Sts1HostAssets.install();
        Sts1HostAssets.install();
        assertTrue(Sts1HostAssets.isInstalled());
        assertFalse(ArtFramework.assets().resolve(ResourceIds.MAP_NODE_REST).source.startsWith("vanilla:"));
    }
}
