package artframework.sts1.assets;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.Assert.assertTrue;

public class Spine42AtlasMaterializerTest {

    @Test
    public void writesLegacyAtlasForRotatedRegion() throws Exception {
        File root = new File(System.getProperty("java.io.tmpdir"), "art-atlas-" + System.nanoTime());
        assertTrue(root.mkdirs());
        File jar = new File(root, "assets.jar");
        JarOutputStream output = new JarOutputStream(new FileOutputStream(jar));
        try {
            put(output, "animations/test/hero.atlas", "hero.png\nsize:100,100\nscale:1\npart\nbounds:1,2,3,4\nrotate:90\n");
            put(output, "animations/test/hero.png", "not-a-real-png");
        } finally {
            output.close();
        }

        Sts2AssetBundle bundle = Sts2AssetBundle.open(jar, new File(root, "cache"));
        try {
            File legacy = Spine42AtlasMaterializer.materialize(bundle, "animations/test/hero.atlas");
            String text = new String(Files.readAllBytes(legacy.toPath()), StandardCharsets.UTF_8);
            assertTrue(text.contains("hero.png"));
            assertTrue(text.contains("rotate: true"));
            assertTrue(text.contains("xy: 1, 2"));
        } finally {
            bundle.close();
        }
    }

    private static void put(JarOutputStream output, String name, String value) throws Exception {
        output.putNextEntry(new JarEntry(name));
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
    }
}
