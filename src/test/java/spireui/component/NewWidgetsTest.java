package spireui.component;

import org.junit.After;
import org.junit.Test;
import spireui.api.SpireUI;
import spireui.api.UiOpResult;
import spireui.api.WindowClass;
import spireui.api.WindowDef;
import spireui.core.SignalHandler;
import spireui.core.SignalNames;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NewWidgetsTest {

    @After
    public void tearDown() {
        SpireUI.resetForTests();
    }

    @Test
    public void loaderAcceptsNewTypes() {
        UiNode n = UiNodeLoader.parse(
                "{\"type\":\"window\",\"title\":\"T\",\"children\":["
                        + "{\"type\":\"textfield\",\"id\":\"t\",\"text\":\"hi\"},"
                        + "{\"type\":\"checkbox\",\"id\":\"c\",\"checked\":true},"
                        + "{\"type\":\"progress\",\"id\":\"p\",\"value\":0.3},"
                        + "{\"type\":\"scroll\",\"id\":\"s\",\"children\":[]},"
                        + "{\"type\":\"center\",\"id\":\"z\",\"children\":[]},"
                        + "{\"type\":\"margin\",\"id\":\"m\",\"children\":[]}"
                        + "]}");
        assertEquals(6, n.children.size());
        assertEquals(UiTypes.TEXTFIELD, n.children.get(0).type);
    }

    @Test
    public void openWidgetsSampleSeedsState() {
        SpireUI.register(
                new WindowDef("w", WindowClass.SYNTHETIC, "layouts/widgets_sample.json"));
        SpireUI.open("w");
        WidgetSession s = SpireUI.widgets("w");
        assertNotNull(s);
        assertTrue(s.hasTextField("name"));
        assertEquals("", s.getText("name"));
        assertTrue(s.hasCheckbox("agree"));
        assertFalse(s.getChecked("agree"));
        assertTrue(s.hasProgress("load"));
        assertEquals(0.25f, s.getProgress("load"), 0.001f);
        assertNotNull(SpireUI.tree("w").get("body_scroll"));
        assertNotNull(SpireUI.tree("w").get("submit"));
    }

    @Test
    public void setTextAndSubmitEmitSignals() {
        SpireUI.register(
                new WindowDef("w", WindowClass.SYNTHETIC, "layouts/widgets_sample.json"));
        SpireUI.open("w");
        final AtomicReference<String> changed = new AtomicReference<String>();
        final AtomicReference<String> submitted = new AtomicReference<String>();
        SpireUI.tree("w")
                .connect(
                        "name",
                        SignalNames.TEXT_CHANGED,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                changed.set((String) args[0]);
                            }
                        });
        SpireUI.tree("w")
                .connect(
                        "name",
                        SignalNames.TEXT_SUBMITTED,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                submitted.set((String) args[0]);
                            }
                        });
        assertEquals(UiOpResult.Status.OK, SpireUI.ops().setText("w", "name", "Ada").status);
        assertEquals("Ada", changed.get());
        assertEquals(UiOpResult.Status.OK, SpireUI.ops().submitText("w", "name").status);
        assertEquals("Ada", submitted.get());
    }

    @Test
    public void checkboxToggleAndProgress() {
        SpireUI.register(
                new WindowDef("w", WindowClass.SYNTHETIC, "layouts/widgets_sample.json"));
        SpireUI.open("w");
        final AtomicReference<Boolean> toggled = new AtomicReference<Boolean>();
        SpireUI.tree("w")
                .connect(
                        "agree",
                        SignalNames.TOGGLED,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                toggled.set((Boolean) args[0]);
                            }
                        });
        assertEquals(UiOpResult.Status.OK, SpireUI.ops().toggleCheckbox("w", "agree").status);
        assertTrue(toggled.get().booleanValue());
        assertEquals(UiOpResult.Status.OK, SpireUI.ops().setChecked("w", "agree", false).status);
        assertFalse(SpireUI.widgets("w").getChecked("agree"));
        assertEquals(UiOpResult.Status.OK, SpireUI.ops().setProgress("w", "load", 0.9f).status);
        assertEquals(0.9f, SpireUI.widgets("w").getProgress("load"), 0.001f);
        assertEquals(UiOpResult.Status.OK, SpireUI.ops().click("w", "agree").status);
        assertTrue(SpireUI.widgets("w").getChecked("agree"));
    }

    @Test
    public void centerPlacesChild() {
        UiNode root = UiNode.of(UiTypes.CENTER)
                .layout(new LayoutSpec(200f, 100f, 0f, 0f, false))
                .child(UiNode.of(UiTypes.BUTTON)
                        .id("b")
                        .layout(new LayoutSpec(40f, 20f, 0f, 0f, false))
                        .build())
                .build();
        LayoutResult r = LayoutEngine.layout(root);
        assertEquals(80f, r.boundsOf("b").x, 0.001f);
        assertEquals(40f, r.boundsOf("b").y, 0.001f);
    }

    @Test
    public void probeListsNewControls() {
        SpireUI.register(
                new WindowDef("w", WindowClass.SYNTHETIC, "layouts/widgets_sample.json"));
        SpireUI.open("w");
        Map<String, Object> snap = SpireUI.probe().asMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> windows = (Map<String, Object>) snap.get("windows");
        @SuppressWarnings("unchecked")
        Map<String, Object> byId = (Map<String, Object>) windows.get("byId");
        @SuppressWarnings("unchecked")
        Map<String, Object> one = (Map<String, Object>) byId.get("w");
        @SuppressWarnings("unchecked")
        Map<String, Object> controls = (Map<String, Object>) one.get("controls");
        assertTrue(((java.util.List<?>) controls.get("textFieldIds")).contains("name"));
        assertTrue(((java.util.List<?>) controls.get("checkboxIds")).contains("agree"));
        assertTrue(((java.util.List<?>) controls.get("progressIds")).contains("load"));
    }
}
