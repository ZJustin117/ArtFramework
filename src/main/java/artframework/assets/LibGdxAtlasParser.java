package artframework.assets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for the legacy libGDX {@code TextureAtlas} text format into host-neutral
 * {@link AtlasRegion} descriptors.
 *
 * <p>This is the classic layout (page header flush-left, region names and their {@code key: value}
 * properties indented) as opposed to the Spine compact syntax handled by
 * {@link artframework.skeleton.SpineAtlas4xParser}. It is also the shape emitted by
 * {@code artframework.sts1.assets.Spine42AtlasMaterializer}: a bare page image name, a page-level
 * {@code size}, optional {@code format}/{@code filter}/{@code repeat}, then region names each
 * followed by {@code rotate}, {@code xy}, {@code size}, {@code orig}, {@code offset}, and
 * {@code index}.
 *
 * <p>Only host-neutral values are produced; there are no libGDX or STS imports. Parsing is
 * whitespace-tolerant, allows multiple page blocks, and preserves region order in the file.
 *
 * <p>Page vs region detection uses lookahead on the next non-blank line. The first bare token always
 * starts a page. Afterwards, a bare token followed by an <em>indented</em> line containing
 * {@code ':'} is a region name, and one followed by a flush-left line containing {@code ':'} is a
 * page header. When the next non-blank line is another bare token or EOF, the token is treated as a
 * page header only if it looks like an image file name ({@code .png}/{@code .jpg}/{@code .jpeg});
 * otherwise it is a region. This matches both canonical libGDX output and the materializer's
 * flush-left region names, because both indent region properties. Residual: a page header whose name
 * is not an image name and is immediately followed by another bare token (e.g. a page with no
 * {@code size} line before its first region) is treated as a region.
 */
public final class LibGdxAtlasParser {

    private LibGdxAtlasParser() {}

    public static List<AtlasRegion> parse(String atlasText) {
        if (atlasText == null) {
            throw new IllegalArgumentException("atlasText required");
        }
        try {
            return parse(new StringReader(atlasText));
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid atlas", e);
        }
    }

    public static List<AtlasRegion> parse(Reader reader) throws IOException {
        if (reader == null) {
            throw new IllegalArgumentException("reader required");
        }
        BufferedReader br = new BufferedReader(reader);
        List<String> lines = new ArrayList<String>();
        List<Boolean> indentedFlags = new ArrayList<Boolean>();
        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }
            lines.add(line.trim());
            indentedFlags.add(Boolean.valueOf(isIndented(line)));
        }

        List<AtlasRegion> regions = new ArrayList<AtlasRegion>();
        String page = null;
        int pageWidth = 0;
        int pageHeight = 0;
        String currentRegion = null;
        Map<String, String> props = new LinkedHashMap<String, String>();
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i);
            int colon = trimmed.indexOf(':');
            if (colon < 0) {
                if (currentRegion != null) {
                    regions.add(region(page, currentRegion, pageWidth, pageHeight, props));
                    props.clear();
                    currentRegion = null;
                }
                if (page == null || isPageHeader(trimmed, i, lines, indentedFlags)) {
                    page = trimmed;
                    pageWidth = 0;
                    pageHeight = 0;
                } else {
                    currentRegion = trimmed;
                }
                continue;
            }
            String key = trimmed.substring(0, colon).trim();
            String value = trimmed.substring(colon + 1).trim();
            if (currentRegion == null) {
                if ("size".equals(key)) {
                    int[] size = parsePageInts(value, page);
                    pageWidth = size[0];
                    pageHeight = size[1];
                }
                // format:, filter:, repeat:, and other page keys are ignored.
            } else {
                props.put(key, value);
            }
        }
        if (currentRegion != null) {
            regions.add(region(page, currentRegion, pageWidth, pageHeight, props));
        }
        return Collections.unmodifiableList(regions);
    }

    private static boolean isIndented(String line) {
        return !line.isEmpty() && Character.isWhitespace(line.charAt(0));
    }

    /**
     * Decides whether the bare token at {@code index} is a page header by inspecting the next
     * non-blank line (blank lines were already dropped while reading).
     */
    private static boolean isPageHeader(
            String token, int index, List<String> lines, List<Boolean> indentedFlags) {
        int next = index + 1;
        if (next < lines.size() && lines.get(next).indexOf(':') >= 0) {
            return !indentedFlags.get(next).booleanValue();
        }
        // Next non-blank line is another bare token, or EOF: fall back to the image-name heuristic.
        return isImageName(token);
    }

    private static AtlasRegion region(
            String page, String name, int pageWidth, int pageHeight, Map<String, String> props) {
        int[] xy = parseRegionInts(props.get("xy"), 2, "xy", name);
        int[] size = parseRegionInts(props.get("size"), 2, "size", name);
        int degrees = parseRegionDegrees(props.get("rotate"), name);
        int originalWidth = 0;
        int originalHeight = 0;
        int[] orig = parseRegionOptionalInts(props.get("orig"), 2, name);
        if (orig != null) {
            // Pass raw components through: AtlasRegion defaults each non-positive value
            // independently from the display dimensions.
            originalWidth = orig[0];
            originalHeight = orig[1];
        }
        int offsetX = 0;
        int offsetY = 0;
        int[] offset = parseRegionOptionalInts(props.get("offset"), 2, name);
        if (offset != null) {
            offsetX = offset[0];
            offsetY = offset[1];
        }
        // "index" is accepted but not modeled: AtlasRegion has no index, and duplicate names are
        // emitted in file order.
        return new AtlasRegion(
                page,
                name,
                xy[0],
                xy[1],
                size[0],
                size[1],
                pageWidth,
                pageHeight,
                originalWidth,
                originalHeight,
                offsetX,
                offsetY,
                degrees);
    }

    private static boolean isImageName(String value) {
        String lower = value.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    private static int parseRegionDegrees(String value, String region) {
        if (value == null || value.isEmpty() || "false".equals(value)) {
            return 0;
        }
        if ("true".equals(value)) {
            return 90;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("region " + region + " invalid value: " + value);
        }
    }

    private static int[] parseRegionInts(String value, int expected, String requiredKey, String region) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("region " + region + " missing " + requiredKey);
        }
        int[] parsed = parseRegionOptionalInts(value, expected, region);
        if (parsed == null) {
            throw new IllegalArgumentException("region " + region + " invalid value: " + value);
        }
        return parsed;
    }

    private static int[] parseRegionOptionalInts(String value, int expected, String region) {
        try {
            return parseOptionalInts(value, expected);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("region " + region + " invalid value: " + value);
        }
    }

    private static int[] parsePageInts(String value, String page) {
        String[] parts = value == null ? new String[0] : value.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("page " + page + " invalid value: " + value);
        }
        try {
            return new int[] { Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()) };
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("page " + page + " invalid value: " + value);
        }
    }

    private static int[] parseOptionalInts(String value, int expected) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String[] parts = value.split(",");
        if (parts.length != expected) {
            return null;
        }
        int[] out = new int[expected];
        for (int i = 0; i < expected; i++) {
            out[i] = Integer.parseInt(parts[i].trim());
        }
        return out;
    }
}
