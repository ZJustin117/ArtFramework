package artframework.sts1.assets;

import artframework.assets.AssetResolveResult;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.helpers.ImageMaster;

import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** STS1-only bridge from resolved logical asset sources to host textures. */
public final class Sts1AssetMaterializer {

    private static final Map<String, Texture> CACHE = new LinkedHashMap<String, Texture>();
    private static final Set<String> MISSING = new HashSet<String>();

    private Sts1AssetMaterializer() {}

    public static boolean isFileBacked(String source) {
        String path = normalize(source);
        return path.startsWith("images/");
    }

    public static boolean isLogicalCardArt(String source) {
        return normalize(source).startsWith("card/art/");
    }

    public static boolean isCardFrameAtlas(String source) {
        return "cardui/frame".equals(normalize(source));
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
        if (MISSING.contains(path)) {
            return null;
        }
        try {
            Texture texture = ImageMaster.loadImage(path);
            if (texture != null) {
                CACHE.put(path, texture);
            } else {
                MISSING.add(path);
            }
            return texture;
        } catch (Throwable ignored) {
            MISSING.add(path);
            return null;
        }
    }

    /** Pure selector retained separately so catalog and renderer behavior are unit-testable. */
    public static String cardFrameAtlasKey(String cardType, String rarity) {
        String type = "SKILL".equals(cardType) ? "skill" : "POWER".equals(cardType) ? "power" : "attack";
        String tier = "RARE".equals(rarity) ? "rare" : "UNCOMMON".equals(rarity) ? "uncommon" : "common";
        return type + "." + tier;
    }

    public static void clearCache() {
        CACHE.clear();
        MISSING.clear();
    }
}
