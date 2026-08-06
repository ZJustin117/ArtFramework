import artframework.sts1.assets.Sts2AssetBundle;
import artframework.sts1.assets.Sts2DevRuntimeLoader;
import artframework.sts1.skeleton.Sts1Spine42Provider;
import artframework.skeleton.SkeletonHandle;
import artframework.skeleton.SkeletonSource;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/** Developer-only real .skel load smoke; never compiled by the standard Gradle test task. */
public final class Spine42LoadSmoke {
    public static void main(String[] args) throws Exception {
        String assetPath = System.getenv("ART_STS2_ASSET_JAR");
        String runtimePath = System.getenv("ART_SPINE42_RUNTIME_JAR");
        if (assetPath == null || runtimePath == null) {
            throw new IllegalStateException("ART_STS2_ASSET_JAR and ART_SPINE42_RUNTIME_JAR are required");
        }
        File root = new File(System.getProperty("java.io.tmpdir"), "art-spine42-smoke-" + System.nanoTime());
        Sts2AssetBundle bundle = Sts2AssetBundle.open(new File(assetPath), root);
        Sts1Spine42Provider provider = new Sts1Spine42Provider();
        if (!Sts2DevRuntimeLoader.configure(provider, new File(runtimePath), new File(root, "dex-cache"))) {
            throw new IllegalStateException("Spine42 runtime could not be loaded: " + provider.unavailableReason());
        }
        String atlas = "animations/characters/ironclad/ironclad.atlas";
        String skeleton = "animations/characters/ironclad/ironclad.skel";
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("assetBundle", bundle);
        params.put("atlasEntry", atlas);
        params.put("skeletonEntry", skeleton);
        SkeletonHandle handle = provider.load(new SkeletonSource("smoke", atlas, skeleton, params));
        provider.setAnimation(handle, 0, "idle_loop", true);
        provider.update(handle, 0.016f);
        provider.apply(handle);
        System.out.println("PASS: loaded Spine42 skeleton=" + provider.currentAnimation(handle, 0));
        provider.unload(handle);
        bundle.close();
    }
}
