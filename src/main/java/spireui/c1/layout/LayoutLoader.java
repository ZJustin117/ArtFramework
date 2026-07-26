package spireui.c1.layout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads C1 layout DSL (JSON subset) into {@link LayoutNode} trees. Pure Java; no LibGDX.
 */
public final class LayoutLoader {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private LayoutLoader() {}

    public static LayoutNode loadClasspath(String resource) {
        if (resource == null || resource.isEmpty()) {
            throw new IllegalArgumentException("layout resource required");
        }
        InputStream in = LayoutLoader.class.getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalArgumentException("layout not found: " + resource);
        }
        try {
            return parse(readFully(in));
        } catch (IOException e) {
            throw new IllegalArgumentException("layout read failed: " + resource, e);
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static LayoutNode parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("layout json required");
        }
        Object root = spireui.component.MiniJson.parse(json);
        if (!(root instanceof Map)) {
            throw new IllegalArgumentException("layout root must be object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) root;
        return toNode(map);
    }

    private static LayoutNode toNode(Map<String, Object> map) {
        String type = stringField(map, "type");
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("layout type required");
        }
        if ("window".equals(type)) {
            String title = stringField(map, "title");
            if (title == null || title.isEmpty()) {
                throw new IllegalArgumentException("window title required");
            }
            List<LayoutNode> children = childrenOf(map);
            return LayoutNode.window(
                    stringField(map, "id"),
                    title,
                    floatField(map, "width", 0f),
                    floatField(map, "height", 0f),
                    children);
        }
        if ("label".equals(type)) {
            return LayoutNode.label(stringField(map, "text"));
        }
        if ("button".equals(type)) {
            return LayoutNode.button(stringField(map, "id"), stringField(map, "text"));
        }
        throw new IllegalArgumentException("unknown layout type: " + type);
    }

    private static List<LayoutNode> childrenOf(Map<String, Object> map) {
        Object raw = map.get("children");
        if (raw == null) {
            return new ArrayList<LayoutNode>();
        }
        if (!(raw instanceof List)) {
            throw new IllegalArgumentException("children must be array");
        }
        List<?> list = (List<?>) raw;
        List<LayoutNode> out = new ArrayList<LayoutNode>(list.size());
        for (Object item : list) {
            if (!(item instanceof Map)) {
                throw new IllegalArgumentException("child must be object");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> child = (Map<String, Object>) item;
            out.add(toNode(child));
        }
        return out;
    }

    private static String stringField(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof String) {
            return (String) v;
        }
        throw new IllegalArgumentException(key + " must be string");
    }

    private static float floatField(Map<String, Object> map, String key, float defaultValue) {
        Object v = map.get(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Number) {
            return ((Number) v).floatValue();
        }
        throw new IllegalArgumentException(key + " must be number");
    }

    private static String readFully(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = in.read(chunk)) >= 0) {
            buf.write(chunk, 0, n);
        }
        return new String(buf.toByteArray(), UTF_8);
    }
}
