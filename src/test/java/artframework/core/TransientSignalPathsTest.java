package artframework.core;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TransientSignalPathsTest {
    @Test
    public void routesAreStableAndDistinct() {
        String[] routes = {
                TransientSignalPaths.SURFACE_LIFECYCLE,
                TransientSignalPaths.SURFACE_INTENT,
                TransientSignalPaths.NATIVE_INTENT_LIFECYCLE,
                TransientSignalPaths.AUTHORITY_FRAME,
                TransientSignalPaths.AUTHORITY_BUSINESS_CONFIRMATION
        };

        assertEquals(5, new HashSet<String>(Arrays.asList(routes)).size());
        assertEquals("transient/surface/lifecycle", TransientSignalPaths.SURFACE_LIFECYCLE);
        assertEquals("transient/surface/intent", TransientSignalPaths.SURFACE_INTENT);
        assertEquals("transient/native/intent_lifecycle",
                TransientSignalPaths.NATIVE_INTENT_LIFECYCLE);
        assertEquals("transient/authority/frame", TransientSignalPaths.AUTHORITY_FRAME);
        assertEquals("transient/authority/business_confirmation",
                TransientSignalPaths.AUTHORITY_BUSINESS_CONFIRMATION);
        for (String route : routes) assertTrue(route.startsWith("transient/"));
    }
}
