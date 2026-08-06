package artframework.core;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.component.ComponentRegistry;
import artframework.component.TemplateExpander;
import artframework.component.UiNode;
import artframework.component.UiTypes;
import artframework.render.RenderHost;
import artframework.render.RenderHosts;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Milestone 38: PresentPack + EnabledPresents regex — no profile-id special cases. */
public class PresentPackTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void registerAndActivateLoadsTemplatesAndWindows() {
        PresentPack pack =
                PresentPack.builder("mod.ui_a")
                        .profileId("mod.skin_a")
                        .template("mod.ui_a.panel", "present-packs/lightwave/panel_chrome.json")
                        .window("mod_a_win", "layouts/demo.json")
                        .build();
        ArtFramework.registerPresentPack(pack);
        assertTrue(ArtFramework.presentPackIds().contains("mod.ui_a"));
        assertFalse(ComponentRegistry.global().contains("mod.ui_a.panel"));

        ArtFramework.activatePresentPack("mod.ui_a");
        assertEquals("mod.ui_a", ArtFramework.activePresentPack());
        assertTrue(ComponentRegistry.global().contains("mod.ui_a.panel"));
        assertTrue(ArtFramework.isRegistered("mod_a_win"));
        UiNode tmpl = ComponentRegistry.global().get("mod.ui_a.panel");
        assertNotNull(tmpl);
        assertEquals(UiTypes.PANEL, tmpl.type);
        assertFalse(tmpl.effects.isEmpty());
        assertEquals("lightwave", tmpl.effects.get(0).id);
    }

    @Test
    public void deactivateUnregistersTemplatesWhenConfigured() {
        PresentPack pack =
                PresentPack.builder("mod.ui_b")
                        .template("mod.ui_b.t", "present-packs/lightwave/panel_chrome.json")
                        .unregisterTemplatesOnDeactivate(true)
                        .build();
        PresentPacks.register(pack);
        PresentPacks.activate("mod.ui_b");
        assertTrue(ComponentRegistry.global().contains("mod.ui_b.t"));
        PresentPacks.deactivate("mod.ui_b");
        assertFalse(ComponentRegistry.global().contains("mod.ui_b.t"));
        assertEquals("", PresentPacks.activeId());
    }

    @Test
    public void projectPresentActivatesPackByPackId() {
        Theme t = LightwaveTheme.createDefault();
        t.setName("mod.linked");
        ArtFramework.registerPresentProfile("mod.linked", t, "mod.pack_linked");
        PresentPacks.register(
                PresentPack.builder("mod.pack_linked")
                        .profileId("mod.linked")
                        .template("mod.linked.chrome", "present-packs/lightwave/panel_chrome.json")
                        .window("mod_linked_demo", "layouts/demo.json")
                        .build());

        ArtFramework.setProjectPresent("mod.linked");
        assertEquals("mod.linked", ArtFramework.projectPresent());
        assertEquals("mod.pack_linked", ArtFramework.activePresentPack());
        assertTrue(ComponentRegistry.global().contains("mod.linked.chrome"));
        assertTrue(ArtFramework.isRegistered("mod_linked_demo"));

        ArtFramework.setProjectPresent(PresentProfiles.STS);
        assertEquals("", ArtFramework.activePresentPack());
        assertFalse(ComponentRegistry.global().contains("mod.linked.chrome"));
    }

    @Test
    public void projectPresentActivatesPackByProfileIdWhenPackIdEmpty() {
        Theme t = new Theme();
        t.setName("mod.by_profile");
        ArtFramework.registerPresentProfile("mod.by_profile", t, "");
        PresentPacks.register(
                PresentPack.builder("mod.pack_prof")
                        .profileId("mod.by_profile")
                        .template("mod.by_profile.t", "present-packs/lightwave/panel_chrome.json")
                        .build());
        ArtFramework.setProjectPresent("mod.by_profile");
        assertEquals("mod.pack_prof", ArtFramework.activePresentPack());
    }

    @Test
    public void builtinLightwavePackManifest() {
        PresentPacks.installBuiltinLightwavePack();
        assertTrue(PresentPacks.contains(PresentProfiles.LIGHTWAVE));
        PresentPack p = PresentPacks.get(PresentProfiles.LIGHTWAVE);
        assertEquals(PresentProfiles.LIGHTWAVE, p.profileId);
        assertFalse(p.templates.isEmpty());

        ArtFramework.setProjectPresent(PresentProfiles.LIGHTWAVE);
        assertEquals(PresentProfiles.LIGHTWAVE, ArtFramework.activePresentPack());
        assertTrue(ComponentRegistry.global().contains("lightwave.panel_chrome"));
        assertTrue(ArtFramework.isRegistered("lightwave_pack_demo"));
    }

    @Test
    public void refExpandUsesPackTemplate() {
        PresentPacks.register(
                PresentPack.builder("mod.refpack")
                        .template("mod.refpack.panel", "present-packs/lightwave/panel_chrome.json")
                        .build());
        PresentPacks.activate("mod.refpack");
        UiNode root =
                UiNode.of(UiTypes.WINDOW)
                        .id("w")
                        .prop("title", "T")
                        .child(
                                UiNode.of(UiTypes.REF)
                                        .ref("mod.refpack.panel")
                                        .slot(
                                                "body",
                                                Collections.singletonList(
                                                        UiNode.of(UiTypes.LABEL)
                                                                .id("hi")
                                                                .prop("text", "x")
                                                                .build()))
                                        .build())
                        .build();
        UiNode expanded = new TemplateExpander().expand(root);
        assertFalse(expanded.children.isEmpty());
        UiNode panel = expanded.children.get(0);
        assertEquals(UiTypes.PANEL, panel.type);
        assertFalse(panel.effects.isEmpty());
    }

    @Test
    public void enabledRestrictionBlocksSet() {
        ArtFramework.setEnabledPresentProfiles(Collections.singletonList(PresentProfiles.STS));
        try {
            ArtFramework.setProjectPresent(PresentProfiles.LIGHTWAVE);
            fail("expected not enabled");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("not enabled"));
        }
        ArtFramework.clearEnabledPresentRestriction();
        ArtFramework.setProjectPresent(PresentProfiles.LIGHTWAVE);
        assertEquals(PresentProfiles.LIGHTWAVE, ArtFramework.projectPresent());
    }

    @Test
    public void regexSelectAndModifyEnable() {
        Theme t = new Theme();
        t.setName("mod.alpha_one");
        ArtFramework.registerPresentProfile("mod.alpha_one", t);
        Theme t2 = new Theme();
        t2.setName("mod.alpha_two");
        ArtFramework.registerPresentProfile("mod.alpha_two", t2);
        Theme t3 = new Theme();
        t3.setName("mod.beta_x");
        ArtFramework.registerPresentProfile("mod.beta_x", t3);

        List<String> hits = ArtFramework.presentIdsMatching("^mod\\.alpha_");
        assertEquals(2, hits.size());
        assertTrue(hits.contains("mod.alpha_one"));

        ArtFramework.setEnabledPresentProfiles(
                Arrays.asList("mod.alpha_one", "mod.alpha_two", PresentProfiles.STS));
        String selected = ArtFramework.selectPresentMatching("^mod\\.alpha_");
        assertEquals("mod.alpha_one", selected);
        assertEquals("mod.alpha_one", ArtFramework.projectPresent());

        int n = ArtFramework.modifyPresentsMatching("^mod\\.alpha_", false);
        assertEquals(2, n);
        assertFalse(ArtFramework.enabledPresentIds().contains("mod.alpha_one"));
    }

    @Test
    public void regexModifyPackId() {
        Theme t = LightwaveTheme.createDefault();
        t.setName("mod.pack_target");
        ArtFramework.registerPresentProfile("mod.pack_target", t, "");
        PresentPacks.register(
                PresentPack.builder("mod.regex_pack")
                        .template("mod.regex_pack.t", "present-packs/lightwave/panel_chrome.json")
                        .build());
        int n =
                ArtFramework.modifyPresentPackIdMatching(
                        "^mod\\.pack_target$", "mod.regex_pack", true);
        assertEquals(1, n);
        assertEquals("mod.pack_target", ArtFramework.projectPresent());
        assertEquals("mod.regex_pack", ArtFramework.getPresentProfile("mod.pack_target").packId);
        assertEquals("mod.regex_pack", ArtFramework.activePresentPack());
    }

    @Test
    public void packIdsMatchingRegex() {
        PresentPacks.register(PresentPack.builder("skin.wave_1").build());
        PresentPacks.register(PresentPack.builder("skin.wave_2").build());
        PresentPacks.register(PresentPack.builder("other.x").build());
        List<String> m = ArtFramework.presentPackIdsMatching("^skin\\.wave_");
        assertEquals(2, m.size());
    }

    @Test
    public void manifestLoaderParsesClasspath() {
        PresentPack p = PresentPackLoader.loadClasspath("present-packs/lightwave/pack.json");
        assertEquals("lightwave", p.id);
        assertEquals("lightwave", p.profileId);
        assertEquals(1, p.templates.size());
        assertEquals("lightwave.panel_chrome", p.templates.get(0).name);
        assertEquals(2, p.windows.size());
    }

    @Test
    public void probeIncludesPacksAndEnabled() {
        PresentPacks.installBuiltinLightwavePack();
        Map<String, Object> snap = ArtFramework.probe().asMap();
        assertTrue(snap.containsKey("presentPacks"));
        assertTrue(snap.containsKey("enabledPresents"));
        @SuppressWarnings("unchecked")
        Map<String, Object> packs = (Map<String, Object>) snap.get("presentPacks");
        assertTrue(((Number) packs.get("count")).intValue() >= 1);
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) packs.get("ids");
        assertTrue(ids.contains(PresentProfiles.LIGHTWAVE));
    }

    @Test
    public void componentLevelDefaultsBindOnOpenDemo() {
        PresentPacks.installBuiltinLightwavePack();
        ArtFramework.setProjectPresent(PresentProfiles.LIGHTWAVE);
        ArtFramework.register(
                new WindowDef("demo_lw", WindowClass.SYNTHETIC, "layouts/demo.json"));
        ArtFramework.open("demo_lw");
        RenderHost host = RenderHosts.get();
        assertNotNull(host.getTarget("c1:demo_lw"));
        // window + hello + close + title all get pack lightwave when defaults present
        assertFalse(host.effectsOf("c1:demo_lw").isEmpty());
        assertEquals("lightwave", host.effectsOf("c1:demo_lw").get(0).effectId);
        assertNotNull(host.getTarget("c1:demo_lw:close"));
        assertEquals("lightwave", host.effectsOf("c1:demo_lw:close").get(0).effectId);
        assertNotNull(host.getTarget("c1:demo_lw:hello"));
        assertEquals("lightwave", host.effectsOf("c1:demo_lw:hello").get(0).effectId);
        assertNotNull(host.getTarget("c1:demo_lw:__art_title"));
        assertFalse(host.effectsOf("c1:demo_lw:__art_title").isEmpty());
        ArtFramework.close("demo_lw");
    }

    @Test
    public void packAmbientFullFrameAndEffectDefaultsAndSurfaces() {
        Map<String, Object> params = new java.util.LinkedHashMap<String, Object>();
        params.put("intensity", Float.valueOf(0.4f));
        PresentPack pack =
                PresentPack.builder("mod.ambient")
                        .profileId("mod.ambient_prof")
                        .effectDefault(
                                "panel",
                                new artframework.component.EffectDecl("lightwave", params))
                        .fullFrameEffect(
                                new artframework.component.EffectDecl("lightwave", params))
                        .bindSurface(artframework.context.SurfaceIds.EVENT)
                        .build();
        Theme t = LightwaveTheme.createDefault();
        t.setName("mod.ambient_prof");
        ArtFramework.registerPresentProfile("mod.ambient_prof", t, "mod.ambient");
        PresentPacks.register(pack);

        ArtFramework.setProjectPresent("mod.ambient_prof");
        assertEquals("mod.ambient", ArtFramework.activePresentPack());
        assertTrue(RenderHosts.get().isFullFrameEnabled());
        assertFalse(RenderHosts.get().effectsOf(RenderHost.FULL_FRAME_ID).isEmpty());
        assertEquals(
                "mod.ambient_prof",
                SurfacePresent.profileId(artframework.context.SurfaceIds.EVENT));
        assertFalse(PresentPackApply.effectDefaultsForType("panel").isEmpty());
        assertEquals("lightwave", PresentPackApply.effectDefaultsForType("panel").get(0).id);

        Map<String, Object> apply = PresentPackApply.probeSummary();
        assertEquals("mod.ambient", apply.get("activePack"));
        assertEquals(Boolean.TRUE, apply.get("managedFullFrame"));

        ArtFramework.setProjectPresent(PresentProfiles.STS);
        assertEquals("", ArtFramework.activePresentPack());
        assertFalse(RenderHosts.get().isFullFrameEnabled());
        assertNull(SurfacePresent.profileId(artframework.context.SurfaceIds.EVENT));
    }

    @Test
    public void switchPacksGenericModAModB() {
        PresentPacks.register(
                PresentPack.builder("pack.a")
                        .template("pack.a.t", "present-packs/lightwave/panel_chrome.json")
                        .build());
        PresentPacks.register(
                PresentPack.builder("pack.b")
                        .template("pack.b.t", "layouts/demo.json")
                        .build());
        Theme ta = new Theme();
        ta.setName("prof.a");
        Theme tb = new Theme();
        tb.setName("prof.b");
        ArtFramework.registerPresentProfile("prof.a", ta, "pack.a");
        ArtFramework.registerPresentProfile("prof.b", tb, "pack.b");

        ArtFramework.setProjectPresent("prof.a");
        assertTrue(ComponentRegistry.global().contains("pack.a.t"));
        ArtFramework.setProjectPresent("prof.b");
        assertFalse(ComponentRegistry.global().contains("pack.a.t"));
        assertTrue(ComponentRegistry.global().contains("pack.b.t"));
        assertEquals("pack.b", ArtFramework.activePresentPack());
    }
}
