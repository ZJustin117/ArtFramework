package artframework.core;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Milestone 37: global PresentProfile catalog facade (skin register). */
public class PresentProfileCatalogTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void builtinsVisibleOnFacade() {
        List<String> ids = ArtFramework.presentProfileIds();
        assertTrue(ids.contains(PresentProfiles.STS));
        assertTrue(ids.contains(PresentProfiles.LIGHTWAVE));
        assertNotNull(ArtFramework.getPresentProfile(PresentProfiles.LIGHTWAVE));
        assertTrue(ArtFramework.presentProfiles().contains(PresentProfiles.STS));
    }

    @Test
    public void registerDoesNotApplyProjectPresent() {
        assertEquals(PresentProfiles.STS, ArtFramework.projectPresent());
        Theme t = new Theme();
        t.setName("mod.catalog_wave");
        t.setColor("Label", "font_color", 0.2f, 0.8f, 1f, 1f);
        t.setConstant("Card", "alpha_pct", 75);
        ArtFramework.registerPresentProfile("mod.catalog_wave", t, "mod.pack");

        assertEquals(PresentProfiles.STS, ArtFramework.projectPresent());
        assertTrue(ArtFramework.presentProfileIds().contains("mod.catalog_wave"));
        PresentProfile p = ArtFramework.getPresentProfile("mod.catalog_wave");
        assertNotNull(p);
        assertEquals("mod.pack", p.packId);
        assertEquals(0.75f, p.chrome.cardAlpha, 0.01f);
        assertNotNull(Themes.get("mod.catalog_wave"));
        assertEquals("mod.catalog_wave", Themes.get("mod.catalog_wave").name());
    }

    @Test
    public void registerThenSetProjectApplies() {
        Theme t = LightwaveTheme.createDefault();
        t.setName("mod.apply_me");
        ArtFramework.registerPresentProfile("mod.apply_me", t);
        ArtFramework.setProjectPresent("mod.apply_me");
        assertEquals("mod.apply_me", ArtFramework.projectPresent());
        assertEquals("mod.apply_me", Themes.getDefault().name());
        assertTrue(ArtFramework.presentChrome().cardAlpha < 1f);
    }

    @Test
    public void catalogRegisterSugarAndPresentProfileObject() {
        Theme t = new Theme();
        t.setName("mod.obj");
        t.setColor("Panel", "border_color", 1f, 0f, 1f, 1f);
        t.setConstant("Panel", "border_width", 5);
        PresentProfile built =
                new PresentProfile(
                        "mod.obj", t, PresentChromeStyle.fromTheme(t), "beautify_x");
        ArtFramework.presentProfiles().register(built);
        assertTrue(ArtFramework.presentProfiles().contains("mod.obj"));
        assertEquals(5f, ArtFramework.getPresentProfile("mod.obj").chrome.borderWidth, 0.01f);

        ArtFramework.presentProfiles().register("mod.sugar", t, "pack_y");
        assertEquals("pack_y", ArtFramework.getPresentProfile("mod.sugar").packId);
    }

    @Test
    public void registerSyncsThemeByProfileIdWhenThemeNameEmpty() {
        Theme t = new Theme();
        // name empty → putAndSyncTheme uses profile id
        t.setColor("Label", "font_color", 1f, 0f, 0f, 1f);
        ArtFramework.registerPresentProfile("mod.noname", t);
        assertNotNull(Themes.get("mod.noname"));
        assertEquals("mod.noname", Themes.get("mod.noname").name());
    }

    @Test
    public void probeIncludesPresentProfilesCatalog() {
        ArtFramework.registerPresentProfile(
                "mod.probe_skin", LightwaveTheme.createDefault(), "p1");
        Map<String, Object> snap = ArtFramework.probe().asMap();
        assertTrue(snap.containsKey("presentProfiles"));
        @SuppressWarnings("unchecked")
        Map<String, Object> cat = (Map<String, Object>) snap.get("presentProfiles");
        assertTrue(((Number) cat.get("count")).intValue() >= 3);
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) cat.get("ids");
        assertTrue(ids.contains("mod.probe_skin"));
        assertTrue(ids.contains(PresentProfiles.STS));
        @SuppressWarnings("unchecked")
        Map<String, Object> byId = (Map<String, Object>) cat.get("byId");
        assertNotNull(byId.get("mod.probe_skin"));
        @SuppressWarnings("unchecked")
        Map<String, Object> one = (Map<String, Object>) byId.get("mod.probe_skin");
        assertEquals("p1", one.get("packId"));

        @SuppressWarnings("unchecked")
        Map<String, Object> pp = (Map<String, Object>) snap.get("presentProfile");
        assertTrue(pp.containsKey("registeredIds"));
    }

    @Test
    public void resetForTestsDropsThirdPartyKeepsBuiltins() {
        ArtFramework.registerPresentProfile("mod.temp", LightwaveTheme.createDefault());
        assertTrue(ArtFramework.presentProfiles().contains("mod.temp"));
        ArtFramework.resetForTests();
        assertNull(ArtFramework.getPresentProfile("mod.temp"));
        assertTrue(ArtFramework.presentProfiles().contains(PresentProfiles.STS));
        assertTrue(ArtFramework.presentProfiles().contains(PresentProfiles.LIGHTWAVE));
        assertEquals(PresentProfiles.STS, ArtFramework.projectPresent());
    }

    @Test
    public void surfaceBindUsesRegisteredCatalogProfile() {
        Theme t = new Theme();
        t.setName("mod.surface_skin");
        t.setConstant("Card", "alpha_pct", 70);
        ArtFramework.registerPresentProfile("mod.surface_skin", t);
        ArtFramework.bindSurfacePresent(SurfaceIds.EVENT, "mod.surface_skin");
        PresentResolved r = ArtFramework.resolveSurfacePresent(SurfaceIds.EVENT);
        assertEquals("mod.surface_skin", r.profileId);
        assertEquals(0.7f, r.chrome.cardAlpha, 0.01f);
        assertFalse(r.fromProject);
    }
}
