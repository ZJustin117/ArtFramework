package artframework.sts1.skeleton;

import org.junit.After;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Sts1SpineProviderTest {

    @After
    public void tearDown() {
        artframework.api.ArtFramework.resetForTests();
    }

    @Test
    public void spine34ProviderHasStableId() {
        assertEquals("spine34", new Sts1Spine34Provider().id());
    }

    @Test
    public void spine42ProviderDetectsMissingRuntime() {
        Sts1Spine42Provider provider = new Sts1Spine42Provider("missing.ShadedSkeleton");

        assertEquals("spine42", provider.id());
        assertFalse(provider.isAvailable());
        assertTrue(provider.unavailableReason().contains("ClassNotFoundException"));
    }

    @Test
    public void bridgeProbeReportsSpine42Availability() {
        Sts1Spine42Provider provider = new Sts1Spine42Provider("missing.ShadedSkeleton");
        artframework.api.ArtFramework.skeletons().register(provider);
        Sts1SkeletonBridge.setProviderId(Sts1Spine42Provider.ID);

        Map<String, Object> probe = Sts1SkeletonBridge.probeSlice();

        assertEquals(Boolean.TRUE, probe.get("commandProvider"));
        assertEquals(Boolean.FALSE, probe.get("spine42Available"));
        assertEquals("missing.ShadedSkeleton", probe.get("spine42RuntimeClass"));
    }

    @Test(expected = IllegalStateException.class)
    public void spine42LoadFailsClearlyWhenRuntimeMissing() {
        new Sts1Spine42Provider("missing.ShadedSkeleton").load(null);
    }
}
