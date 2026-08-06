package artframework.sts1.assets;

import artframework.sts1.skeleton.Sts1Spine42Provider;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;

/** Loads the optional developer runtime without adding it to the mod library. */
public final class Sts2DevRuntimeLoader {

    private Sts2DevRuntimeLoader() {}

    public static boolean configure(Sts1Spine42Provider provider, File runtimeJar, File optimizedDir) {
        if (provider == null || runtimeJar == null || !runtimeJar.isFile()) {
            return false;
        }
        try {
            ClassLoader parent = Sts2DevRuntimeLoader.class.getClassLoader();
            ClassLoader loader;
            if (isAndroid()) {
                // The developer runtime is replaced at a stable device path. Never reuse dex
                // compiled for a previous jar when configuring a new runtime on game startup.
                deleteContents(optimizedDir);
                if (!optimizedDir.isDirectory() && !optimizedDir.mkdirs()) {
                    return false;
                }
                Class<?> dexClass = Class.forName("dalvik.system.DexClassLoader");
                Constructor<?> ctor = dexClass.getConstructor(String.class, String.class, String.class, ClassLoader.class);
                loader = (ClassLoader) ctor.newInstance(runtimeJar.getAbsolutePath(), optimizedDir.getAbsolutePath(), null, parent);
            } else {
                loader = new URLClassLoader(new URL[] {runtimeJar.toURI().toURL()}, parent);
            }
            provider.setRuntimeClassLoader(loader);
            return provider.isAvailable();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void deleteContents(File directory) {
        if (!directory.isDirectory()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                deleteContents(file);
            }
            file.delete();
        }
    }

    private static boolean isAndroid() {
        try {
            Class.forName("android.os.Build", false, Sts2DevRuntimeLoader.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
