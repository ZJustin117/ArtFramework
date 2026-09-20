package artframework.vfx;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class VfxManifestLoaderTest {
    private final VfxManifestLoader loader = new VfxManifestLoader();

    @Test
    public void loadsManifestSceneDurationEmitterAndResourceOrder() {
        VfxBundleDefinition bundle = loader.parseManifest("{\"format\":\"art.sts2-vfx-bundle\",\"schemaVersion\":1,"
                + "\"bundleId\":\"demo\",\"capability\":\"DEGRADED\",\"scenes\":[{\"id\":\"s\",\"path\":\"scenes/s.json\",\"capability\":\"DEGRADED\"}],"
                + "\"resources\":[{\"id\":\"b\",\"kind\":\"TEXTURE\",\"sourcePath\":\"b.png\",\"status\":\"missing-resource\"},{\"id\":\"a\",\"kind\":\"TEXTURE\",\"sourcePath\":\"a.png\",\"outputPath\":\"resources/a.png\",\"status\":\"supported\"}]}" );
        assertEquals("b", bundle.resources.get(0).resourceId);
        assertEquals("a", bundle.resources.get(1).resourceId);
        assertNull(bundle.resources.get(0).outputPath);
        assertEquals("resources/a.png", bundle.resources.get(1).outputPath);

        VfxSceneDefinition scene = loader.parseScene("{\"format\":\"art.sts2-vfx-scene\",\"schemaVersion\":1,\"id\":\"s\",\"duration\":1.25,\"capability\":\"DEGRADED\","
                + "\"typedNodes\":[{\"id\":\"n\",\"nodePath\":\"Root\",\"parentId\":null,\"nodeType\":\"GPUParticles2D\",\"transform\":{\"position\":{\"type\":\"Vec2\",\"x\":2,\"y\":3}},"
                + "\"particleEmitter\":{\"amount\":16,\"lifetime\":1.25,\"lifetimeRandomness\":0.2,\"direction\":{\"type\":\"Vec3\",\"x\":1,\"y\":-1,\"z\":0},\"spreadDegrees\":45,\"initialVelocity\":{\"min\":20,\"max\":40},\"randomSeed\":7}}],\"resources\":[]}" );
        assertEquals(1.25f, scene.duration, 0.001f);
        assertEquals(16, scene.nodes.get(0).particleEmitter.amount.intValue());
        assertEquals(40f, scene.nodes.get(0).particleEmitter.initialVelocity.max, 0.001f);
        assertEquals(1f, scene.nodes.get(0).particleEmitter.direction.x, 0.001f);
    }

    @Test
    public void loadsConverterTypedExtResourceTextureReference() {
        VfxSceneDefinition scene = loader.parseScene("{\"format\":\"art.sts2-vfx-scene\",\"schemaVersion\":1,\"id\":\"s\",\"duration\":0,\"capability\":\"DEGRADED\","
                + "\"typedNodes\":[{\"id\":\"n\",\"nodePath\":\"Root\",\"nodeType\":\"GPUParticles2D\",\"particleEmitter\":{\"texture\":{\"ref\":\"ExtResource\",\"id\":\"2_sr53b\"}}}],\"resources\":[]}");
        assertEquals("2_sr53b", scene.nodes.get(0).particleEmitter.textureResourceId);
    }

    @Test
    public void loadsIntegerExtResourceTextureReference() {
        VfxSceneDefinition scene = loader.parseScene("{\"format\":\"art.sts2-vfx-scene\",\"schemaVersion\":1,\"id\":\"s\",\"duration\":0,\"capability\":\"DEGRADED\","
                + "\"typedNodes\":[{\"id\":\"n\",\"nodePath\":\"Root\",\"nodeType\":\"GPUParticles2D\",\"particleEmitter\":{\"texture\":{\"ref\":\"ExtResource\",\"id\":2}}}],\"resources\":[]}");
        assertEquals("2", scene.nodes.get(0).particleEmitter.textureResourceId);
    }

    @Test
    public void rejectsMalformedTextureReferences() {
        assertRejects("{\"ref\":\"SubResource\",\"id\":\"2_sr53b\"}");
        assertRejects("{\"ref\":\"Unknown\",\"id\":\"2_sr53b\"}");
        assertRejects("{\"ref\":\"ExtResource\"}");
        assertRejects("{\"ref\":\"ExtResource\",\"id\":\"\"}");
        assertRejects("{\"ref\":\"ExtResource\",\"id\":1.5}");
        assertRejects("\"2_sr53b\"");
    }

    private void assertRejects(String textureJson) {
        try {
            loader.parseScene("{\"format\":\"art.sts2-vfx-scene\",\"schemaVersion\":1,\"id\":\"s\",\"duration\":0,\"capability\":\"DEGRADED\","
                    + "\"typedNodes\":[{\"id\":\"n\",\"nodePath\":\"Root\",\"nodeType\":\"GPUParticles2D\",\"particleEmitter\":{\"texture\":"
                    + textureJson + "}}],\"resources\":[]}");
            fail("expected malformed texture reference to be rejected: " + textureJson);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsWrongFormat() {
        loader.parseManifest("{\"format\":\"wrong\",\"schemaVersion\":1,\"bundleId\":\"x\",\"capability\":\"SUPPORTED\",\"scenes\":[],\"resources\":[]}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSchemaAndTraversal() {
        loader.parseManifest("{\"format\":\"art.sts2-vfx-bundle\",\"schemaVersion\":2,\"bundleId\":\"x\",\"capability\":\"SUPPORTED\",\"scenes\":[{\"id\":\"x\",\"path\":\"../x.json\",\"capability\":\"SUPPORTED\"}],\"resources\":[]}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidNumericType() {
        loader.parseScene("{\"format\":\"art.sts2-vfx-scene\",\"schemaVersion\":1,\"id\":\"s\",\"duration\":\"bad\",\"capability\":\"SUPPORTED\",\"typedNodes\":[],\"resources\":[]}");
    }

    @Test
    public void missingResourcesRemainRepresentable() {
        VfxSceneDefinition scene = loader.parseScene("{\"format\":\"art.sts2-vfx-scene\",\"schemaVersion\":1,\"id\":\"s\",\"duration\":0,\"capability\":\"DEGRADED\",\"typedNodes\":[],\"resources\":[{\"id\":\"missing\",\"kind\":\"TEXTURE\",\"sourcePath\":\"missing.png\",\"status\":\"missing-resource\"}]}");
        assertNotNull(scene.resources.get(0));
        assertNull(scene.resources.get(0).outputPath);
    }

    @Test
    public void selectsRequestedDefaultOrSoleSceneDeterministically() {
        VfxBundleDefinition bundle = new VfxBundleDefinition("b", 1, VfxCapability.DEGRADED,
                java.util.Arrays.asList(
                        new VfxBundleDefinition.VfxSceneEntry("other", "other.json", VfxCapability.DEGRADED),
                        new VfxBundleDefinition.VfxSceneEntry("default", "default.json", VfxCapability.DEGRADED)),
                java.util.Collections.<VfxResourceRef>emptyList());
        assertEquals("other", loader.selectScene(bundle, "other").id);
        assertEquals("default", loader.selectScene(bundle, null).id);
    }

    @Test(expected = IllegalArgumentException.class)
    public void requiresSceneForAmbiguousBundleWithoutDefault() {
        VfxBundleDefinition bundle = new VfxBundleDefinition("b", 1, VfxCapability.DEGRADED,
                java.util.Arrays.asList(
                        new VfxBundleDefinition.VfxSceneEntry("a", "a.json", VfxCapability.DEGRADED),
                        new VfxBundleDefinition.VfxSceneEntry("b", "b.json", VfxCapability.DEGRADED)),
                java.util.Collections.<VfxResourceRef>emptyList());
        loader.selectScene(bundle, null);
    }
}
