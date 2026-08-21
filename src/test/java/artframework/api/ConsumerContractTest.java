package artframework.api;

import org.junit.Test;
import artframework.c1.SyntheticRuntime;
import artframework.c2.EntityPresent;
import artframework.component.NativeTemplateIds;
import artframework.c2.NativeTemplateRuntime;
import artframework.core.SignalDispatchResult;
import artframework.core.UiSignal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Locks the public surface downstream mods are expected to compile against.
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
        assertEquals("sts1.map", NativeTemplateIds.MAP);
        assertEquals("sts1.event", NativeTemplateIds.EVENT);
        assertEquals("sts1.select.grid", NativeTemplateIds.SELECT_GRID);
        assertEquals("sts1.select.hand", NativeTemplateIds.SELECT_HAND);
        assertEquals("sts1.endturn", NativeTemplateIds.END_TURN);
    }

    @Test
    public void documentedFacadeCompatibilitySurfaceRemainsCallable() {
        assertEquals("artframework", UiProbe.MOD_ID);
        assertTrue(UiProbe.SCHEMA_VERSION > 0);
        assertNotNull(ArtFramework.ops());
        assertNotNull(ArtFramework.probe());
        assertNotNull(ArtFramework.assets());
        assertNotNull(ArtFramework.entities());
        assertNotNull(ArtFramework.entityPresent());

        SignalDispatchResult emitted = ArtFramework.emit(
                new UiSignal("unbound/test", "consumer", null));
        SignalDispatchResult dispatched = ArtFramework.dispatch(
                new UiSignal("unbound/test", "consumer", null));
        assertNotNull(emitted);
        assertNotNull(dispatched);
    }
}
