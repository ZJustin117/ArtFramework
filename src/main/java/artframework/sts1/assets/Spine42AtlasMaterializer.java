package artframework.sts1.assets;

import artframework.skeleton.SpineAtlas4xParser;
import artframework.skeleton.SpineAtlasRegion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts a compact Spine 4.x atlas into the legacy libGDX atlas syntax understood by STS1.
 */
public final class Spine42AtlasMaterializer {

    private Spine42AtlasMaterializer() {}

    public static File materialize(Sts2AssetBundle bundle, String atlasEntry) throws IOException {
        if (bundle == null) {
            throw new IllegalArgumentException("bundle required");
        }
        if (atlasEntry == null || !atlasEntry.endsWith(".atlas")) {
            throw new IllegalArgumentException("atlas entry required");
        }
        String text = new String(bundle.read(atlasEntry), StandardCharsets.UTF_8);
        List<SpineAtlasRegion> regions = SpineAtlas4xParser.parse(text);
        if (regions.isEmpty()) {
            throw new IOException("atlas contains no regions: " + atlasEntry);
        }
        File atlasFile = bundle.materialize(atlasEntry);
        File legacy = new File(atlasFile.getParentFile(), atlasFile.getName() + ".legacy");
        FileOutputStream output = new FileOutputStream(legacy);
        try {
            Map<String, List<SpineAtlasRegion>> byPage = new LinkedHashMap<String, List<SpineAtlasRegion>>();
            for (SpineAtlasRegion region : regions) {
                List<SpineAtlasRegion> pageRegions = byPage.get(region.page);
                if (pageRegions == null) {
                    pageRegions = new java.util.ArrayList<SpineAtlasRegion>();
                    byPage.put(region.page, pageRegions);
                }
                pageRegions.add(region);
            }
            boolean firstPage = true;
            for (Map.Entry<String, List<SpineAtlasRegion>> page : byPage.entrySet()) {
                if (!firstPage) {
                    output.write('\n');
                }
                firstPage = false;
                File pageFile = bundle.materialize(sibling(atlasEntry, page.getKey()));
                output.write(pageFile.getName().getBytes(StandardCharsets.UTF_8));
                output.write('\n');
                output.write("size: 0,0\nformat: RGBA8888\nfilter: Linear,Linear\nrepeat: none\n".getBytes(StandardCharsets.UTF_8));
                for (SpineAtlasRegion region : page.getValue()) {
                output.write(region.name.getBytes(StandardCharsets.UTF_8));
                output.write('\n');
                output.write(("  rotate: " + (region.degrees != 0 ? "true" : "false") + "\n").getBytes(StandardCharsets.UTF_8));
                output.write(("  xy: " + region.x + ", " + region.y + "\n").getBytes(StandardCharsets.UTF_8));
                output.write(("  size: " + region.width + ", " + region.height + "\n").getBytes(StandardCharsets.UTF_8));
                output.write(("  orig: " + region.originalWidth + ", " + region.originalHeight + "\n").getBytes(StandardCharsets.UTF_8));
                output.write(("  offset: " + region.offsetX + ", " + region.offsetY + "\n").getBytes(StandardCharsets.UTF_8));
                output.write("  index: -1\n".getBytes(StandardCharsets.UTF_8));
                }
            }
        } finally {
            output.close();
        }
        return legacy;
    }

    private static String sibling(String atlasEntry, String page) {
        int slash = atlasEntry.lastIndexOf('/');
        String parent = slash >= 0 ? atlasEntry.substring(0, slash + 1) : "";
        return parent + page;
    }
}
