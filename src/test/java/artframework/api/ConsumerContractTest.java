package artframework.api;

import org.junit.Test;
import artframework.c1.SyntheticRuntime;
import artframework.c2.EntityPresent;
import artframework.c2.NativeTemplateIds;
import artframework.c2.NativeTemplateRuntime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Locks the public surface consumers (e.g. CrossSpire) are expected to compile against.
 */
public class ConsumerContractTest {

    @Test
    public void facadeAndRuntimesResolvable() {
        assertNotNull(ArtFramework.class);
        assertNotNull(WindowDef.class);
        assertNotNull(WindowClass.SYNTHETIC);
        assertNotNull(WindowClass.NATIVE_TEMPLATE);
        assertNotNull(WindowHandle.class);
        assertTrue(SyntheticRuntime.isAvailable());
        assertTrue(NativeTemplateRuntime.isAvailable());
        EntityPresent entities = ArtFramework.entities();
        assertNotNull(entities);
        assertEquals("sts.map", NativeTemplateIds.MAP);
        assertEquals("sts.event", NativeTemplateIds.EVENT);
        assertEquals("sts.select.grid", NativeTemplateIds.SELECT_GRID);
        assertEquals("sts.select.hand", NativeTemplateIds.SELECT_HAND);
        assertEquals("sts.endturn", NativeTemplateIds.END_TURN);
    }
}
