package artframework.component;

import org.junit.Test;
import artframework.core.SignalNames;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UiNodeSignalsTest {

    @Test
    public void builderRecordsSignalsInOrderWithoutDuplicates() {
        UiNode n =
                UiNode.of(UiTypes.BUTTON)
                        .id("ok")
                        .signal(SignalNames.PRESSED)
                        .signal(SignalNames.PRESSED)
                        .signal(SignalNames.VALUE_CHANGED)
                        .build();
        assertEquals(
                Arrays.asList(SignalNames.PRESSED, SignalNames.VALUE_CHANGED), n.signals);
        try {
            n.signals.add("x");
            fail("signals must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void emptySignalsGetBuiltInDefaultsForLeaves() {
        UiNode button = UiNode.of(UiTypes.BUTTON).id("b").build();
        assertTrue(button.declaresSignal(SignalNames.PRESSED));
        UiNode slider = UiNode.of(UiTypes.SLIDER).id("s").build();
        assertTrue(slider.declaresSignal(SignalNames.VALUE_CHANGED));
        UiNode field = UiNode.of(UiTypes.TEXTFIELD).id("t").build();
        assertTrue(field.declaresSignal(SignalNames.TEXT_CHANGED));
        assertTrue(field.declaresSignal(SignalNames.TEXT_SUBMITTED));
        UiNode box = UiNode.of(UiTypes.CHECKBOX).id("c").build();
        assertTrue(box.declaresSignal(SignalNames.TOGGLED));
        UiNode hit = UiNode.of(UiTypes.HITAREA).id("h").build();
        assertTrue(hit.declaresSignal(SignalNames.PRESSED));
        UiNode label = UiNode.of(UiTypes.LABEL).id("l").build();
        assertTrue(label.signals.isEmpty());
    }

    @Test
    public void explicitSignalsDisableDefaultFill() {
        UiNode n =
                UiNode.of(UiTypes.BUTTON)
                        .id("b")
                        .signals(Arrays.asList(SignalNames.VALUE_CHANGED))
                        .build();
        assertEquals(Arrays.asList(SignalNames.VALUE_CHANGED), n.signals);
        assertFalse(n.declaresSignal(SignalNames.PRESSED));
    }

    @Test
    public void loaderParsesSignalsArray() {
        UiNode n =
                UiNodeLoader.parse(
                        "{"
                                + "\"type\":\"window\",\"id\":\"w\",\"title\":\"T\","
                                + "\"children\":[{"
                                + "\"type\":\"button\",\"id\":\"ok\",\"text\":\"OK\","
                                + "\"signals\":[\"pressed\",\"value_changed\"]"
                                + "}]}");
        UiNode btn = n.children.get(0);
        assertEquals(
                Arrays.asList(SignalNames.PRESSED, SignalNames.VALUE_CHANGED), btn.signals);
    }

    @Test
    public void loaderAppliesDefaultsWhenSignalsOmitted() {
        UiNode n =
                UiNodeLoader.parse(
                        "{"
                                + "\"type\":\"window\",\"id\":\"w\",\"title\":\"T\","
                                + "\"children\":[{\"type\":\"button\",\"id\":\"ok\",\"text\":\"OK\"}]}");
        assertTrue(n.children.get(0).declaresSignal(SignalNames.PRESSED));
    }

    @Test
    public void expanderPreservesSignals() {
        UiNode root =
                UiNode.of(UiTypes.COL)
                        .child(
                                UiNode.of(UiTypes.BUTTON)
                                        .id("ok")
                                        .signal(SignalNames.PRESSED)
                                        .build())
                        .build();
        UiNode expanded = new TemplateExpander().expand(root);
        assertTrue(expanded.children.get(0).declaresSignal(SignalNames.PRESSED));
    }

    @Test
    public void refMergeUnionsSignalsFromRefNodeOntoTemplateRoot() {
        ComponentRegistry reg = new ComponentRegistry();
        reg.register(
                "card",
                UiNode.of(UiTypes.PANEL)
                        .id("card_root")
                        .signal(SignalNames.PRESSED)
                        .build());
        UiNode call =
                UiNode.of(UiTypes.REF)
                        .ref("card")
                        .id("c1")
                        .signal(SignalNames.VALUE_CHANGED)
                        .build();
        UiNode out = new TemplateExpander(reg).expand(call);
        assertTrue(out.declaresSignal(SignalNames.PRESSED));
        assertTrue(out.declaresSignal(SignalNames.VALUE_CHANGED));
        assertEquals("c1", out.id);
    }

    @Test
    public void blankSignalRejected() {
        try {
            UiNode.of(UiTypes.BUTTON).signal("  ").build();
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("signal"));
        }
    }

    @Test
    public void defaultSignalsHelper() {
        List<String> d = UiTypes.defaultSignals(UiTypes.BUTTON);
        assertEquals(1, d.size());
        assertEquals(SignalNames.PRESSED, d.get(0));
        assertTrue(UiTypes.defaultSignals(UiTypes.PANEL).isEmpty());
    }
}
