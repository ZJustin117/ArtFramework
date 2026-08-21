package artframework.api;

import org.junit.After;
import org.junit.Test;
import artframework.c2.NativeTemplateRuntime;
import artframework.component.NativeTemplateIds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ArtFrameworkTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void registerOpenCloseSynthetic() {
        ArtFramework.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        assertTrue(ArtFramework.isRegistered("demo"));

        WindowHandle h = ArtFramework.open("demo");
        assertEquals("demo", h.id());
        assertEquals(WindowClass.SYNTHETIC, h.windowClass());
        assertTrue(h.isOpen());
        assertEquals(1, ArtFramework.listOpenIds().size());

        ArtFramework.close("demo");
        assertFalse(h.isOpen());
        assertTrue(ArtFramework.listOpenIds().isEmpty());
    }

    @Test
    public void bindNativeTemplate() {
        ArtFramework.register(new WindowDef("sts1.map", WindowClass.NATIVE_TEMPLATE, "sts1.map"));
        WindowHandle h = ArtFramework.bind("sts1.map");
        assertEquals(WindowClass.NATIVE_TEMPLATE, h.windowClass());
        h.close();
        assertFalse(h.isOpen());
    }

    @Test(expected = IllegalArgumentException.class)
    public void openUnregisteredThrows() {
        ArtFramework.open("missing");
    }

    @Test(expected = IllegalArgumentException.class)
    public void registerNullThrows() {
        ArtFramework.register(null);
    }

    @Test
    public void reRegisterOverwritesDef() {
        ArtFramework.register(new WindowDef("w", WindowClass.SYNTHETIC, "layouts/demo.json"));
        ArtFramework.register(new WindowDef("w", WindowClass.NATIVE_TEMPLATE, "sts1.map"));
        WindowHandle h = ArtFramework.open("w");
        assertEquals(WindowClass.NATIVE_TEMPLATE, h.windowClass());
    }

    @Test
    public void doubleOpenReplacesOpenEntry() {
        ArtFramework.register(new WindowDef("w", WindowClass.SYNTHETIC, "layouts/demo.json"));
        WindowHandle first = ArtFramework.open("w");
        WindowHandle second = ArtFramework.open("w");
        assertEquals(1, ArtFramework.listOpenIds().size());
        assertSame(second, ArtFramework.find("w"));
        assertFalse(first.isOpen());
        second.close();
        assertNull(ArtFramework.find("w"));
        assertTrue(ArtFramework.listOpenIds().isEmpty());
    }

    @Test
    public void findOpenClosedMissing() {
        ArtFramework.register(new WindowDef("w", WindowClass.SYNTHETIC, "layouts/demo.json"));
        assertNull(ArtFramework.find("w"));
        WindowHandle h = ArtFramework.open("w");
        assertSame(h, ArtFramework.find("w"));
        ArtFramework.close("w");
        assertNull(ArtFramework.find("w"));
        assertNull(ArtFramework.find("nope"));
    }

    @Test
    public void closeUnopenedIsNoOp() {
        ArtFramework.close("missing");
        assertTrue(ArtFramework.listOpenIds().isEmpty());
    }

    @Test
    public void handleCloseRemovesFromOpen() {
        ArtFramework.register(new WindowDef("w", WindowClass.SYNTHETIC, "layouts/demo.json"));
        WindowHandle h = ArtFramework.open("w");
        h.close();
        assertFalse(h.isOpen());
        assertNull(ArtFramework.find("w"));
        assertTrue(ArtFramework.listOpenIds().isEmpty());
    }

    @Test
    public void closeUsesEcsLifecycleWhenHandleCacheIsMissing() throws Exception {
        ArtFramework.register(new WindowDef("w", WindowClass.SYNTHETIC, "layouts/demo.json"));
        ArtFramework.open("w");
        java.lang.reflect.Field lifecycleField = ArtFramework.class.getDeclaredField("LIFECYCLE");
        lifecycleField.setAccessible(true);
        Object lifecycle = lifecycleField.get(null);
        java.lang.reflect.Field handlesField = lifecycle.getClass().getDeclaredField("handles");
        handlesField.setAccessible(true);
        ((java.util.Map<?, ?>) handlesField.get(lifecycle)).clear();

        ArtFramework.close("w");

        assertTrue(ArtFramework.listOpenIds().isEmpty());
    }

    @Test
    public void closeNativeUsesDefinitionIdWhenResourceIsEmptyAndHandleCacheIsMissing()
            throws Exception {
        ArtFramework.register(
                new WindowDef(NativeTemplateIds.MAP, WindowClass.NATIVE_TEMPLATE, ""));
        ArtFramework.bind(NativeTemplateIds.MAP);
        assertTrue(NativeTemplateRuntime.isMapBound());

        java.lang.reflect.Field lifecycleField = ArtFramework.class.getDeclaredField("LIFECYCLE");
        lifecycleField.setAccessible(true);
        Object lifecycle = lifecycleField.get(null);
        java.lang.reflect.Field handlesField = lifecycle.getClass().getDeclaredField("handles");
        handlesField.setAccessible(true);
        ((java.util.Map<?, ?>) handlesField.get(lifecycle)).clear();

        ArtFramework.unmount(NativeTemplateIds.MAP);

        assertFalse(NativeTemplateRuntime.isMapBound());
        assertTrue(ArtFramework.listOpenIds().isEmpty());
    }

    @Test
    public void resetForTestsClearsDefsAndOpen() {
        ArtFramework.register(new WindowDef("w", WindowClass.SYNTHETIC, "layouts/demo.json"));
        ArtFramework.open("w");
        ArtFramework.resetForTests();
        assertFalse(ArtFramework.isRegistered("w"));
        assertTrue(ArtFramework.listOpenIds().isEmpty());
        assertNull(ArtFramework.find("w"));
    }
}
