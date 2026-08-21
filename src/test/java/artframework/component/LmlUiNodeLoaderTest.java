package artframework.component;

import org.junit.After;
import org.junit.Test;
import artframework.core.SignalNames;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LmlUiNodeLoaderTest {

    @After
    public void tearDown() {
        UiNodeRegistry.global().resetBuiltinsForTests();
    }

    @Test
    public void parseMinimalWindowWithButton() {
        UiNode n =
                LmlUiNodeLoader.parse(
                        "<window id=\"w\" title=\"Hello\">"
                                + "<button id=\"ok\" text=\"OK\" signals=\"pressed\"/>"
                                + "</window>");
        assertEquals(UiTypes.WINDOW, n.type);
        assertEquals("w", n.id);
        assertEquals("Hello", n.propString("title", ""));
        assertEquals(1, n.children.size());
        UiNode btn = n.children.get(0);
        assertEquals(UiTypes.BUTTON, btn.type);
        assertEquals("ok", btn.id);
        assertEquals("OK", btn.propString("text", ""));
        assertTrue(btn.declaresSignal(SignalNames.PRESSED));
    }

    @Test
    public void parseLayoutAndNestedCol() {
        UiNode n =
                LmlUiNodeLoader.parse(
                        "<window id=\"w\" title=\"T\" width=\"400\" height=\"240\" pad=\"8\">"
                                + "<col id=\"main\" gap=\"6\">"
                                + "<label id=\"hi\" text=\"Hi\"/>"
                                + "<slider id=\"vol\" min=\"0\" max=\"1\" value=\"0.5\" signals=\"value_changed\"/>"
                                + "</col>"
                                + "</window>");
        assertEquals(400f, n.layout.width, 0.01f);
        assertEquals(240f, n.layout.height, 0.01f);
        assertEquals(8f, n.layout.pad, 0.01f);
        UiNode col = n.children.get(0);
        assertEquals(UiTypes.COL, col.type);
        assertEquals(6f, col.layout.gap, 0.01f);
        assertEquals(2, col.children.size());
        assertTrue(col.children.get(1).declaresSignal(SignalNames.VALUE_CHANGED));
    }

    @Test
    public void parseEffectChild() {
        UiNode n =
                LmlUiNodeLoader.parse(
                        "<window id=\"w\" title=\"T\">"
                                + "<panel id=\"p\">"
                                + "<effect id=\"glow\" intensity=\"0.4\"/>"
                                + "<label id=\"l\" text=\"x\"/>"
                                + "</panel>"
                                + "</window>");
        UiNode panel = n.children.get(0);
        assertEquals(1, panel.effects.size());
        assertEquals("glow", panel.effects.get(0).id);
        assertEquals(0.4f, ((Number) panel.effects.get(0).params.get("intensity")).floatValue(), 0.01f);
        assertEquals(1, panel.children.size());
    }

    @Test
    public void parseNodeTypeAttributeForThirdParty() {
        UiNodeRegistry.global()
                .register(
                        UiNodeType.builder("my_mod.badge")
                                .kind(NodeKind.LEAF)
                                .allowsChildren(false)
                                .build());
        UiNode n =
                LmlUiNodeLoader.parse(
                        "<window id=\"w\" title=\"T\">"
                                + "<node type=\"my_mod.badge\" id=\"b\" text=\"x\"/>"
                                + "</window>");
        assertEquals("my_mod.badge", n.children.get(0).type);
    }

    @Test
    public void parseRefAndSlot() {
        ComponentRegistry.global()
                .register(
                        "dlg",
                        UiNode.of(UiTypes.PANEL)
                                .id("dlg_root")
                                .child(UiNode.of(UiTypes.SLOT).prop("name", "body").build())
                                .build());
        try {
            UiNode n =
                    LmlUiNodeLoader.parse(
                            "<window id=\"w\" title=\"T\">"
                                    + "<ref ref=\"dlg\" id=\"d\">"
                                    + "<slot name=\"body\"><label id=\"msg\" text=\"hi\"/></slot>"
                                    + "</ref>"
                                    + "</window>");
            UiNode expanded = new TemplateExpander().expand(n);
            assertEquals("d", expanded.children.get(0).id);
            assertEquals("msg", expanded.children.get(0).children.get(0).id);
        } finally {
            ComponentRegistry.global().unregister("dlg");
        }
    }

    @Test
    public void lmlRequiresExplicitSignalsOnInteractiveLeaves() {
        try {
            LmlUiNodeLoader.parse(
                    "<window id=\"w\" title=\"T\"><button id=\"ok\" text=\"OK\"/></window>");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().toLowerCase().contains("signal"));
        }
    }

    @Test
    public void rejectUnknownType() {
        try {
            LmlUiNodeLoader.parse(
                    "<window id=\"w\" title=\"T\"><not_a_type id=\"x\"/></window>");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("unknown") || e.getMessage().contains("not_a_type"));
        }
    }

    @Test
    public void rejectExternalEntity() {
        PrintStream originalErr = System.err;
        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(capturedErr));
        try {
            LmlUiNodeLoader.parse(
                    "<?xml version=\"1.0\"?>"
                            + "<!DOCTYPE window [ <!ENTITY xxe SYSTEM \"file:///etc/passwd\"> ]>"
                            + "<window id=\"w\" title=\"&xxe;\"/>");
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("", capturedErr.toString());
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    public void loadClasspathLml() {
        UiNode n = LmlUiNodeLoader.loadClasspath("layouts/lml_sample.lml");
        assertEquals(UiTypes.WINDOW, n.type);
        assertEquals("lml_sample", n.id);
    }

    @Test
    public void syntheticRuntimeDispatchesLmlResource() {
        artframework.api.ArtFramework.register(
                new artframework.api.WindowDef(
                        "lml_demo",
                        artframework.api.WindowClass.SYNTHETIC,
                        "layouts/lml_sample.lml"));
        artframework.api.ArtFramework.open("lml_demo");
        try {
            artframework.component.UiNode root = artframework.api.ArtFramework.uiRoot("lml_demo");
            assertEquals("lml_sample", root.id);
            assertTrue(artframework.presentation.PresentationRuntime.find(
                    artframework.presentation.PresentationRuntime.context("lml_demo"), "ok") != null);
        } finally {
            artframework.api.ArtFramework.close("lml_demo");
            artframework.api.ArtFramework.resetForTests();
        }
    }
}
