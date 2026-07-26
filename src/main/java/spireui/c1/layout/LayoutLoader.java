package spireui.c1.layout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
        Object root = new MiniJson(json).parseValue();
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

    /** Minimal JSON parser: object, array, string, number, true/false/null. */
    private static final class MiniJson {
        private final String s;
        private int i;

        MiniJson(String s) {
            this.s = s;
        }

        Object parseValue() {
            skipWs();
            if (i >= s.length()) {
                throw new IllegalArgumentException("unexpected end of json");
            }
            char c = s.charAt(i);
            if (c == '{') {
                return parseObject();
            }
            if (c == '[') {
                return parseArray();
            }
            if (c == '"') {
                return parseString();
            }
            if (c == 't' || c == 'f' || c == 'n') {
                return parseLiteral();
            }
            if (c == '-' || (c >= '0' && c <= '9')) {
                return parseNumber();
            }
            throw new IllegalArgumentException("unexpected char at " + i + ": " + c);
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            skipWs();
            if (peek('}')) {
                i++;
                return map;
            }
            while (true) {
                skipWs();
                String key = parseString();
                skipWs();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWs();
                if (peek('}')) {
                    i++;
                    return map;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> list = new ArrayList<Object>();
            skipWs();
            if (peek(']')) {
                i++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWs();
                if (peek(']')) {
                    i++;
                    return list;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (i >= s.length()) {
                        throw new IllegalArgumentException("bad escape");
                    }
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"':
                        case '\\':
                        case '/':
                            sb.append(e);
                            break;
                        case 'b':
                            sb.append('\b');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'u':
                            if (i + 4 > s.length()) {
                                throw new IllegalArgumentException("bad unicode escape");
                            }
                            int code = Integer.parseInt(s.substring(i, i + 4), 16);
                            sb.append((char) code);
                            i += 4;
                            break;
                        default:
                            throw new IllegalArgumentException("bad escape: \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalArgumentException("unterminated string");
        }

        private Number parseNumber() {
            int start = i;
            if (peek('-')) {
                i++;
            }
            digits();
            if (peek('.')) {
                i++;
                digits();
            }
            if (peek('e') || peek('E')) {
                i++;
                if (peek('+') || peek('-')) {
                    i++;
                }
                digits();
            }
            String raw = s.substring(start, i);
            if (raw.indexOf('.') >= 0 || raw.indexOf('e') >= 0 || raw.indexOf('E') >= 0) {
                return Double.valueOf(raw);
            }
            long v = Long.parseLong(raw);
            if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) {
                return Integer.valueOf((int) v);
            }
            return Long.valueOf(v);
        }

        private void digits() {
            if (i >= s.length() || s.charAt(i) < '0' || s.charAt(i) > '9') {
                throw new IllegalArgumentException("expected digit at " + i);
            }
            while (i < s.length() && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
                i++;
            }
        }

        private Object parseLiteral() {
            if (s.startsWith("true", i)) {
                i += 4;
                return Boolean.TRUE;
            }
            if (s.startsWith("false", i)) {
                i += 5;
                return Boolean.FALSE;
            }
            if (s.startsWith("null", i)) {
                i += 4;
                return null;
            }
            throw new IllegalArgumentException("bad literal at " + i);
        }

        private void skipWs() {
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                    i++;
                } else {
                    return;
                }
            }
        }

        private boolean peek(char c) {
            return i < s.length() && s.charAt(i) == c;
        }

        private void expect(char c) {
            skipWs();
            if (i >= s.length() || s.charAt(i) != c) {
                throw new IllegalArgumentException("expected '" + c + "' at " + i);
            }
            i++;
        }
    }
}
