package artframework.component;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.api.UiOpResult;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.core.SignalHandler;
import artframework.core.SignalNames;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NewWidgetsTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
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
        ArtFramework.register(
                new WindowDef("w", WindowClass.SYNTHETIC, "layouts/widgets_sample.json"));
        ArtFramework.open("w");
        WidgetSession s = ArtFramework.widgets("w");
        assertNotNull(s);
        assertTrue(s.hasTextField("name"));
        assertEquals("", s.getText("name"));
        assertTrue(s.hasCheckbox("agree"));
        assertFalse(s.getChecked("agree"));
        assertTrue(s.hasProgress("load"));
        assertEquals(0.25f, s.getProgress("load"), 0.001f);
        assertNotNull(ArtFramework.tree("w").get("body_scroll"));
        assertNotNull(ArtFramework.tree("w").get("submit"));
    }

    @Test
    public void setTextAndSubmitEmitSignals() {
        ArtFramework.register(
                new WindowDef("w", WindowClass.SYNTHETIC, "layouts/widgets_sample.json"));
        ArtFramework.open("w");
        final AtomicReference<String> changed = new AtomicReference<String>();
        final AtomicReference<String> submitted = new AtomicReference<String>();
        ArtFramework.tree("w")
                .connect(
                        "name",
                        SignalNames.TEXT_CHANGED,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                changed.set((String) args[0]);
                            }
                        });
        ArtFramework.tree("w")
                .connect(
                        "name",
                        SignalNames.TEXT_SUBMITTED,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                submitted.set((String) args[0]);
                            }
                        });
        assertEquals(UiOpResult.Status.OK, ArtFramework.ops().setText("w", "name", "Ada").status);
        assertEquals("Ada", changed.get());
        assertEquals(UiOpResult.Status.OK, ArtFramework.ops().submitText("w", "name").status);
        assertEquals("Ada", submitted.get());
    }

    @Test
    public void checkboxToggleAndProgress() {
        ArtFramework.register(
                new WindowDef("w", WindowClass.SYNTHETIC, "layouts/widgets_sample.json"));
        ArtFramework.open("w");
        final AtomicReference<Boolean> toggled = new AtomicReference<Boolean>();
        ArtFramework.tree("w")
                .connect(
                        "agree",
                        SignalNames.TOGGLED,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                toggled.set((Boolean) args[0]);
                            }
                        });
        assertEquals(UiOpResult.Status.OK, ArtFramework.ops().toggleCheckbox("w", "agree").status);
        assertTrue(toggled.get().booleanValue());
        assertEquals(UiOpResult.Status.OK, ArtFramework.ops().setChecked("w", "agree", false).status);
        assertFalse(ArtFramework.widgets("w").getChecked("agree"));
        assertEquals(UiOpResult.Status.OK, ArtFramework.ops().setProgress("w", "load", 0.9f).status);
        assertEquals(0.9f, ArtFramework.widgets("w").getProgress("load"), 0.001f);
        assertEquals(UiOpResult.Status.OK, ArtFramework.ops().click("w", "agree").status);
        assertTrue(ArtFramework.widgets("w").getChecked("agree"));
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
        ArtFramework.register(
                new WindowDef("w", WindowClass.SYNTHETIC, "layouts/widgets_sample.json"));
        ArtFramework.open("w");
        Map<String, Object> snap = ArtFramework.probe().asMap();
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
