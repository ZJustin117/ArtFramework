package artframework.component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON parser: object, array, string, number, true/false/null. Pure Java.
 */
public final class MiniJson {

    private final String s;
    private int i;

    public MiniJson(String s) {
        this.s = s;
    }

    public static Object parse(String json) {
        return new MiniJson(json).parseValue();
    }

    public Object parseValue() {
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
