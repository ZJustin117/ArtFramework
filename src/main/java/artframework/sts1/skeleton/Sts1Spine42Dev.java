package artframework.sts1.skeleton;

import artframework.api.ArtFramework;
import artframework.sts1.assets.Sts2AssetBundle;
import artframework.sts1.assets.Sts2DevRuntimeLoader;
import com.badlogic.gdx.Gdx;
import basemod.BaseMod;

import java.io.File;

/** Installs the optional D1 developer bundle when the two private files are present. */
public final class Sts1Spine42Dev {

    public static final String DEVICE_DIR = "sts/art-assets";

    private Sts1Spine42Dev() {}

    public static Sts1Spine42Provider install() {
        Sts1Spine42Provider provider = new Sts1Spine42Provider();
        ArtFramework.skeletons().register(provider);
        Sts1SkeletonBridge.setProviderId(Sts1Spine42Provider.ID);
        try {
            File root = null;
            File[] candidates = new File[] {
                Gdx.files.external("Android/data/io.stamethyst/files/" + DEVICE_DIR).file(),
                Gdx.files.external(DEVICE_DIR).file(),
                Gdx.files.absolute("/sdcard/Android/data/io.stamethyst/files/" + DEVICE_DIR).file()
            };
            for (File candidate : candidates) {
                if (candidate.isDirectory()) {
                    root = candidate;
                    break;
                }
            }
            if (root == null) {
                BaseMod.logger.warn("ArtFramework: Spine42 asset directory not found: " + candidates[0] + ", " + candidates[1] + ", " + candidates[2]);
                return provider;
            }
            File assetJar = new File(root, "Sts2Assets.jar");
            File runtimeJar = new File(root, "ArtFramework-Spine42Runtime.jar");
            if (assetJar.isFile()) {
                Sts2AssetBundle bundle = Sts2AssetBundle.open(assetJar, new File(root, "cache"));
                Sts1SkeletonBridge.setDeveloperBundle(bundle);
            }
            if (runtimeJar.isFile()) {
                Sts2DevRuntimeLoader.configure(provider, runtimeJar, new File(root, "dex-cache"));
            }
        } catch (Throwable ignored) {
            // Optional developer files must never prevent the main mod from starting.
            try {
                BaseMod.logger.warn("ArtFramework: Spine42 developer setup failed: " + ignored.getClass().getSimpleName() + ": " + ignored.getMessage());
            } catch (Throwable ignoredLog) {
            }
        }
        return provider;
    }
}
