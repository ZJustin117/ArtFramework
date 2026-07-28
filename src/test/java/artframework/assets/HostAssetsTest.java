package artframework.assets;

import artframework.api.ArtFramework;
import artframework.core.Theme;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HostAssetsTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void minimalVanillaResolves() {
        HostAssets assets = ArtFramework.assets();
        AssetResolveResult r = assets.resolve(ResourceIds.UI_BUTTON_DEFAULT);
        assertTrue(r.found);
        assertEquals(HostAssets.VANILLA_PACK_ID, r.packId);
        assertTrue(r.source.startsWith("vanilla:"));
    }

    @Test
    public void packOverridesVanillaByPriority() {
        HostAssets assets = FakeHostAssets.create();
        assets.registerPack(
                AssetPack.builder("beauty")
                        .priority(10)
                        .domain(AssetDomain.CARD)
                        .entry(ResourceIds.cardArt("Strike_R"), "pack:strike")
                        .build());
        AssetResolveResult r = assets.resolve(ResourceIds.cardArt("Strike_R"));
        assertTrue(r.found);
        assertEquals("beauty", r.packId);
        assertEquals("pack:strike", r.source);
    }

    @Test
    public void packOrderLaterWins() {
        HostAssets assets = FakeHostAssets.create();
        assets.registerPack(
                AssetPack.builder("a")
                        .priority(100)
                        .domain(AssetDomain.UI)
                        .entry(ResourceIds.UI_PANEL_DEFAULT, "from-a")
                        .build());
        assets.registerPack(
                AssetPack.builder("b")
                        .priority(1)
                        .domain(AssetDomain.UI)
                        .entry(ResourceIds.UI_PANEL_DEFAULT, "from-b")
                        .build());
        assets.setPackOrder(Arrays.asList("a", "b"));
        AssetResolveResult r = assets.resolve(ResourceIds.UI_PANEL_DEFAULT);
        assertEquals("b", r.packId);
        assertEquals("from-b", r.source);
    }

    @Test
    public void domainDisabledFallsThrough() {
        HostAssets assets = FakeHostAssets.create();
        assets.registerPack(
                AssetPack.builder("mapskin")
                        .domain(AssetDomain.MAP)
                        .entry(ResourceIds.MAP_NODE_MONSTER, "custom-monster")
                        .build());
        assets.config().setDomainEnabled(AssetDomain.MAP, false);
        AssetResolveResult r = assets.resolve(ResourceIds.MAP_NODE_MONSTER);
        assertTrue(r.fallback || r.found);
        if (r.found) {
            assertEquals(HostAssets.VANILLA_PACK_ID, r.packId);
        }
    }

    @Test
    public void disablePackUsesVanilla() {
        HostAssets assets = FakeHostAssets.create();
        assets.registerPack(
                AssetPack.builder("x")
                        .domain(AssetDomain.CARD)
                        .entry(ResourceIds.CARD_FRAME_RED, "red-x")
                        .build());
        assets.enablePack("x", false);
        AssetResolveResult r = assets.resolve(ResourceIds.CARD_FRAME_RED);
        assertTrue(r.found);
        assertEquals(HostAssets.VANILLA_PACK_ID, r.packId);
    }

    @Test
    public void aliasAndStrictMissing() {
        HostAssets assets = FakeHostAssets.empty();
        assets.registerAlias("old.btn", ResourceIds.UI_BUTTON_DEFAULT);
        assets.loadMinimalVanillaCatalog();
        assertTrue(assets.resolve("old.btn").found);

        assets.config().setStrictMissing(true);
        AssetResolveResult miss = assets.resolve("no.such.key");
        assertFalse(miss.found);
        assertFalse(miss.fallback);
    }

    @Test
    public void probeListsPacks() {
        HostAssets assets = FakeHostAssets.create();
        assets.registerPack(
                AssetPack.builder("p1").domain(AssetDomain.FX).entry("fx.glow", "g").build());
        assertTrue(assets.probeAssets().containsKey("packs"));
        assertTrue(assets.packIds().contains("p1"));
    }

    @Test
    public void themeResolvesThroughHostAssets() {
        Theme t = new Theme();
        t.setIcon("Button", "icon", ResourceIds.UI_BUTTON_DEFAULT);
        String resolved = t.resolveIconAsset("Button", "icon");
        assertEquals(ResourceIds.UI_BUTTON_DEFAULT, resolved);
        AssetResolveResult style = t.resolveStyleAsset("Panel", "bg");
        assertTrue(style.found || style.fallback);
    }

    @Test
    public void resourceIdHelpers() {
        assertTrue(ResourceIds.isValid(ResourceIds.cardArt("Bash")));
        assertEquals("card.art.Bash", ResourceIds.cardArt("Bash"));
        assertEquals("map.node.elite", ResourceIds.mapNode("elite"));
        assertTrue(ResourceIds.minimalVanillaKeys().length > 5);
    }
}
