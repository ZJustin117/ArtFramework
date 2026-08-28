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
        assertTrue(cat.containsKey(ResourceIds.UI_BUTTON_PROCEED_ENABLED));
        assertTrue(cat.containsKey(ResourceIds.UI_BUTTON_PROCEED_DISABLED));
        assertTrue(cat.containsKey(ResourceIds.UI_TOP_PANEL_BAR));
        assertTrue(cat.containsKey(ResourceIds.UI_TOP_PANEL_HP));
        assertTrue(cat.containsKey(ResourceIds.UI_TOP_PANEL_GOLD));
        assertTrue(cat.containsKey(ResourceIds.UI_TOP_PANEL_FLOOR));
        assertTrue(cat.containsKey(ResourceIds.UI_REWARD_GOLD));
        assertTrue(cat.containsKey(ResourceIds.UI_REWARD_CARD));
        assertTrue(cat.containsKey(ResourceIds.UI_REWARD_RELIC));
        assertTrue(cat.containsKey(ResourceIds.UI_REWARD_BOSS_RELIC));
        assertTrue(cat.containsKey(ResourceIds.UI_REWARD_DISABLED));
        assertTrue(cat.containsKey(ResourceIds.UI_CAMPFIRE_PANEL));
        assertTrue(cat.containsKey(ResourceIds.UI_CAMPFIRE_REST_OPTION));
        assertTrue(cat.containsKey(ResourceIds.UI_CAMPFIRE_SMITH_OPTION));
        assertTrue(cat.containsKey(ResourceIds.UI_CAMPFIRE_DIG_OPTION));
        assertTrue(cat.containsKey(ResourceIds.UI_CAMPFIRE_RECALL_OPTION));
        assertTrue(cat.containsKey(ResourceIds.UI_CAMPFIRE_TOKE_OPTION));
        assertTrue(cat.containsKey(ResourceIds.UI_CAMPFIRE_OTHER_OPTION));
        assertTrue(cat.containsKey(ResourceIds.UI_CAMPFIRE_DISABLED_OPTION));
        assertTrue(cat.containsKey(ResourceIds.UI_CAMPFIRE_OUTLINE));
        assertTrue(cat.containsKey(ResourceIds.UI_CAMPFIRE_SLEEP));
        assertTrue(cat.containsKey(ResourceIds.UI_CAMPFIRE_SMITH));
        assertTrue(cat.containsKey(ResourceIds.UI_EVENT_PANEL));
        assertTrue(cat.containsKey(ResourceIds.UI_EVENT_TITLE));
        assertTrue(cat.containsKey(ResourceIds.UI_EVENT_BUTTON_ENABLED));
        assertTrue(cat.containsKey(ResourceIds.UI_EVENT_BUTTON_DISABLED));
        assertTrue(cat.containsKey(ResourceIds.UI_SELECT_CARD));
        assertTrue(cat.containsKey(ResourceIds.UI_SELECT_CARD_DISABLED));
        assertTrue(cat.containsKey(ResourceIds.UI_SELECT_CARD_SELECTED));
        assertTrue(cat.containsKey(ResourceIds.UI_SELECT_CARD_FRAME));
        assertTrue(cat.containsKey(ResourceIds.UI_SELECT_CONFIRM));
        assertTrue(cat.containsKey(ResourceIds.UI_SELECT_CONFIRM_DISABLED));
        assertTrue(cat.containsKey(ResourceIds.UI_SHOP_MERCHANT));
        assertTrue(cat.containsKey(ResourceIds.UI_SHOP_GOLD));
        assertTrue(cat.containsKey(ResourceIds.UI_SHOP_ENTRY_PANEL));
        assertTrue(cat.containsKey(ResourceIds.UI_SHOP_PURGE));
        assertTrue(cat.containsKey(ResourceIds.UI_SHOP_SOLD_OUT));
        assertTrue(cat.containsKey(ResourceIds.UI_TREASURE_PANEL));
        assertTrue(cat.containsKey(ResourceIds.UI_TREASURE_CHEST_CLOSED));
        assertTrue(cat.containsKey(ResourceIds.UI_TREASURE_CHEST_OPEN));
        assertTrue(cat.containsKey(ResourceIds.UI_TREASURE_RELIC));
        assertEquals(
                "sts1:images/ui/intent/attack/attack_intent_1.png",
                cat.get(ResourceIds.intent("attack/attack_intent_1")));
        assertTrue(cat.get(ResourceIds.MAP_NODE_MONSTER).startsWith(Sts1VanillaCatalog.SOURCE_PREFIX));
        assertTrue(cat.get(ResourceIds.cardArt("Strike_R")).contains("Strike_R"));
        assertTrue(cat.get(ResourceIds.UI_CAMPFIRE_PANEL).startsWith(Sts1VanillaCatalog.SOURCE_PREFIX));
        assertTrue(cat.get(ResourceIds.UI_CAMPFIRE_REST_OPTION).contains("campfire"));
        assertTrue(cat.get(ResourceIds.UI_CAMPFIRE_SMITH_OPTION).contains("campfire"));
        assertTrue(cat.get(ResourceIds.UI_CAMPFIRE_DIG_OPTION).contains("dig"));
        assertTrue(cat.get(ResourceIds.UI_CAMPFIRE_RECALL_OPTION).contains("recall"));
        assertTrue(cat.get(ResourceIds.UI_CAMPFIRE_TOKE_OPTION).contains("toke"));
        assertTrue(cat.get(ResourceIds.UI_CAMPFIRE_OTHER_OPTION).contains("outline"));
        assertTrue(cat.get(ResourceIds.UI_CAMPFIRE_DISABLED_OPTION).contains("buttonShadow"));
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
