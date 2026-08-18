package artframework.component;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class TemplateExpanderTest {

    private ComponentRegistry registry;

    @Before
    public void setUp() {
        registry = new ComponentRegistry();
        UiNode dialog = UiNode.of(UiTypes.COL)
                .id("dialog_root")
                .child(UiNode.of(UiTypes.LABEL).id("title_lbl").prop("text", "${title}").build())
                .child(UiNode.of(UiTypes.SLOT).prop("name", "body").build())
                .child(UiNode.of(UiTypes.SLOT).prop("name", "footer").build())
                .build();
        registry.register("dialog", dialog);
    }

    @After
    public void tearDown() {
        ComponentRegistry.resetGlobalForTests();
    }

    @Test
    public void expandsRefWithSlotsAndPropSubstitution() {
        UiNode instance = UiNode.of(UiTypes.REF)
                .ref("dialog")
                .prop("title", "Vote")
                .slot(
                        "body",
                        Collections.singletonList(
                                UiNode.of(UiTypes.LABEL).id("body_txt").prop("text", "Ready?").build()))
                .slot(
                        "footer",
                        Collections.singletonList(
                                UiNode.of(UiTypes.BUTTON).id("yes").prop("text", "Yes").build()))
                .build();

        UiNode expanded = new TemplateExpander(registry).expand(instance);
        assertEquals(UiTypes.COL, expanded.type);
        NodeIndex idx = NodeIndex.of(expanded);
        assertEquals("Vote", idx.get("title_lbl").propString("text", ""));
        assertTrue(idx.contains("body_txt"));
        assertTrue(idx.contains("yes"));
        assertEquals(0, countType(expanded, UiTypes.REF));
        assertEquals(0, countType(expanded, UiTypes.SLOT));
    }

    @Test
    public void emptySlotBecomesFragmentWithoutChildren() {
        UiNode instance = UiNode.of(UiTypes.REF).ref("dialog").prop("title", "T").build();
        UiNode expanded = new TemplateExpander(registry).expand(instance);
        NodeIndex idx = NodeIndex.of(expanded);
        assertEquals("T", idx.get("title_lbl").propString("text", ""));
        // template root id + title label; empty slots add no interactive ids
        assertEquals(2, idx.size());
        assertTrue(idx.contains("dialog_root"));
        assertEquals(0, countType(expanded, UiTypes.SLOT));
    }

    @Test
    public void defaultChildrenFillDefaultSlot() {
        registry.register(
                "box",
                UiNode.of(UiTypes.COL)
                        .child(UiNode.of(UiTypes.SLOT).prop("name", "default").build())
                        .build());
        UiNode instance = UiNode.of(UiTypes.REF)
                .ref("box")
                .child(UiNode.of(UiTypes.BUTTON).id("inner").build())
                .build();
        UiNode expanded = new TemplateExpander(registry).expand(instance);
        assertTrue(NodeIndex.of(expanded).contains("inner"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingTemplateThrows() {
        new TemplateExpander(registry).expand(UiNode.of(UiTypes.REF).ref("nope").build());
    }

    @Test
    public void layoutAfterExpand() {
        UiNode instance = UiNode.of(UiTypes.REF)
                .ref("dialog")
                .prop("title", "X")
                .layout(new LayoutSpec(300f, 200f, 0f, 0f, false))
                .slot(
                        "body",
                        Collections.singletonList(
                                UiNode.of(UiTypes.BUTTON).id("ok").layout(new LayoutSpec(0f, 32f, 0f, 0f, false)).build()))
                .build();
        LayoutResult r = LayoutEngine.layout(new TemplateExpander(registry).expand(instance));
        assertTrue(r.hasId("ok"));
        assertEquals(300f, r.rootBounds.width, 0.001f);
    }

    @Test
    public void nestedCallPropertiesAreCopiedAndValidated() {
        registry.register(
                "nested",
                UiNode.of(UiTypes.LABEL).prop("payload", "${payload}").build());
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("items", new ArrayList<Object>(Arrays.<Object>asList("one")));

        UiNode expanded = new TemplateExpander(registry).expand(
                UiNode.of(UiTypes.REF).ref("nested").prop("payload", payload).build());
        ((List<Object>) payload.get("items")).add("two");

        @SuppressWarnings("unchecked")
        Map<String, Object> copied = (Map<String, Object>) expanded.props.get("payload");
        assertEquals(Arrays.<Object>asList("one"), copied.get("items"));
        assertNull(copied.get("later"));
        try {
            ((List<Object>) copied.get("items")).add("blocked");
            fail("expanded nested properties must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
        try {
            new TemplateExpander(registry).expand(
                    UiNode.of(UiTypes.REF).ref("nested").prop("payload", new Object()).build());
            fail("host values must be rejected through template calls");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private static int countType(UiNode n, String type) {
        int c = type.equals(n.type) ? 1 : 0;
        for (UiNode ch : n.children) {
            c += countType(ch, type);
        }
        for (java.util.List<UiNode> slot : n.slots.values()) {
            for (UiNode s : slot) {
                c += countType(s, type);
            }
        }
        return c;
    }
}
