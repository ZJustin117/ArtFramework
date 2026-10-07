package artframework.sts1.assets;

import artframework.assets.AtlasRegion;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parser for the TexturePacker JSON sheet format ({@code .tpsheet}) used by STS2 image atlases
 * into host-neutral {@link AtlasRegion} descriptors.
 *
 * <p>This is the JSON sibling of {@link artframework.assets.LibGdxAtlasParser}: it emits the same
 * host-neutral value type (page identity, page size, packed rect, untrimmed size, offset,
 * rotation) but reads the TexturePacker JSON shape rather than the legacy libGDX text shape. The
 * sts1/assets layer is allowed to import libGDX, and the parser reuses
 * {@code com.badlogic.gdx.utils.JsonReader}/{@code JsonValue} (present in the host jar) instead of
 * hand-parsing.
 *
 * <p>The expected shape is:
 * <pre>{@code
 * {
 *   "textures": [
 *     { "image": "intent_atlas.png",
 *       "size": { "w": 932, "h": 932 },
 *       "sprites": [
 *         { "filename": "attack/intent_attack_5.png",
 *           "region": { "x": 1, "y": 66, "w": 63, "h": 62 },
 *           "margin": { "x": 4, "y": 7, "w": 9, "h": 10 } }, ... ] } ]
 * }
 * }</pre>
 *
 * <p><strong>Field mapping (one {@code textures[]} entry = one page, one {@code sprites[]} entry =
 * one region).</strong>
 * <ul>
 *   <li>{@code textures[].image} -&gt; {@link AtlasRegion#page} (page image filename, kept as-is).</li>
 *   <li>{@code textures[].size.w/h} -&gt; {@link AtlasRegion#pageWidth}/{@code pageHeight}.</li>
 *   <li>{@code sprites[].filename} -&gt; {@link AtlasRegion#name} (kept as-is, including any
 *       {@code dir/} prefix).</li>
 *   <li>{@code sprites[].region.x/y/w/h} -&gt; {@link AtlasRegion#x}/{@code y}/{@code width}/
 *       {@code height} (the packed page rect).</li>
 *   <li>{@code sprites[].margin.x/y} -&gt; {@link AtlasRegion#offsetX}/{@code offsetY} (the trim
 *       offset back to the untrimmed image).</li>
 *   <li>{@code sprites[].margin.w/h} -&gt; added to the packed size for
 *       {@link AtlasRegion#originalWidth}/{@code originalHeight}
 *       ({@code originalWidth = region.w + margin.w}, {@code originalHeight = region.h +
 *       margin.h}), so an untrimmed entry ({@code margin} all zero) reports {@code trimmed() ==
 *       false}. This is verified against the real sample
 *       ({@code intent_attack_5.png}: {@code region} 63x62 + {@code margin} 9x10 = original
 *       72x72).</li>
 *   <li>{@link AtlasRegion#degrees} is always {@code 0}. The TexturePacker JSON shape as emitted in
 *       the verified STS2 samples carries no rotation field (a key scan of `/home/justinz/sts2`
 *       atlases found no {@code rotated}/{@code rotate}), so every region is unrotated here. NOTE:
 *       general TexturePacker JSON can emit a rotation flag; if a rotated `.tpsheet` appears, this
 *       parser would report {@code degrees == 0} and misclassify the packed rect, so a rotation
 *       field should be read before this parser is used on such a sheet.</li>
 * </ul>
 *
 * <p>Multiple pages are supported and region order is preserved. Key lookup is case-insensitive via
 * {@code JsonValue.get}/{@code has}, and the underlying {@code JsonReader} is whitespace-tolerant.
 * Missing container fields ({@code textures}, {@code sprites}, {@code size}, {@code region},
 * {@code margin}) are defaulted (zero) or skipped rather than throwing, and a sprite without a
 * usable {@code filename} is skipped.
 *
 * <p><strong>Fail-open (divergence from {@link artframework.assets.LibGdxAtlasParser}).</strong>
 * {@code LibGdxAtlasParser.parse(String)} throws {@link IllegalArgumentException} on a null or
 * invalid atlas and its {@code parse(Reader)} propagates {@link java.io.IOException}. This parser
 * instead NEVER throws: a {@code null}, blank, or unparseable input returns an empty list, and
 * per-field shape problems are defaulted. The reason is the caller context: a {@code .tpsheet} is a
 * developer/user-owned asset read at load time, where an empty region set is a recoverable "no
 * atlas regions" result for a materializer, whereas a hard throw would abort asset load. Callers
 * that need strictness can treat an empty result as the error signal.
 */
public final class Sts2TpsheetParser {

    private Sts2TpsheetParser() {}

    /**
     * Parses a TexturePacker {@code .tpsheet} JSON string. Fail-open: a {@code null}, blank, or
     * malformed input yields an empty (immutable) list and never throws.
     */
    public static List<AtlasRegion> parse(String json) {
        if (json == null) {
            return Collections.emptyList();
        }
        return parse(new StringReader(json));
    }

    /**
     * Parses a TexturePacker {@code .tpsheet} JSON stream. Fail-open: a {@code null} reader, an
     * empty stream, or malformed content yields an empty (immutable) list and never throws.
     */
    public static List<AtlasRegion> parse(Reader reader) {
        if (reader == null) {
            return Collections.emptyList();
        }
        JsonValue root;
        try {
            root = new JsonReader().parse(reader);
        } catch (RuntimeException e) {
            // Malformed JSON (SerializationException), a null body, or a low-level read failure.
            return Collections.emptyList();
        }
        if (root == null || !root.isObject()) {
            return Collections.emptyList();
        }
        JsonValue textures = childOf(root, "textures");
        if (textures == null || !textures.isArray()) {
            return Collections.emptyList();
        }
        List<AtlasRegion> regions = new ArrayList<AtlasRegion>();
        for (JsonValue texture : textures) {
            if (texture != null && texture.isObject()) {
                parsePage(texture, regions);
            }
        }
        return Collections.unmodifiableList(regions);
    }

    /**
     * Appends every region of one {@code textures[]} page object to {@code out}. Package-private
     * seam for testing a single page; never throws.
     */
    static void parsePage(JsonValue texture, List<AtlasRegion> out) {
        String page = stringOf(childOf(texture, "image"), "");
        int[] pageSize = intsOf(childOf(texture, "size"), "w", "h");
        JsonValue sprites = childOf(texture, "sprites");
        if (sprites == null || !sprites.isArray()) {
            return;
        }
        for (JsonValue sprite : sprites) {
            if (sprite == null || !sprite.isObject()) {
                continue;
            }
            String name = stringOf(childOf(sprite, "filename"), null);
            if (name == null || name.trim().isEmpty()) {
                // A region with no usable name cannot be addressed by a consumer; skip it.
                continue;
            }
            int[] rect = intsOf(childOf(sprite, "region"), "x", "y", "w", "h");
            int[] margin = intsOf(childOf(sprite, "margin"), "x", "y", "w", "h");
            int originalWidth = rect[2] + margin[2];
            int originalHeight = rect[3] + margin[3];
            out.add(new AtlasRegion(
                    page,
                    name,
                    rect[0],
                    rect[1],
                    rect[2],
                    rect[3],
                    pageSize[0],
                    pageSize[1],
                    originalWidth,
                    originalHeight,
                    margin[0],
                    margin[1],
                    0));
        }
    }

    /**
     * Case-insensitive child lookup that never throws for non-object parents. Falls back to a
     * linear child scan because {@code JsonValue.get(String)} can NPE on some malformed shapes.
     */
    private static JsonValue childOf(JsonValue parent, String name) {
        if (parent == null || !parent.isObject()) {
            return null;
        }
        JsonValue direct;
        try {
            direct = parent.get(name);
        } catch (RuntimeException e) {
            direct = null;
        }
        if (direct != null) {
            return direct;
        }
        for (JsonValue child = parent.child; child != null; child = child.next) {
            if (name.equalsIgnoreCase(child.name)) {
                return child;
            }
        }
        return null;
    }

    /** Reads a string field, returning {@code fallback} for missing/non-string values. */
    private static String stringOf(JsonValue value, String fallback) {
        if (value == null || value.isNull()) {
            return fallback;
        }
        try {
            if (!value.isString()) {
                return fallback;
            }
            String text = value.asString();
            return text != null ? text : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /**
     * Reads the named integer fields from an object, defaulting any missing/blank/non-numeric field
     * to {@code 0}. Guards a non-object container so a missing {@code region}/{@code margin}/
     * {@code size} yields all-zero defaults rather than an NPE. Extra keys are ignored.
     */
    private static int[] intsOf(JsonValue container, String... names) {
        int[] values = new int[names.length];
        if (container == null || !container.isObject()) {
            return values;
        }
        for (int i = 0; i < names.length; i++) {
            JsonValue field = childOf(container, names[i]);
            if (field == null || field.isNull()) {
                continue;
            }
            try {
                if (!field.isNumber()) {
                    continue;
                }
                values[i] = field.asInt();
            } catch (RuntimeException e) {
                // Non-numeric value: keep the 0 default (fail-open).
            }
        }
        return values;
    }
}
