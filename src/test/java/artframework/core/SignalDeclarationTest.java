package artframework.core;

import artframework.component.UiNode;
import artframework.component.UiTypes;
import artframework.presentation.Node;
import artframework.presentation.NodeTree;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SignalDeclarationTest {
    @Test public void instanceExposesDeclaredSignals() {
        NodeTree tree = mountButtonTree();
        Node ok = tree.get("ok");
        assertTrue(ok.declaresSignal(SignalNames.PRESSED));
        assertFalse(ok.declaresSignal(SignalNames.VALUE_CHANGED));
        assertEquals(1, ok.signals().size());
        tree.close();
    }

    @Test public void connectAndEmitAllowDeclaredSignal() {
        NodeTree tree = mountButtonTree();
        final int[] n = {0};
        tree.connect("ok", SignalNames.PRESSED, args -> n[0]++);
        tree.emit("ok", SignalNames.PRESSED);
        tree.get("ok").emitSignal(SignalNames.PRESSED);
        assertEquals(2, n[0]);
        tree.close();
    }

    @Test public void undeclaredPortsAreRejected() {
        NodeTree tree = mountButtonTree();
        try { tree.emit("ok", SignalNames.TOGGLED); fail(); }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains(SignalNames.TOGGLED)); }
        try { tree.get("ok").connect(SignalNames.VALUE_CHANGED, args -> {}); fail(); }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains(SignalNames.VALUE_CHANGED)); }
        tree.close();
    }

    private static NodeTree mountButtonTree() {
        UiNode root = UiNode.of(UiTypes.WINDOW).id("w")
                .child(UiNode.of(UiTypes.BUTTON).id("ok").build()).build();
        return NodeTree.mount("tree:win", root, null);
    }
}
