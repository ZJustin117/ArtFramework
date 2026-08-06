package artframework.sts1.assets;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Developer-only STS2 asset source. It accepts either an animations directory or a generated
 * Sts2Assets.jar and never exposes the source checkout path to the runtime adapter.
 */
public final class Sts2AssetBundle implements AutoCloseable {

    public static final String ROOT = "animations/";
    private final File source;
    private final JarFile jar;
    private final File cacheRoot;
    private final Map<String, File> materialized = new LinkedHashMap<String, File>();

    private Sts2AssetBundle(File source, JarFile jar, File cacheRoot) {
        this.source = source;
        this.jar = jar;
        this.cacheRoot = cacheRoot;
    }

    public static Sts2AssetBundle open(File path, File cacheRoot) throws IOException {
        if (path == null || !path.isFile()) {
            throw new IOException("asset bundle does not exist: " + path);
        }
        if (cacheRoot == null) {
            throw new IllegalArgumentException("cacheRoot required");
        }
        if (!cacheRoot.isDirectory() && !cacheRoot.mkdirs()) {
            throw new IOException("cannot create asset cache: " + cacheRoot);
        }
        return new Sts2AssetBundle(path, new JarFile(path), cacheRoot);
    }

    public static Sts2AssetBundle openDirectory(File animations, File cacheRoot) throws IOException {
        if (animations == null || !animations.isDirectory()) {
            throw new IOException("animations directory does not exist: " + animations);
        }
        if (cacheRoot == null) {
            throw new IllegalArgumentException("cacheRoot required");
        }
        if (!cacheRoot.isDirectory() && !cacheRoot.mkdirs()) {
            throw new IOException("cannot create asset cache: " + cacheRoot);
        }
        return new Sts2AssetBundle(animations, null, cacheRoot);
    }

    public InputStream open(String entry) throws IOException {
        String normalized = normalize(entry);
        if (jar != null) {
            JarEntry found = jar.getJarEntry(normalized);
            if (found == null || found.isDirectory()) {
                throw new IOException("missing asset entry: " + normalized);
            }
            return new BufferedInputStream(jar.getInputStream(found));
        }
        File file = sourceFile(normalized);
        if (!file.isFile()) {
            throw new IOException("missing asset file: " + normalized);
        }
        return new BufferedInputStream(new FileInputStream(file));
    }

    public byte[] read(String entry) throws IOException {
        InputStream input = open(entry);
        try {
            byte[] buffer = new byte[8192];
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            int count;
            while ((count = input.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } finally {
            input.close();
        }
    }

    /** Materialize an entry to a stable local file for libGDX FileHandle consumers. */
    public synchronized File materialize(String entry) throws IOException {
        String normalized = normalize(entry);
        File existing = materialized.get(normalized);
        if (existing != null && existing.isFile()) {
            return existing;
        }
        File target = new File(cacheRoot, normalized);
        File parent = target.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("cannot create asset parent: " + parent);
        }
        InputStream input = open(normalized);
        FileOutputStream output = new FileOutputStream(target);
        try {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
        } finally {
            try {
                output.close();
            } finally {
                input.close();
            }
        }
        materialized.put(normalized, target);
        return target;
    }

    public List<String> entries(String suffix) throws IOException {
        List<String> result = new ArrayList<String>();
        if (jar != null) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(ROOT) && !name.endsWith("/") && (suffix == null || name.endsWith(suffix))) {
                    result.add(name);
                }
            }
        } else {
            collectDirectory(source, "animations/", suffix, result);
        }
        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }

    public File cacheRoot() {
        return cacheRoot;
    }

    @Override
    public void close() throws IOException {
        materialized.clear();
        if (jar != null) {
            jar.close();
        }
    }

    private File sourceFile(String normalized) {
        String relative = normalized.substring(ROOT.length());
        return new File(source, relative);
    }

    private static void collectDirectory(File root, String relative, String suffix, List<String> output) {
        File dir = new File(root, relative.substring("animations/".length()));
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            String child = relative + file.getName();
            if (file.isDirectory()) {
                collectDirectory(root, child, suffix, output);
            } else if (suffix == null || child.endsWith(suffix)) {
                output.add(child);
            }
        }
    }

    private static String normalize(String entry) {
        if (entry == null) {
            throw new IllegalArgumentException("asset entry required");
        }
        String normalized = entry.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.startsWith(ROOT) || normalized.contains("../") || normalized.contains("/..")) {
            throw new IllegalArgumentException("asset entry must stay under animations/: " + entry);
        }
        return normalized;
    }
}
