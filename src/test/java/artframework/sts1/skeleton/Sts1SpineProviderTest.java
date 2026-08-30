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

    @Test
    public void regionVerticesUseBatchOrderAndPreserveUvs() {
        float[] world = {10f, 11f, 20f, 21f, 30f, 31f, 40f, 41f};
        float[] uvs = {.1f, .2f, .3f, .4f, .5f, .6f, .7f, .8f};

        float[] vertices = Sts1Spine42Provider.regionVertices(world, uvs, 9f);

        assertEquals(20, vertices.length);
        // Batch order is BL, TL, TR, BR; Spine's source order is BR, BL, UL, UR.
        assertEquals(20f, vertices[0], 0f);
        assertEquals(21f, vertices[1], 0f);
        assertEquals(.3f, vertices[3], 0f);
        assertEquals(30f, vertices[5], 0f);
        assertEquals(40f, vertices[10], 0f);
        assertEquals(.7f, vertices[13], 0f);
        assertEquals(10f, vertices[15], 0f);
        assertEquals(.1f, vertices[18], 0f);
        assertEquals(9f, vertices[2], 0f);
    }
}
