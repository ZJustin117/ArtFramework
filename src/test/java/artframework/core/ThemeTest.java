package artframework.core;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.component.TemplateExpander;
import artframework.component.UiNode;
import artframework.component.UiNodeLoader;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ThemeTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Themes.resetForTests();
    }

    @Test
    public void localOverrideBeatsTheme() {
        Theme theme = new Theme();
        theme.setColor("Label", "font_color", 1f, 0f, 0f, 1f);
        UiTree tree = UiTree.mount("w", expandedSample());
        tree.setTheme(theme);
        UiInstance hello = tree.get("hello");
        hello.setThemeColorOverride("font_color", 0f, 1f, 0f, 1f);
        ThemeColor c = hello.getThemeColor("font_color", "Label");
        assertNotNull(c);
        assertEquals(0f, c.r, 0.001f);
        assertEquals(1f, c.g, 0.001f);
    }

    @Test
    public void cascadeWalksParentTheme() {
        Theme rootTheme = new Theme();
        rootTheme.setConstant("Box", "separation", 8);
        Theme childTheme = new Theme();
        childTheme.setColor("Button", "font_color", 1f, 1f, 0f, 1f);

        UiTree tree = UiTree.mount("w", expandedSample());
        tree.setTheme(rootTheme);
        tree.get("main_col").setTheme(childTheme);

        assertEquals(8, tree.get("ok").getThemeConstant("separation", "Box"));
        ThemeColor btn = tree.get("ok").getThemeColor("font_color", "Button");
        assertNotNull(btn);
        assertEquals(1f, btn.r, 0.001f);
        assertEquals(1f, btn.g, 0.001f);
    }

    @Test
    public void defaultStsThemeHasTokens() {
        Theme t = StsTheme.createDefault();
        assertNotNull(t.getColor("Label", "font_color"));
        assertNotNull(t.getColor("Button", "font_color"));
        assertTrue(t.getConstant("Box", "separation") >= 0);
        assertTrue(t.getFontSize("Label", "font_size") > 0);
    }

    @Test
    public void openUsesGlobalDefaultTheme() {
        Themes.setDefault(StsTheme.createDefault());
        ArtFramework.register(
                new WindowDef("comp", WindowClass.SYNTHETIC, "layouts/composition_sample.json"));
        ArtFramework.open("comp");
        UiTree tree = ArtFramework.tree("comp");
        assertNotNull(tree.theme());
        ThemeColor c = tree.get("hello").getThemeColor("font_color", "Label");
        assertNotNull(c);
    }

    @Test
    public void probeIncludesThemeSummaryWhenPresent() {
        Themes.setDefault(StsTheme.createDefault());
        ArtFramework.register(
                new WindowDef("comp", WindowClass.SYNTHETIC, "layouts/composition_sample.json"));
        ArtFramework.open("comp");
        Map<String, Object> snap = ArtFramework.probe().asMap();
        assertTrue(snap.containsKey("theme") || windowsHaveTheme(snap));
    }

    @Test
    public void missingItemReturnsNullOrZero() {
        Theme empty = new Theme();
        assertNull(empty.getColor("Label", "font_color"));
        assertEquals(0, empty.getConstant("Box", "separation"));
        assertEquals(0, empty.getFontSize("Label", "font_size"));
    }

    private static boolean windowsHaveTheme(Map<String, Object> snap) {
        Object windows = snap.get("windows");
        if (!(windows instanceof Map)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> w = (Map<String, Object>) windows;
        Object byId = w.get("byId");
        if (!(byId instanceof Map)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> by = (Map<String, Object>) byId;
        Object one = by.get("comp");
        if (!(one instanceof Map)) {
            return false;
        }
        return ((Map<?, ?>) one).containsKey("theme");
    }

    private static UiNode expandedSample() {
        return new TemplateExpander().expand(UiNodeLoader.loadClasspath("layouts/composition_sample.json"));
    }
}
