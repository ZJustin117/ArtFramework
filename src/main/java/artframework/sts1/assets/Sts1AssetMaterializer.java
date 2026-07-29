package artframework.sts1.assets;

import artframework.assets.AssetResolveResult;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.helpers.ImageMaster;

import java.util.LinkedHashMap;
import java.util.Map;

/** STS1-only bridge from resolved logical asset sources to host textures. */
public final class Sts1AssetMaterializer {

    private static final Map<String, Texture> CACHE = new LinkedHashMap<String, Texture>();

    private Sts1AssetMaterializer() {}

    public static boolean isFileBacked(String source) {
        String path = normalize(source);
        return path.startsWith("images/");
    }

    public static boolean isLogicalCardArt(String source) {
        return normalize(source).startsWith("card/art/");
    }

    public static String normalize(String source) {
        if (source == null) {
            return "";
        }
        return source.startsWith(Sts1VanillaCatalog.SOURCE_PREFIX)
                ? source.substring(Sts1VanillaCatalog.SOURCE_PREFIX.length())
                : source;
    }

    public static Texture resolveTexture(AssetResolveResult result) {
        return result != null && result.found ? resolveTexture(result.source) : null;
    }

    public static Texture resolveTexture(String source) {
        if (!isFileBacked(source)) {
            return null;
        }
        String path = normalize(source);
        Texture cached = CACHE.get(path);
        if (cached != null) {
            return cached;
        }
        try {
            Texture texture = ImageMaster.loadImage(path);
            if (texture != null) {
                CACHE.put(path, texture);
            }
            return texture;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
