package artframework.core;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.component.ArtNodeTypes;
import artframework.component.UiNode;
import artframework.component.UiNodeLoader;
import artframework.component.UiTypes;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PresentProfileTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void builtinsRegistered() {
        assertNotNull(PresentProfiles.get(PresentProfiles.STS));
        assertNotNull(PresentProfiles.get(PresentProfiles.LIGHTWAVE));
        assertTrue(PresentProfiles.ids().contains(PresentProfiles.STS));
        assertTrue(PresentProfiles.ids().contains(PresentProfiles.LIGHTWAVE));
    }

    @Test
    public void projectPresentSwitchesFallbackThemeAndChrome() {
        ProjectPresent.set(PresentProfiles.LIGHTWAVE);
        assertEquals(PresentProfiles.LIGHTWAVE, ProjectPresent.id());
        assertEquals(PresentProfiles.LIGHTWAVE, ArtFramework.projectPresent());
        assertEquals("lightwave", Themes.getDefault().name());
        assertTrue(ProjectPresent.chrome().cardAlpha < 1f);
        assertTrue(ProjectPresent.chrome().borderWidth >= 1f);

        ProjectPresent.set(PresentProfiles.STS);
        assertEquals(PresentProfiles.STS, ArtFramework.presentProfile());
        assertEquals(1f, ProjectPresent.chrome().cardAlpha, 0.001f);
    }

    @Test
    public void lightwaveThemeHasBorderAndAlphaTokens() {
        Theme t = LightwaveTheme.createDefault();
        assertNotNull(t.getColor("Panel", "border_color"));
        assertNotNull(t.getColor("Panel", "bg"));
        assertTrue(t.getColor("Panel", "bg").a < 0.9f);
        assertTrue(t.getConstant("Panel", "border_width") >= 1);
        assertTrue(t.getConstant("Card", "alpha_pct") > 0);
        assertNotNull(t.getColor("Lightwave", "band"));
    }

    @Test
    public void chromeFromLightwaveTheme() {
        PresentChromeStyle c = PresentChromeStyle.fromTheme(LightwaveTheme.createDefault());
        assertTrue(c.cardAlpha > 0.5f && c.cardAlpha < 1f);
        assertTrue(c.borderWidth >= 1f);
        assertTrue(c.borderA > 0.5f);
        Map<String, Object> probe = c.probeSummary();
        assertTrue(probe.containsKey("cardAlpha"));
        assertTrue(probe.containsKey("border"));
    }

    @Test
    public void probeIncludesProjectPresent() {
        ProjectPresent.set(PresentProfiles.LIGHTWAVE);
        Map<String, Object> snap = ArtFramework.probe().asMap();
        assertTrue(snap.containsKey("presentProfile"));
        assertTrue(snap.containsKey("projectPresent"));
        @SuppressWarnings("unchecked")
        Map<String, Object> pp = (Map<String, Object>) snap.get("presentProfile");
        assertEquals(PresentProfiles.LIGHTWAVE, pp.get("project"));
        assertEquals(PresentProfiles.LIGHTWAVE, pp.get("active"));
        assertTrue(pp.containsKey("chrome"));
    }

    @Test
    public void mountUsesDeclaredPresentProfileWithoutChangingProject() {
        assertEquals(PresentProfiles.STS, ProjectPresent.id());
        UiNode root =
                UiNode.of(UiTypes.WINDOW)
                        .id("w")
                        .prop("title", "T")
                        .prop("present_profile", "lightwave")
                        .child(UiNode.of(UiTypes.LABEL).id("hello").prop("text", "hi").build())
                        .build();
        UiTree tree = UiTree.mount("lw", root);
        assertNotNull(tree.theme());
        assertEquals("lightwave", tree.theme().name());
        assertEquals(PresentProfiles.STS, ProjectPresent.id());
        PresentResolved r = tree.resolvePresent();
        assertEquals("lightwave", r.profileId);
        assertFalse(r.fromProject);
        ThemeColor c = tree.get("hello").getThemeColor("font_color", "Label");
        assertNotNull(c);
        assertEquals(1f, c.a, 0.001f);
        assertTrue(c.r > 0.5f && c.g > 0.5f && c.b > 0.5f);
    }

    @Test
    public void mountUsesDeclaredThemeName() {
        UiNode root =
                UiNode.of(UiTypes.WINDOW)
                        .id("w")
                        .prop("title", "T")
                        .prop("theme", "lightwave")
                        .child(UiNode.of(UiTypes.LABEL).id("hello").prop("text", "hi").build())
                        .build();
        UiTree tree = UiTree.mount("t", root);
        assertEquals("lightwave", tree.theme().name());
        assertEquals(PresentProfiles.STS, ProjectPresent.id());
    }

    @Test
    public void presentProfileNodeOverrideAndAttach() {
        UiNode root =
                UiNode.of(UiTypes.WINDOW)
                        .id("w")
                        .prop("title", "T")
                        .child(
                                UiNode.of(ArtNodeTypes.PRESENT_PROFILE)
                                        .id("skin")
                                        .prop("profile", "lightwave")
                                        .prop("mode", "override")
                                        .child(
                                                UiNode.of(UiTypes.PANEL)
                                                        .id("panel")
                                                        .child(
                                                                UiNode.of(UiTypes.LABEL)
                                                                        .id("hello")
                                                                        .prop("text", "hi")
                                                                        .build())
                                                        .build())
                                        .build())
                        .build();
        UiTree tree = UiTree.mount("node_pp", root);
        assertEquals("sts", tree.resolvePresent().profileId);
        assertTrue(tree.resolvePresent().fromProject);
        PresentResolved atPanel = tree.get("panel").resolvePresent();
        assertEquals("lightwave", atPanel.profileId);
        assertFalse(atPanel.fromProject);
        assertTrue(atPanel.chrome.cardAlpha < 1f);
    }

    @Test
    public void attachDoesNotTruncateParentOverride() {
        UiNode root =
                UiNode.of(UiTypes.WINDOW)
                        .id("w")
                        .prop("present_profile", "lightwave")
                        .child(
                                UiNode.of(ArtNodeTypes.PRESENT_PROFILE)
                                        .id("inner")
                                        .prop("profile", "sts")
                                        .prop("mode", "attach")
                                        .child(UiNode.of(UiTypes.LABEL).id("x").prop("text", "t").build())
                                        .build())
                        .build();
        UiTree tree = UiTree.mount("attach", root);
        // nearest attach (sts) wins whole package at label
        assertEquals("sts", tree.get("x").resolvePresent().profileId);
        // root still lightwave
        assertEquals("lightwave", tree.resolvePresent().profileId);
    }

    @Test
    public void openLightwaveDemoLayout() {
        ArtFramework.register(
                new WindowDef("lightwave_demo", WindowClass.SYNTHETIC, "layouts/lightwave_demo.json"));
        ArtFramework.open("lightwave_demo");
        UiTree tree = ArtFramework.tree("lightwave_demo");
        assertNotNull(tree);
        assertEquals("lightwave", tree.theme().name());
        assertEquals("lightwave", tree.resolvePresent().profileId);
        assertEquals(PresentProfiles.STS, ProjectPresent.id());
        assertNotNull(tree.get("panel"));
        assertNotNull(tree.get("motion"));
        assertNotNull(AnimationPlayers.get("lightwave_demo", "motion"));
        Map<String, Object> snap = ArtFramework.probe().asMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> wins = (Map<String, Object>) snap.get("windows");
        @SuppressWarnings("unchecked")
        Map<String, Object> byId = (Map<String, Object>) wins.get("byId");
        @SuppressWarnings("unchecked")
        Map<String, Object> demo = (Map<String, Object>) byId.get("lightwave_demo");
        assertNotNull(demo.get("present"));
        @SuppressWarnings("unchecked")
        Map<String, Object> present = (Map<String, Object>) demo.get("present");
        assertEquals("lightwave", present.get("id"));
    }

    @Test
    public void loaderParsesPresentProfileShorthand() {
        UiNode root =
                UiNodeLoader.parse(
                        "{\"type\":\"window\",\"id\":\"w\",\"title\":\"T\",\"present_profile\":\"lightwave\","
                                + "\"children\":[{\"type\":\"label\",\"id\":\"l\",\"text\":\"x\"}]}");
        assertEquals("lightwave", root.propString("present_profile", ""));
    }
}
