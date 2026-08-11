package artframework.core;

import artframework.component.UiNode;
import artframework.component.UiNodeLoader;
import artframework.component.UiTypes;
import artframework.presentation.Node;
import artframework.presentation.NodeTree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UiTreeSignalTest {
    @Test public void mountBuildsNodesAndStablePaths() {
        NodeTree tree = NodeTree.mount("tree:w", sample(), null);
        try {
            assertEquals("w", tree.windowId());
            assertEquals(UiTypes.WINDOW, tree.root().type());
            assertEquals(UiTypes.BUTTON, tree.get("ok").type());
            assertEquals("ok", tree.find("comp_sample/main_col/actions/ok").id());
        } finally { tree.close(); }
    }

    @Test public void hierarchyAndLifecycleAreEcsBacked() {
        final List<String> events = new ArrayList<String>();
        NodeTree tree = NodeTree.mount("tree:w", sample(), new artframework.presentation.NodeTreeLifecycle() {
            public void onMount(Node n) { events.add("m:" + n.id()); }
            public void onReady(Node n) { events.add("r:" + n.id()); }
            public void onUnmount(Node n) { events.add("u:" + n.id()); }
        });
        Node ok = tree.get("ok");
        assertTrue(ok.isMounted());
        assertTrue(ok.isReady());
        assertNotNull(ok.parent());
        tree.close();
        assertTrue(events.indexOf("m:comp_sample") < events.indexOf("m:ok"));
        assertTrue(events.indexOf("r:ok") < events.indexOf("r:comp_sample"));
        assertTrue(events.indexOf("u:ok") < events.indexOf("u:comp_sample"));
    }

    @Test public void signalConnectEmitDisconnect() {
        NodeTree tree = NodeTree.mount("tree:w", sample(), null);
        AtomicInteger count = new AtomicInteger();
        artframework.core.SignalHandler handler = args -> count.incrementAndGet();
        tree.connect("ok", SignalNames.PRESSED, handler);
        tree.emit("ok", SignalNames.PRESSED);
        tree.disconnect("ok", SignalNames.PRESSED, handler);
        tree.emit("ok", SignalNames.PRESSED);
        assertEquals(1, count.get());
        tree.close();
    }

    @Test public void mutablePropsStayInEcs() {
        NodeTree tree = NodeTree.mount("tree:w", sample(), null);
        Node label = tree.get("hello");
        label.set("text", "Hi");
        assertEquals("Hi", label.propString("text", ""));
        assertFalse(tree.frame().items == null);
        tree.close();
    }

    private static UiNode sample() {
        UiNode raw = UiNodeLoader.loadClasspath("layouts/composition_sample.json");
        return new artframework.component.TemplateExpander().expand(raw);
    }
}
