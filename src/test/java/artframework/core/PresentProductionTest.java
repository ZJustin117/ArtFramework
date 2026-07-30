package artframework.core;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.assets.AssetPack;
import artframework.assets.AssetResolveResult;
import artframework.assets.HostAssets;
import artframework.assets.HostAssetsHolder;
import artframework.assets.ResourceIds;
import artframework.c1.FakeStageBackend;
import artframework.c1.SyntheticRuntime;
import artframework.component.UiNode;
import artframework.component.UiTypes;
import artframework.context.SurfaceIds;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Milestone 35: hot restyle, SurfacePresent, packId → HostAssets. */
public class PresentProductionTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void projectPresentRestylesOpenProjectFallbackTree() {
        FakeStageBackend stage = new FakeStageBackend();
        SyntheticRuntime.installStageBackend(stage);
        ArtFramework.register(
                new WindowDef("plain", WindowClass.SYNTHETIC, "layouts/demo.json"));
        // Mount without present_profile → from project
        UiNode root =
                UiNode.of(UiTypes.WINDOW)
                        .id("plain")
                        .prop("title", "P")
                        .child(UiNode.of(UiTypes.LABEL).id("hello").prop("text", "hi").build())
                        .build();
        UiTree tree = UiTrees.open("plain", root);
        assertEquals(PresentProfiles.STS, tree.resolvePresent().profileId);
        assertTrue(tree.resolvePresent().fromProject);
        assertEquals("sts", tree.theme().name());

