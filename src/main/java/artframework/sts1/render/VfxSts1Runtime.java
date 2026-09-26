package artframework.sts1.render;

import artframework.ecs.ArtEcs;
import artframework.ecs.EntityId;
import artframework.render.ArtRenderContributionComponent;
import artframework.render.ArtRenderFrame;
import artframework.render.ArtRenderFrameAggregationSystem;
import artframework.vfx.ParticleRenderProjectionSystem;
import artframework.vfx.VfxBundleDefinition;
import artframework.vfx.VfxDrawListComponent;
import artframework.vfx.VfxInstance;
import artframework.vfx.VfxInstantiateSystem;
import artframework.vfx.VfxManifestLoader;
import artframework.vfx.VfxSceneDefinition;
import artframework.vfx.VfxSceneResourcesComponent;
import artframework.vfx.VfxSceneRuntimeComponent;
import artframework.vfx.VfxSystems;
import artframework.vfx.VfxTransformComponent;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Developer-only STS1 host controller for converter-produced ART VFX bundles. */
public final class VfxSts1Runtime {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final VfxManifestLoader LOADER = new VfxManifestLoader();
    private static final List<EntityId> roots = new ArrayList<EntityId>();
    private static final TextureCache textures = new TextureCache();
    private static Path bundleRoot;
    private static String sceneId;
    private static String lastError;
    private static long epoch;

    private VfxSts1Runtime() {}

    public static synchronized void load(String directory, String requestedScene) throws IOException {
        load(directory, requestedScene, 0f, 0f);
    }

    public static synchronized void load(String directory, String requestedScene,
            float originX, float originY) throws IOException {
        Path root = safeRoot(directory);
        String manifestJson = new String(Files.readAllBytes(root.resolve("manifest.json")), UTF8);
        VfxBundleDefinition bundle = LOADER.load(manifestJson, new VfxManifestLoader.SceneReader() {
            @Override public String read(String relativePath) {
                try { return new String(Files.readAllBytes(resolve(root, relativePath)), UTF8); }
                catch (IOException e) { return null; }
            }
        });
        VfxBundleDefinition.VfxSceneEntry entry = LOADER.selectScene(bundle, requestedScene);
        VfxSceneDefinition scene = LOADER.parseScene(new String(Files.readAllBytes(resolve(root, entry.path)), UTF8));
        clear();
        VfxInstance instance = new VfxInstantiateSystem().instantiate(ArtEcs.world(), scene, ++epoch,
                originX, originY);
        roots.add(instance.rootEntity);
        VfxSystems.enable();
        bundleRoot = root;
        sceneId = scene.id;
        lastError = null;
    }

    public static synchronized void clear() {
        for (EntityId root : new ArrayList<EntityId>(roots)) {
            if (!ArtEcs.world().contains(root)) continue;
            VfxSceneRuntimeComponent runtime = ArtEcs.world().get(root, VfxSceneRuntimeComponent.class);
            if (runtime != null) {
                ArtRenderContributionComponent.clear(ArtEcs.world(),
                        ParticleRenderProjectionSystem.producerId(runtime));
            }
            destroyGraph(root);
        }
        roots.clear();
        textures.clear();
        bundleRoot = null;
        sceneId = null;
        lastError = null;
    }

    public static synchronized String statusLine() {
        List<EntityId> liveRoots = ArtEcs.world().query(VfxSceneRuntimeComponent.class);
        pruneRoots(liveRoots);
        int draws = 0;
        for (EntityId root : liveRoots) {
            VfxDrawListComponent draw = ArtEcs.world().get(root, VfxDrawListComponent.class);
            if (draw != null && draw.value != null) draws += draw.value.draws.size();
        }
        boolean completed = sceneId != null && liveRoots.isEmpty();
        String status = completed ? "completed" : (sceneId == null ? "clear" : "loaded");
        return "ART_VFX status=" + status
                + " scene=" + (sceneId == null ? "" : sceneId)
                + " liveRoots=" + liveRoots.size()
                + " draws=" + draws
                + " completed=" + completed
                + (lastError == null ? "" : " error=" + lastError);
    }

