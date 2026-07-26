package spireui.api;

import org.junit.Test;
import spireui.c1.SyntheticRuntime;
import spireui.c2.EntityPresent;
import spireui.c2.NativeTemplateIds;
import spireui.c2.NativeTemplateRuntime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Locks the public surface consumers (e.g. CrossSpire) are expected to compile against.
 */
public class ConsumerContractTest {

    @Test
    public void facadeAndRuntimesResolvable() {
        assertNotNull(SpireUI.class);
        assertNotNull(WindowDef.class);
        assertNotNull(WindowClass.SYNTHETIC);
        assertNotNull(WindowClass.NATIVE_TEMPLATE);
        assertNotNull(WindowHandle.class);
        assertTrue(SyntheticRuntime.isAvailable());
        assertTrue(NativeTemplateRuntime.isAvailable());
        EntityPresent entities = SpireUI.entities();
        assertNotNull(entities);
        assertEquals("sts.map", NativeTemplateIds.MAP);
        assertEquals("sts.event", NativeTemplateIds.EVENT);
        assertEquals("sts.select.grid", NativeTemplateIds.SELECT_GRID);
        assertEquals("sts.select.hand", NativeTemplateIds.SELECT_HAND);
        assertEquals("sts.endturn", NativeTemplateIds.END_TURN);
    }
}
