package artframework.sts1.render;

import artframework.api.ArtFramework;
import org.junit.After;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Sts1RenderBoundaryTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void postRenderHostDoesNotClaimPreNativeBackgroundSupport() {
        assertEquals(Sts1RenderBoundary.BackgroundCapability.UNSUPPORTED,
                Sts1RenderBoundary.backgroundCapability());
        assertEquals("stage.draw", Sts1RenderBoundary.nativeInterval());
        assertEquals("post_native_overlay", Sts1RenderBoundary.artSubmissionInterval());
    }

    @Test
    public void probeSliceReportsConfiguredBoundaryWithoutPreNativeClaim() {
        Map<String, Object> slice = Sts1RenderBoundary.probeSlice();
        assertEquals(4, slice.size());
        assertEquals("stage.draw", slice.get("nativeInterval"));
        assertEquals("post_native_overlay", slice.get("artInterval"));
        assertEquals("unsupported", slice.get("backgroundCapability"));
        assertEquals(Boolean.FALSE, slice.get("supportsPreNativeBackground"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void uiProbeBackendExposesRenderBoundary() {
        Map<String, Object> snap = ArtFramework.probe().asMap();
        Map<String, Object> backend = (Map<String, Object>) snap.get("backend");
        assertTrue(backend.containsKey("renderBoundary"));
        Map<String, Object> boundary = (Map<String, Object>) backend.get("renderBoundary");
        assertEquals("stage.draw", boundary.get("nativeInterval"));
        assertEquals("post_native_overlay", boundary.get("artInterval"));
        assertEquals("unsupported", boundary.get("backgroundCapability"));
        assertEquals(Boolean.FALSE, boundary.get("supportsPreNativeBackground"));
        assertFalse(boundary.containsKey("preNativeBackground"));
    }
}