    /** Returns whether a live shared-ECS VFX root currently projects at least one draw. */
    public static synchronized boolean hasLiveDraws() {
        List<EntityId> liveRoots = ArtEcs.world().query(VfxSceneRuntimeComponent.class);
        pruneRoots(liveRoots);
        for (EntityId root : liveRoots) {
            VfxDrawListComponent draw = ArtEcs.world().get(root, VfxDrawListComponent.class);
            if (draw != null && draw.value != null && !draw.value.draws.isEmpty()) return true;
        }
        return false;
    }

    public static synchronized void recordError(Throwable error) {
        lastError = error == null ? "unknown" : error.getClass().getSimpleName() + ":" + String.valueOf(error.getMessage());
    }

    public static synchronized void render(SpriteBatch batch) {
        if (batch == null || bundleRoot == null) return;
        try {
            ArtRenderFrame frame = ArtRenderFrameAggregationSystem.frameFor(ArtEcs.world());
            if (frame == null || frame.isEmpty()) return;
            // Consume the shared aggregate family-by-family: each VFX root owns exactly one
            // producer contribution, so per-root segments are recovered via entriesFor(producerId).
            // This preserves the legacy per-root submission boundaries while the shared frame stays
            // the single ordering authority.
            for (EntityId root : ArtEcs.world().query(VfxSceneRuntimeComponent.class,
                    VfxSceneResourcesComponent.class)) {
                VfxSceneRuntimeComponent runtime =
                        ArtEcs.world().get(root, VfxSceneRuntimeComponent.class);
                if (runtime == null) continue;
                List<artframework.render.RenderPlan.Entry> entries =
                        frame.entriesFor(ParticleRenderProjectionSystem.producerId(runtime));
                Sts1VfxOverlayRenderer.render(batch, entries,
                        new Sts1VfxOverlayRenderer.TextureResolver() {
                            @Override public Texture resolve(String path) { return textures.resolve(bundleRoot, path); }
                        });
            }
        } catch (Throwable error) { recordError(error); }
    }

    private static void pruneRoots(List<EntityId> liveRoots) {
        for (int index = roots.size() - 1; index >= 0; index--) {
            if (!liveRoots.contains(roots.get(index))) roots.remove(index);
        }
    }

    public static synchronized void resetForTests() { clear(); roots.clear(); lastError = null; epoch = 0L; }

    static Path safeRoot(String directory) {
        if (directory == null || directory.trim().isEmpty()) throw new IllegalArgumentException("bundle directory required");
        Path root = Paths.get(directory).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) throw new IllegalArgumentException("bundle directory not found: " + directory);
        return root;
    }

    static Path resolve(Path root, String relative) {
        if (root == null || relative == null || relative.isEmpty()) throw new IllegalArgumentException("relative path required");
        Path candidate = root.resolve(relative).normalize();
        if (!candidate.startsWith(root)) throw new IllegalArgumentException("path outside bundle: " + relative);
        return candidate;
    }

    private static void destroyGraph(EntityId root) {
        if (!ArtEcs.world().contains(root)) return;
        for (EntityId entity : new ArrayList<EntityId>(ArtEcs.world().query(artframework.vfx.VfxNodeComponent.class))) {
            if (root.equals(ArtEcs.world().get(entity, artframework.vfx.VfxNodeComponent.class).rootEntity)) ArtEcs.world().destroyEntity(entity);
        }
        ArtEcs.world().destroyEntity(root);
    }

    private static final class TextureCache {
        private final Map<Path, Texture> values = new HashMap<Path, Texture>();
        Texture resolve(Path root, String relative) {
            try {
                Path path = VfxSts1Runtime.resolve(root, relative);
                Texture found = values.get(path);
                if (found != null) return found;
                if (Gdx.files == null) return null;
                Texture created = new Texture(Gdx.files.absolute(path.toString()));
                values.put(path, created);
                return created;
            } catch (Throwable ignored) { return null; }
        }
        void clear() { for (Texture texture : values.values()) try { texture.dispose(); } catch (Throwable ignored) {} values.clear(); }
    }
}
