package artframework.sts1.assets;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Sts2AssetBundleTest {

    @Test
    public void readsAndMaterializesGeneratedBundle() throws Exception {
        File root = tempDirectory("sts2-bundle");
        File jar = new File(root, "assets.jar");
        JarOutputStream output = new JarOutputStream(new FileOutputStream(jar));
        try {
            put(output, "META-INF/artframework-sts2-assets.properties", "format=artframework-sts2-assets\n");
            put(output, "animations/characters/test/test.skel", "binary-test");
            put(output, "animations/characters/test/test.atlas", "page.png\n");
        } finally {
            output.close();
        }

        File cache = new File(root, "cache");
        Sts2AssetBundle bundle = Sts2AssetBundle.open(jar, cache);
        try {
            assertEquals(1, bundle.entries(".skel").size());
            assertEquals("binary-test", new String(bundle.read("animations/characters/test/test.skel"), StandardCharsets.UTF_8));
            File materialized = bundle.materialize("animations/characters/test/test.atlas");
            assertTrue(materialized.isFile());
            assertTrue(materialized.getPath().replace(File.separatorChar, '/').contains("animations/characters/test/test.atlas"));
        } finally {
            bundle.close();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsTraversal() throws Exception {
        File root = tempDirectory("sts2-traversal");
        Sts2AssetBundle bundle = Sts2AssetBundle.openDirectory(root, new File(root, "cache"));
        try {
            bundle.open("animations/../secret.skel");
        } finally {
            bundle.close();
        }
    }

    private static void put(JarOutputStream output, String name, String value) throws Exception {
        output.putNextEntry(new JarEntry(name));
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
    }

    private static File tempDirectory(String name) {
        File dir = new File(System.getProperty("java.io.tmpdir"), "art-test-" + name + "-" + System.nanoTime());
        assertTrue(dir.mkdirs());
        return dir;
    }
}
