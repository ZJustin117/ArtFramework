package artframework.c1;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.api.WindowHandle;
import artframework.c1.layout.LayoutNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SyntheticRuntimeTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void logicRuntimeAvailableWithoutStage() {
        assertTrue(SyntheticRuntime.isAvailable());
        assertFalse(SyntheticRuntime.isStageReady());
    }

    @Test
    public void openTracksLayoutRoot() {
        ArtFramework.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        WindowHandle h = ArtFramework.open("demo");
        assertTrue(h.isOpen());
        assertTrue(WindowManager.contains("demo"));
        LayoutNode root = ArtFramework.layoutRoot("demo");
        assertNotNull(root);
        assertEquals(LayoutNode.Type.WINDOW, root.type);
        assertEquals("ArtFramework Demo", root.title);
    }

    @Test
    public void closeRemovesManagerEntry() {
        ArtFramework.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        WindowHandle h = ArtFramework.open("demo");
        h.close();
        assertFalse(h.isOpen());
        assertFalse(WindowManager.contains("demo"));
        assertNull(ArtFramework.layoutRoot("demo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void openMissingLayoutLeavesNoOpen() {
        ArtFramework.register(new WindowDef("bad", WindowClass.SYNTHETIC, "layouts/missing.json"));
        try {
            ArtFramework.open("bad");
        } finally {
            assertTrue(ArtFramework.listOpenIds().isEmpty());
            assertFalse(WindowManager.contains("bad"));
        }
    }

    @Test
    public void doubleOpenReplacesPrevious() {
        ArtFramework.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        WindowHandle first = ArtFramework.open("demo");
        WindowHandle second = ArtFramework.open("demo");
        assertEquals(1, ArtFramework.listOpenIds().size());
        assertFalse(first.isOpen());
        assertTrue(second.isOpen());
        assertTrue(WindowManager.contains("demo"));
    }
}
