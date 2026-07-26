package spireui.c1;

import org.junit.After;
import org.junit.Test;
import spireui.api.SpireUI;
import spireui.api.WindowClass;
import spireui.api.WindowDef;
import spireui.api.WindowHandle;
import spireui.c1.layout.LayoutNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SyntheticRuntimeTest {

    @After
    public void tearDown() {
        SpireUI.resetForTests();
    }

    @Test
    public void logicRuntimeAvailableWithoutStage() {
        assertTrue(SyntheticRuntime.isAvailable());
        assertFalse(SyntheticRuntime.isStageReady());
    }

    @Test
    public void openTracksLayoutRoot() {
        SpireUI.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        WindowHandle h = SpireUI.open("demo");
        assertTrue(h.isOpen());
        assertTrue(WindowManager.contains("demo"));
        LayoutNode root = SpireUI.layoutRoot("demo");
        assertNotNull(root);
        assertEquals(LayoutNode.Type.WINDOW, root.type);
        assertEquals("SpireUI Demo", root.title);
    }

    @Test
    public void closeRemovesManagerEntry() {
        SpireUI.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        WindowHandle h = SpireUI.open("demo");
        h.close();
        assertFalse(h.isOpen());
        assertFalse(WindowManager.contains("demo"));
        assertNull(SpireUI.layoutRoot("demo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void openMissingLayoutLeavesNoOpen() {
        SpireUI.register(new WindowDef("bad", WindowClass.SYNTHETIC, "layouts/missing.json"));
        try {
            SpireUI.open("bad");
        } finally {
            assertTrue(SpireUI.listOpenIds().isEmpty());
            assertFalse(WindowManager.contains("bad"));
        }
    }

    @Test
    public void doubleOpenReplacesPrevious() {
        SpireUI.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        WindowHandle first = SpireUI.open("demo");
        WindowHandle second = SpireUI.open("demo");
        assertEquals(1, SpireUI.listOpenIds().size());
        assertFalse(first.isOpen());
        assertTrue(second.isOpen());
        assertTrue(WindowManager.contains("demo"));
    }
}
