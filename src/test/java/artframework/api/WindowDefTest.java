package artframework.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WindowDefTest {

    @Test(expected = IllegalArgumentException.class)
    public void nullIdThrows() {
        new WindowDef(null, WindowClass.SYNTHETIC, "x");
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyIdThrows() {
        new WindowDef("", WindowClass.SYNTHETIC, "x");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullWindowClassThrows() {
        new WindowDef("id", null, "x");
    }

    @Test
    public void nullResourceBecomesEmpty() {
        WindowDef def = new WindowDef("id", WindowClass.NATIVE_TEMPLATE, null);
        assertEquals("id", def.id);
        assertEquals(WindowClass.NATIVE_TEMPLATE, def.windowClass);
        assertEquals("", def.resource);
    }
}
