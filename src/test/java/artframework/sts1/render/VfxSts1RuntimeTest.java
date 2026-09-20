package artframework.sts1.render;

import artframework.core.PackSystems;
import artframework.ecs.ArtEcs;
import artframework.ecs.EntityId;
import artframework.ecs.EcsTick;
import artframework.sts1.PresentSafety;
import artframework.vfx.VfxLifecycleSystem;
import artframework.vfx.VfxDrawList;
import artframework.vfx.VfxDrawListComponent;
import artframework.vfx.VfxParticleDraw;
import artframework.vfx.VfxSceneRuntimeComponent;
import artframework.vfx.VfxTransformComponent;
import org.junit.After;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VfxSts1RuntimeTest {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    @After public void cleanup() {
        VfxSts1Runtime.resetForTests();
        ArtEcs.world().clear();
        PackSystems.resetForTests();
    }

    @Test
    public void rejectsBundleTraversalBeforeReading() throws Exception {
        Path root = Files.createTempDirectory("art-vfx");
        try {
            VfxSts1Runtime.resolve(root, "../outside.png");
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("traversal accepted");
    }

    @Test
    public void loadsIntoSharedWorldAndClearRemovesGraph() throws Exception {
        Path root = Files.createTempDirectory("art-vfx");
        Files.write(root.resolve("manifest.json"), ("{\"format\":\"art.sts2-vfx-bundle\",\"schemaVersion\":1,"
                + "\"bundleId\":\"b\",\"capability\":\"DEGRADED\",\"scenes\":[{\"id\":\"default\","
                + "\"path\":\"scene.json\",\"capability\":\"DEGRADED\"}],\"resources\":[]}").getBytes(UTF8));
        Files.write(root.resolve("scene.json"), ("{\"format\":\"art.sts2-vfx-scene\",\"schemaVersion\":1,"
                + "\"id\":\"default\",\"duration\":1,\"capability\":\"DEGRADED\",\"typedNodes\":[],\"resources\":[]}").getBytes(UTF8));
        VfxSts1Runtime.load(root.toString(), null);
        assertEquals(1, ArtEcs.world().entities().size());
        assertTrue(VfxSts1Runtime.statusLine().contains("status=loaded"));
        assertEquals(1, PackSystems.systemsFor(artframework.core.PackSystemPhase.EFFECTS).size());
        VfxSts1Runtime.clear();
        assertEquals(0, ArtEcs.world().entities().size());
        assertTrue(VfxSts1Runtime.statusLine().contains("status=clear"));
    }

    @Test
    public void runtimeOriginOverloadAndCompletionStatusUseSharedEcs() throws Exception {
        Path root = Files.createTempDirectory("art-vfx-origin");
        Files.write(root.resolve("manifest.json"), ("{\"format\":\"art.sts2-vfx-bundle\",\"schemaVersion\":1,"
                + "\"bundleId\":\"b\",\"capability\":\"DEGRADED\",\"scenes\":[{\"id\":\"default\","
                + "\"path\":\"scene.json\",\"capability\":\"DEGRADED\"}],\"resources\":[]}").getBytes(UTF8));
        Files.write(root.resolve("scene.json"), ("{\"format\":\"art.sts2-vfx-scene\",\"schemaVersion\":1,"
                + "\"id\":\"default\",\"duration\":0,\"capability\":\"DEGRADED\",\"typedNodes\":[],\"resources\":[]}").getBytes(UTF8));

        VfxSts1Runtime.load(root.toString(), null, 960f, 540f);
        EntityId rootEntity = ArtEcs.world().query(VfxSceneRuntimeComponent.class).get(0);
        VfxTransformComponent placement = ArtEcs.world().get(rootEntity, VfxTransformComponent.class);
        assertEquals(960f, placement.position.x, 0f);
        assertEquals(540f, placement.position.y, 0f);
        assertTrue(VfxSts1Runtime.statusLine().contains("liveRoots=1"));
        assertTrue(VfxSts1Runtime.statusLine().contains("draws=0"));

        new VfxLifecycleSystem().run(ArtEcs.world(), new EcsTick(0f, 1L));
        String completed = VfxSts1Runtime.statusLine();
        assertTrue(completed.contains("liveRoots=0"));
        assertTrue(completed.contains("draws=0"));
        assertTrue(completed.contains("completed=true"));
    }

    @Test
    public void liveDrawAdmissionIsFalseForEmptyAndStaleRoots() throws Exception {
        EntityId emptyRoot = ArtEcs.world().createEntity();
        ArtEcs.world().put(emptyRoot, VfxSceneRuntimeComponent.class,
                new VfxSceneRuntimeComponent("empty", 1L, 1L, false));
        ArtEcs.world().put(emptyRoot, VfxDrawListComponent.class,
                new VfxDrawListComponent(VfxDrawList.empty()));
        assertTrue(!VfxSts1Runtime.hasLiveDraws());

        ArtEcs.world().destroyEntity(emptyRoot);

        Path bundle = Files.createTempDirectory("art-vfx-stale");
        Files.write(bundle.resolve("manifest.json"), ("{\"format\":\"art.sts2-vfx-bundle\",\"schemaVersion\":1,"
                + "\"bundleId\":\"b\",\"capability\":\"DEGRADED\",\"scenes\":[{\"id\":\"default\","
                + "\"path\":\"scene.json\",\"capability\":\"DEGRADED\"}],\"resources\":[]}").getBytes(UTF8));
        Files.write(bundle.resolve("scene.json"), ("{\"format\":\"art.sts2-vfx-scene\",\"schemaVersion\":1,"
                + "\"id\":\"default\",\"duration\":1,\"capability\":\"DEGRADED\",\"typedNodes\":[],\"resources\":[]}").getBytes(UTF8));
        VfxSts1Runtime.load(bundle.toString(), null);
        EntityId staleRoot = ArtEcs.world().query(VfxSceneRuntimeComponent.class).get(0);
        ArtEcs.world().destroyEntity(staleRoot);
        assertTrue(!VfxSts1Runtime.hasLiveDraws());
    }

    @Test
    public void liveDrawAdmissionIsTrueForProjectedDrawOnLiveRoot() {
        EntityId root = ArtEcs.world().createEntity();
        ArtEcs.world().put(root, VfxSceneRuntimeComponent.class,
                new VfxSceneRuntimeComponent("live", 1L, 1L, false));
        VfxParticleDraw particle = new VfxParticleDraw("live", "p", 0, 0, "p.png",
                0f, 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f, "normal", 0f,
                0, 1, 1, false, false);
        ArtEcs.world().put(root, VfxDrawListComponent.class,
                new VfxDrawListComponent(new VfxDrawList(java.util.Collections.singletonList(particle))));

        assertTrue(VfxSts1Runtime.hasLiveDraws());
    }

    @Test
    public void hostRecreationClearsLoadedGraphAndHostResources() throws Exception {
        Path root = Files.createTempDirectory("art-vfx-recreate");
        Files.write(root.resolve("manifest.json"), ("{\"format\":\"art.sts2-vfx-bundle\",\"schemaVersion\":1,"
                + "\"bundleId\":\"b\",\"capability\":\"DEGRADED\",\"scenes\":[{\"id\":\"default\","
                + "\"path\":\"scene.json\",\"capability\":\"DEGRADED\"}],\"resources\":[]}").getBytes(UTF8));
        Files.write(root.resolve("scene.json"), ("{\"format\":\"art.sts2-vfx-scene\",\"schemaVersion\":1,"
                + "\"id\":\"default\",\"duration\":1,\"capability\":\"DEGRADED\",\"typedNodes\":[],\"resources\":[]}").getBytes(UTF8));
        VfxSts1Runtime.load(root.toString(), null);

        PresentSafety.onHostRecreated();

        assertTrue(VfxSts1Runtime.statusLine().contains("status=clear"));
        assertEquals(0, ArtEcs.world().entities().size());
    }
}