        ProjectPresent.set(PresentProfiles.LIGHTWAVE);
        assertEquals(PresentProfiles.LIGHTWAVE, ProjectPresent.id());
        assertEquals("lightwave", tree.theme().name());
        assertEquals("lightwave", tree.resolvePresent().profileId);
        assertTrue(tree.resolvePresent().fromProject);
    }

    @Test
    public void projectPresentDoesNotOverrideNodeDeclaredProfile() {
        UiNode root =
                UiNode.of(UiTypes.WINDOW)
                        .id("w")
                        .prop("title", "T")
                        .prop("present_profile", "lightwave")
                        .child(UiNode.of(UiTypes.LABEL).id("hello").prop("text", "hi").build())
                        .build();
        UiTree tree = UiTrees.open("decl", root);
        assertEquals("lightwave", tree.theme().name());
        assertFalse(tree.resolvePresent().fromProject);

        ProjectPresent.set(PresentProfiles.STS);
        assertEquals(PresentProfiles.STS, ProjectPresent.id());
        // Node override still lightwave
        assertEquals("lightwave", tree.theme().name());
        assertEquals("lightwave", tree.resolvePresent().profileId);
        assertFalse(tree.resolvePresent().fromProject);
    }

    @Test
    public void reattachCalledForProjectFallbackOnRestyle() {
        FakeStageBackend stage = new FakeStageBackend();
        SyntheticRuntime.installStageBackend(stage);
        ArtFramework.register(
                new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        ArtFramework.open("demo");
        assertTrue(stage.isAttached("demo"));
        int before = stage.attachedCount();
        assertTrue(before >= 1);

        ArtFramework.setProjectPresent(PresentProfiles.LIGHTWAVE);
        // Still attached after reattach
        assertTrue(stage.isAttached("demo"));
        assertEquals("lightwave", ArtFramework.tree("demo").theme().name());
    }

    @Test
    public void surfacePresentOverridesProjectChrome() {
        assertTrue(PresentResolve.chromeForSurface(SurfaceIds.EVENT).cardAlpha >= 0.99f
                || PresentResolve.chromeForSurface(SurfaceIds.EVENT).cardAlpha == 1f);
        SurfacePresent.bind(SurfaceIds.EVENT, PresentProfiles.LIGHTWAVE);
        PresentResolved r = PresentResolve.forSurface(SurfaceIds.EVENT);
        assertEquals(PresentProfiles.LIGHTWAVE, r.profileId);
        assertFalse(r.fromProject);
        assertTrue(r.chrome.cardAlpha < 1f);
        assertTrue(r.chrome.borderWidth >= 1f);

        assertEquals(
                PresentProfiles.STS,
                PresentResolve.forSurface(SurfaceIds.COMBAT_HAND).profileId);

        SurfacePresent.unbind(SurfaceIds.EVENT);
        assertTrue(PresentResolve.forSurface(SurfaceIds.EVENT).fromProject);
    }

    @Test
    public void bindSurfacePresentApiAndProbe() {
        ArtFramework.bindSurfacePresent(SurfaceIds.COMBAT_HAND, PresentProfiles.LIGHTWAVE);
        assertEquals(
                PresentProfiles.LIGHTWAVE,
                ArtFramework.resolveSurfacePresent(SurfaceIds.COMBAT_HAND).profileId);
        assertTrue(ArtFramework.surfaceChrome(SurfaceIds.COMBAT_HAND).cardAlpha < 1f);

        Map<String, Object> snap = ArtFramework.probe().asMap();
        assertTrue(snap.containsKey("surfacePresent"));
        @SuppressWarnings("unchecked")
        Map<String, Object> sp = (Map<String, Object>) snap.get("surfacePresent");
        assertEquals(Integer.valueOf(1), sp.get("count"));
        @SuppressWarnings("unchecked")
        Map<String, String> bindings = (Map<String, String>) sp.get("bindings");
        assertEquals(PresentProfiles.LIGHTWAVE, bindings.get(SurfaceIds.COMBAT_HAND));

        ArtFramework.unbindSurfacePresent(SurfaceIds.COMBAT_HAND);
        assertTrue(ArtFramework.resolveSurfacePresent(SurfaceIds.COMBAT_HAND).fromProject);
    }

    @Test
    public void packIdPrefersHostAssetsPack() {
        HostAssets assets = HostAssetsHolder.get();
        assets.loadMinimalVanillaCatalog();
        assets.registerPack(
                AssetPack.builder("lw_pack")
                        .entry(ResourceIds.UI_PANEL_DEFAULT, "pack:lw_pack/panel.png")
                        .build());
        PresentProfile withPack =
                new PresentProfile(
                        "lw_pack_profile",
                        LightwaveTheme.createDefault(),
                        PresentChromeStyle.fromTheme(LightwaveTheme.createDefault()),
                        "lw_pack");
        PresentProfiles.register(withPack);
        ProjectPresent.set("lw_pack_profile");

        assertEquals("lw_pack", assets.presentPackId());
        AssetResolveResult panel = assets.resolve(ResourceIds.UI_PANEL_DEFAULT);
        assertTrue(panel.found);
        assertEquals("lw_pack", panel.packId);
        assertEquals("pack:lw_pack/panel.png", panel.source);

        Map<String, Object> cfg = assets.config().probeSummary();
        assertEquals("lw_pack", cfg.get("presentPackId"));
    }

    @Test
    public void thirdPartyPresentProfileRegisterSample() {
        Theme custom = new Theme();
        custom.setName("mod.coolwave");
        custom.setColor("Label", "font_color", 0.9f, 0.5f, 1f, 1f);
        custom.setColor("Panel", "border_color", 1f, 0.4f, 0.9f, 1f);
        custom.setConstant("Panel", "border_width", 4);
        custom.setConstant("Card", "alpha_pct", 80);
        PresentProfile third =
                new PresentProfile(
                        "mod.coolwave",
                        custom,
                        PresentChromeStyle.fromTheme(custom),
                        "mod.cool_pack");
        PresentProfiles.register(third);
        assertTrue(PresentProfiles.ids().contains("mod.coolwave"));

        SurfacePresent.bind(SurfaceIds.SHOP, "mod.coolwave");
        PresentResolved r = PresentResolve.forSurface(SurfaceIds.SHOP);
        assertEquals("mod.coolwave", r.profileId);
        assertEquals("mod.cool_pack", r.packId);
        assertEquals(4f, r.chrome.borderWidth, 0.01f);
        assertEquals(0.8f, r.chrome.cardAlpha, 0.01f);
    }

    @Test
    public void handDrawPathProbeUsesSurfacePresent() {
        SurfacePresent.bind(SurfaceIds.COMBAT_HAND, PresentProfiles.LIGHTWAVE);
        Map<String, Object> slice = artframework.sts1.render.HandDrawPath.probeSlice();
        assertEquals(PresentProfiles.LIGHTWAVE, slice.get("presentProfile"));
        assertEquals(Boolean.FALSE, slice.get("fromProject"));
        assertNotNull(slice.get("chrome"));
    }
}
