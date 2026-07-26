package spireui.api;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SpireUITest {

    @After
    public void tearDown() {
        SpireUI.resetForTests();
    }

    @Test
    public void registerOpenCloseSynthetic() {
        SpireUI.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        assertTrue(SpireUI.isRegistered("demo"));

        WindowHandle h = SpireUI.open("demo");
        assertEquals("demo", h.id());
        assertEquals(WindowClass.SYNTHETIC, h.windowClass());
        assertTrue(h.isOpen());
        assertEquals(1, SpireUI.listOpenIds().size());

        SpireUI.close("demo");
        assertFalse(h.isOpen());
        assertTrue(SpireUI.listOpenIds().isEmpty());
    }

    @Test
    public void bindNativeTemplate() {
        SpireUI.register(new WindowDef("sts.map", WindowClass.NATIVE_TEMPLATE, "sts.map"));
        WindowHandle h = SpireUI.bind("sts.map");
        assertEquals(WindowClass.NATIVE_TEMPLATE, h.windowClass());
        h.close();
        assertFalse(h.isOpen());
    }

    @Test(expected = IllegalArgumentException.class)
    public void openUnregisteredThrows() {
        SpireUI.open("missing");
    }

    @Test(expected = IllegalArgumentException.class)
    public void registerNullThrows() {
        SpireUI.register(null);
    }

    @Test
    public void reRegisterOverwritesDef() {
        SpireUI.register(new WindowDef("w", WindowClass.SYNTHETIC, "layouts/demo.json"));
        SpireUI.register(new WindowDef("w", WindowClass.NATIVE_TEMPLATE, "sts.map"));
        WindowHandle h = SpireUI.open("w");
        assertEquals(WindowClass.NATIVE_TEMPLATE, h.windowClass());
    }

    @Test
    public void doubleOpenReplacesOpenEntry() {
        SpireUI.register(new WindowDef("w", WindowClass.SYNTHETIC, "layouts/demo.json"));
        WindowHandle first = SpireUI.open("w");
        WindowHandle second = SpireUI.open("w");
        assertEquals(1, SpireUI.listOpenIds().size());
        assertSame(second, SpireUI.find("w"));
        assertFalse(first.isOpen());
        second.close();
        assertNull(SpireUI.find("w"));
        assertTrue(SpireUI.listOpenIds().isEmpty());
    }

    @Test
    public void findOpenClosedMissing() {
        SpireUI.register(new WindowDef("w", WindowClass.SYNTHETIC, "layouts/demo.json"));
        assertNull(SpireUI.find("w"));
        WindowHandle h = SpireUI.open("w");
        assertSame(h, SpireUI.find("w"));
        SpireUI.close("w");
        assertNull(SpireUI.find("w"));
        assertNull(SpireUI.find("nope"));
    }

    @Test
    public void closeUnopenedIsNoOp() {
        SpireUI.close("missing");
        assertTrue(SpireUI.listOpenIds().isEmpty());
    }

    @Test
    public void handleCloseRemovesFromOpen() {
        SpireUI.register(new WindowDef("w", WindowClass.SYNTHETIC, "layouts/demo.json"));
        WindowHandle h = SpireUI.open("w");
        h.close();
        assertFalse(h.isOpen());
        assertNull(SpireUI.find("w"));
        assertTrue(SpireUI.listOpenIds().isEmpty());
    }

    @Test
    public void resetForTestsClearsDefsAndOpen() {
        SpireUI.register(new WindowDef("w", WindowClass.SYNTHETIC, "layouts/demo.json"));
        SpireUI.open("w");
        SpireUI.resetForTests();
        assertFalse(SpireUI.isRegistered("w"));
        assertTrue(SpireUI.listOpenIds().isEmpty());
        assertNull(SpireUI.find("w"));
    }
}
