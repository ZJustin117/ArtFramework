package artframework.skeleton;

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
 * Parser for Spine/libGDX compact atlas files used by Spine 4.x exports.
 */
public final class SpineAtlas4xParser {

    private SpineAtlas4xParser() {}

    public static List<SpineAtlasRegion> parse(String atlasText) {
        if (atlasText == null) {
            throw new IllegalArgumentException("atlasText required");
        }
        try {
            return parse(new StringReader(atlasText));
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid atlas", e);
        }
    }

    public static List<SpineAtlasRegion> parse(Reader reader) throws IOException {
        if (reader == null) {
            throw new IllegalArgumentException("reader required");
        }
        BufferedReader br = new BufferedReader(reader);
        List<SpineAtlasRegion> regions = new ArrayList<SpineAtlasRegion>();
        String page = null;
        float pageScale = 1.0f;
        String currentRegion = null;
        Map<String, String> props = new LinkedHashMap<String, String>();
        String line;
        while ((line = br.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int colon = trimmed.indexOf(':');
            if (colon < 0) {
                if (page == null || isImageName(trimmed)) {
                    if (currentRegion != null) {
                        regions.add(region(page, currentRegion, props, pageScale));
                        props.clear();
                        currentRegion = null;
                    }
                    page = trimmed;
                    pageScale = 1.0f;
                } else {
                    if (currentRegion != null) {
                        regions.add(region(page, currentRegion, props, pageScale));
                        props.clear();
                    }
                    currentRegion = trimmed;
                }
                continue;
            }
            String key = trimmed.substring(0, colon).trim();
            String value = trimmed.substring(colon + 1).trim();
            if (currentRegion == null) {
                if ("scale".equals(key)) {
                    pageScale = parseFloat(value, 1.0f);
                }
            } else {
                props.put(key, value);
            }
        }
        if (currentRegion != null) {
            regions.add(region(page, currentRegion, props, pageScale));
        }
        return Collections.unmodifiableList(regions);
    }

    private static SpineAtlasRegion region(
            String page, String name, Map<String, String> props, float pageScale) {
        int[] bounds = parseInts(props.get("bounds"), 4, "bounds", name);
        int degrees = parseDegrees(props.get("rotate"));
        int[] offsets = parseInts(props.get("offsets"), 4, null, name);
        int originalWidth = bounds[2];
        int originalHeight = bounds[3];
        int offsetX = 0;
        int offsetY = 0;
        if (offsets != null) {
            offsetX = offsets[0];
            offsetY = offsets[1];
            originalWidth = offsets[2];
            originalHeight = offsets[3];
        }
        return new SpineAtlasRegion(
                page,
                name,
                bounds[0],
                bounds[1],
                bounds[2],
                bounds[3],
                originalWidth,
                originalHeight,
                offsetX,
                offsetY,
                degrees,
                pageScale);
    }

    private static boolean isImageName(String value) {
        String lower = value.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    private static int parseDegrees(String value) {
        if (value == null || value.isEmpty() || "false".equals(value)) {
            return 0;
        }
        if ("true".equals(value)) {
            return 90;
        }
        return Integer.parseInt(value);
    }

    private static int[] parseInts(String value, int expected, String requiredKey, String region) {
        if (value == null || value.isEmpty()) {
            if (requiredKey == null) {
                return null;
            }
            throw new IllegalArgumentException("region " + region + " missing " + requiredKey);
        }
        String[] parts = value.split(",");
        if (parts.length != expected) {
            throw new IllegalArgumentException("region " + region + " invalid value: " + value);
        }
        int[] out = new int[expected];
        for (int i = 0; i < expected; i++) {
            out[i] = Integer.parseInt(parts[i].trim());
        }
        return out;
    }

    private static float parseFloat(String value, float fallback) {
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        return Float.parseFloat(value);
    }
}
