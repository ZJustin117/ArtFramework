package artframework.sts1.skeleton;

import org.junit.After;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

    @Test
    public void bridgeProbeReportsZeroDrawEvidenceWhenDeveloperHandleIsAbsent() {
        Sts1Spine42Provider provider = new Sts1Spine42Provider("missing.ShadedSkeleton");
        artframework.api.ArtFramework.skeletons().register(provider);
        artframework.sts1.skeleton.Sts1SkeletonBridge.setProviderId(Sts1Spine42Provider.ID);

        Map<String, Object> evidence = (Map<String, Object>)
                Sts1SkeletonBridge.probeSlice().get("drawEvidence");

        assertEquals("d1_ironclad", evidence.get("handle"));
        assertEquals(Integer.valueOf(0), evidence.get("count"));
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

    @Test
    public void meshTrianglesExpandToLegacyBatchQuads() {
        float[] world = {10f, 11f, 20f, 21f, 30f, 31f, 40f, 41f};
        float[] uvs = {.1f, .2f, .3f, .4f, .5f, .6f, .7f, .8f};

        float[] vertices = Sts1Spine42Provider.meshTriangleVertices(world, uvs,
                new short[] {2, 0, 1}, 0, 9f);

        assertEquals(20, vertices.length);
        assertEquals(30f, vertices[0], 0f);
        assertEquals(.5f, vertices[3], 0f);
        assertEquals(10f, vertices[5], 0f);
        assertEquals(.1f, vertices[8], 0f);
        assertEquals(20f, vertices[10], 0f);
        assertEquals(.3f, vertices[13], 0f);
        assertEquals(vertices[10], vertices[15], 0f);
        assertEquals(vertices[13], vertices[18], 0f);
    }

    @Test
    public void cpuParityReportCapturesRegionAndMeshSuccess() {
        // Spine source order: BR, BL, UL, UR. The provider expands this to Batch order.
        float[] regionWorld = {1f, 0f, 0f, 0f, 0f, 1f, 1f, 1f};
        float[] regionUvs = {.1f, .2f, .3f, .4f, .5f, .6f, .7f, .8f};

        Spine42Parity.ParityResult region = Spine42Parity.region(regionWorld, regionUvs, 9f);
        region.assertPassed();
        Spine42Parity.ParityResult regionComparison = Spine42Parity.compareCoverage(
                Spine42Parity.Kind.REGION, region.coverageVertices, region.coverageVertices, 0f);
        regionComparison.assertPassed();
        assertEquals("REGION", region.kind.name());
        assertEquals("OK", region.reason.name());
        assertEquals(Integer.valueOf(4), region.toMap().get("coverageVertexCount"));

        float[] meshWorld = {0f, 0f, 2f, 0f, 0f, 2f};
        float[] meshUvs = {0f, 0f, 1f, 0f, 0f, 1f};
        Spine42Parity.ParityResult mesh = Spine42Parity.mesh(meshWorld, meshUvs, new short[] {0, 1, 2}, 7f);
        mesh.assertPassed();
        Spine42Parity.ParityResult meshComparison = Spine42Parity.compareCoverage(
                Spine42Parity.Kind.MESH, mesh.coverageVertices, mesh.coverageVertices, 0f);
        meshComparison.assertPassed();
        assertEquals("MESH", mesh.kind.name());
        assertEquals(Integer.valueOf(4), mesh.toMap().get("coverageVertexCount"));
    }

    @Test
    public void cpuParityReportRejectsClockwiseAndDegenerateGeometry() {
        Spine42Parity.ParityResult validRegion = Spine42Parity.region(
                new float[] {1f, 0f, 0f, 0f, 0f, 1f, 1f, 1f},
                new float[] {.1f, .2f, .3f, .4f, .5f, .6f, .7f, .8f}, 9f);
        assertTrue(validRegion.passed);
        assertEquals("CLOCKWISE", Spine42Parity.orientation(
                validRegion.coverageVertices, 0).name());

        Spine42Parity.ParityResult degenerateRegion = Spine42Parity.region(
                new float[] {10f, 11f, 10f, 11f, 30f, 31f, 40f, 41f},
                new float[] {.1f, .2f, .3f, .4f, .5f, .6f, .7f, .8f}, 9f);
        assertFalse(degenerateRegion.passed);
        assertEquals("DEGENERATE", degenerateRegion.reason.name());
        assertTrue(degenerateRegion.message.contains("degenerate"));

        Spine42Parity.ParityResult clockwiseMesh = Spine42Parity.mesh(
                new float[] {0f, 0f, 2f, 0f, 0f, 2f},
                new float[] {0f, 0f, 1f, 0f, 0f, 1f},
                new short[] {0, 2, 1}, 7f);
        assertFalse(clockwiseMesh.passed);
        assertEquals("WINDING", clockwiseMesh.reason.name());
        assertTrue(clockwiseMesh.message.contains("clockwise"));
    }

    @Test
    public void cpuParityReportRejectsInvalidAndMalformedMeshData() {
        Spine42Parity.ParityResult invalidIndex = Spine42Parity.mesh(
                new float[] {0f, 0f, 2f, 0f, 0f, 2f},
                new float[] {0f, 0f, 1f, 0f, 0f, 1f},
                new short[] {0, 1, 3}, 7f);
        assertFalse(invalidIndex.passed);
        assertEquals("INVALID_DATA", invalidIndex.reason.name());
        assertTrue(invalidIndex.message.contains("outside world vertices"));

        Spine42Parity.ParityResult malformed = Spine42Parity.mesh(
                new float[] {0f, 0f, 2f, 0f},
                new float[] {0f, 0f},
                new short[] {0, 1, 2}, 7f);
        assertFalse(malformed.passed);
        assertEquals("INVALID_DATA", malformed.reason.name());
        assertTrue(malformed.message.contains("UV count"));

        Spine42Parity.ParityResult empty = Spine42Parity.mesh(
                new float[] {0f, 0f, 2f, 0f, 0f, 2f},
                new float[] {0f, 0f, 1f, 0f, 0f, 1f},
                new short[0], 7f);
        assertFalse(empty.passed);
        assertEquals("DEGENERATE", empty.reason.name());
        assertTrue(empty.message.contains("no triangles"));
    }

    @Test
    public void cpuParityReportDiagnosesExpectedCoverageMismatch() {
        Spine42Parity.ParityResult result = Spine42Parity.compareCoverage(
                Spine42Parity.Kind.MESH, new float[] {0f, 0f}, new float[] {0f, 1f}, .001f);

        assertFalse(result.passed);
        assertEquals("MISMATCH", result.reason.name());
        assertEquals(Integer.valueOf(1), result.triangleIndex);
        assertTrue(result.message.contains("index 1"));
    }

    @Test
    public void parityAssertionSurfacesReadableFailureText() {
        Spine42Parity.ParityResult failed = Spine42Parity.region(
                new float[] {10f, 11f, 10f, 11f, 30f, 31f, 40f, 41f},
                new float[] {.1f, .2f, .3f, .4f, .5f, .6f, .7f, .8f}, 9f);

        try {
            failed.assertPassed();
            fail("expected assertion");
        } catch (AssertionError error) {
            assertTrue(error.getMessage().contains("REGION parity failed"));
            assertTrue(error.getMessage().contains("DEGENERATE"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void malformedMeshIndexIsRejectedForFailOpenCaller() {
        Sts1Spine42Provider.meshTriangleVertices(new float[] {0f, 0f, 1f, 1f},
                new float[] {0f, 0f, 1f, 1f}, new short[] {0, 1, 2}, 0, 1f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void legacyBatchVerticesRejectNonFinitePreparedData() {
        Sts1Spine42Provider.validateBatchVertices(new float[] {
                0f, 0f, 1f, 0f, 0f, 0f, Float.NaN, 0f, 0f, 0f,
                1f, 1f, 1f, 0f, 0f, 1f, 0f, 0f, 0f, 1f
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void meshPreflightRejectsLaterMalformedTriangleData() throws Exception {
        java.lang.reflect.Method validate = Sts1Spine42Provider.class
                .getDeclaredMethod("validateMeshData", int.class, float[].class, short[].class);
        validate.setAccessible(true);
        try {
            validate.invoke(null, 6, new float[] {0f, 0f, 1f, 0f, 0f, 1f},
                    new short[] {0, 1, 2, 0, 2, 3});
        } catch (java.lang.reflect.InvocationTargetException error) {
            throw (Exception) error.getCause();
        }
    }

    @Test
    public void clippingCapabilityIsExplicitlyUnsupportedWithoutHostClipState() {
        Sts1Spine42Provider provider = new Sts1Spine42Provider("missing.ShadedSkeleton");

        assertFalse(provider.supportsClipping());
        assertTrue(Sts1Spine42Provider.isClippingAttachment(ClippingFixture.class, ClippingFixture.class));
        assertFalse(Sts1Spine42Provider.isClippingAttachment(AttachmentFixture.class, ClippingFixture.class));
    }

    @Test
    public void legacyBatchDoesNotClaimTwoColorVertexCapability() {
        Sts1Spine42Provider provider = new Sts1Spine42Provider("missing.ShadedSkeleton");

        assertFalse(provider.supportsTwoColor(new LegacyBatchFixture()));
    }

    @Test
    public void twoColorDetectionUsesDarkColorPresence() {
        assertFalse(Sts1Spine42Provider.containsUnsupportedTwoColor(
                new Object[] {new PlainSlotFixture()}, 1));
        assertTrue(Sts1Spine42Provider.containsUnsupportedTwoColor(
                new Object[] {new TwoColorSlotFixture()}, 1));
    }

    @Test
    public void twoColorDetectionAlsoChecksAttachmentColor() {
        assertTrue(Sts1Spine42Provider.containsUnsupportedTwoColor(
                new Object[] {new AttachmentColorSlotFixture()}, 1));
    }

    @Test
    public void cpuParityAcceptsBatchRegionWinding() {
        Spine42Parity.ParityResult result = Spine42Parity.region(
                new float[] {1f, 0f, 0f, 0f, 0f, 1f, 1f, 1f},
                new float[] {0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f}, 1f);

        assertTrue(result.passed);
        assertEquals(Spine42Parity.Reason.OK, result.reason);
    }

    private static class AttachmentFixture {}
    private static class ClippingFixture extends AttachmentFixture {}

    private static class LegacyBatchFixture {}

    public static class PlainSlotFixture {
        public Object getAttachment() { return null; }
    }

    public static class TwoColorSlotFixture {
        public Object getDarkColor() { return new Object(); }
        public Object getAttachment() { return null; }
    }

    public static class AttachmentColorSlotFixture {
        public Object getAttachment() { return new TwoColorAttachmentFixture(); }
    }

    public static class TwoColorAttachmentFixture {
        public Object getDarkColor() { return new Object(); }
    }
}
