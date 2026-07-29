package artframework.c2;

import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.core.SignalDecision;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MapTemplateTest {
    @After public void tearDown() { ArtFramework.resetForTests(); }
    @Test public void bindActivatesMapTemplate() {
        ArtFramework.register(new WindowDef(NativeTemplateIds.MAP, WindowClass.NATIVE_TEMPLATE, NativeTemplateIds.MAP));
        ArtFramework.bind(NativeTemplateIds.MAP);
        assertTrue(NativeTemplateRuntime.isMapBound());
        ArtFramework.close(NativeTemplateIds.MAP);
        assertFalse(NativeTemplateRuntime.isMapBound());
    }
    @Test public void mapClickPolicyUsesSignalBusInOrder() {
        ArtFramework.register(new WindowDef(NativeTemplateIds.MAP, WindowClass.NATIVE_TEMPLATE, NativeTemplateIds.MAP));
        ArtFramework.bind(NativeTemplateIds.MAP);
        AtomicInteger hits = new AtomicInteger();
        ArtFramework.connect("ui/" + NativeTemplateIds.MAP + "/node_clicked", signal -> { hits.incrementAndGet(); return SignalDecision.continueSignal(); });
        ArtFramework.connect("ui/" + NativeTemplateIds.MAP + "/node_clicked", signal -> { hits.incrementAndGet(); return SignalDecision.stopRejected("blocked"); });
        assertFalse(
                artframework.c2.hooks.HostPatchResults.allowsNative(
                        artframework.c2.hooks.NativeUiHooks.onMapNodeClick(2, 1, "monster")));
        assertEquals(2, hits.get());
    }
}
