package artframework.component;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CompositionPipelineTest {

    @After
    public void tearDown() {
        ComponentRegistry.resetGlobalForTests();
    }

    @Test
    public void compileClasspathSample() {
        UiNode root = Composition.loadClasspath("layouts/composition_sample.json");
        LayoutResult r = Composition.layout(Composition.expand(root));
        assertEquals("comp_sample", r.root.id);
        assertTrue(r.hasId("ok"));
        assertTrue(r.hasId("cancel"));
        assertTrue(r.hasId("intensity"));
        assertTrue(r.hasId("card_zone"));
        Rect ok = r.boundsOf("ok");
        Rect cancel = r.boundsOf("cancel");
        assertTrue(cancel.x > ok.x);
    }

    @Test
    public void indexDetectsDuplicateIds() {
        try {
            NodeIndex.of(UiNode.of(UiTypes.COL)
                    .child(UiNode.of(UiTypes.BUTTON).id("x").build())
                    .child(UiNode.of(UiTypes.BUTTON).id("x").build())
                    .build());
            throw new AssertionError("expected duplicate id");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("duplicate"));
        }
    }

    @Test
    public void registryRegisterAndExpandViaComposition() {
        ComponentRegistry reg = new ComponentRegistry();
        reg.register(
                "line",
                UiNode.of(UiTypes.ROW)
                        .child(UiNode.of(UiTypes.LABEL).id("L").prop("text", "${t}").build())
                        .build());
        UiNode tree = UiNode.of(UiTypes.REF).ref("line").prop("t", "Hi").build();
        UiNode expanded = Composition.expand(tree, reg);
        assertEquals("Hi", NodeIndex.of(expanded).get("L").propString("text", ""));
    }
}
