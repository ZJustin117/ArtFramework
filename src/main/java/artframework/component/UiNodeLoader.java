package artframework.component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads composition JSON into {@link UiNode} trees. Pure Java; no LibGDX.
 */
public final class UiNodeLoader {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final String[] SHORTHAND_PROPS = {
        "text",
        "title",
        "min",
        "max",
        "value",
        "name",
        "enabled",
        "checked",
        "placeholder",
        "progress"
    };

    private UiNodeLoader() {}

    public static UiNode loadClasspath(String resource) {
        if (resource == null || resource.isEmpty()) {
            throw new IllegalArgumentException("layout resource required");
        }
        InputStream in = UiNodeLoader.class.getClassLoader().getResourceAsStream(resource);
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

    public static UiNode parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("layout json required");
        }
        Object root = MiniJson.parse(json);
        if (!(root instanceof Map)) {
            throw new IllegalArgumentException("layout root must be object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) root;
        return toNode(map);
    }

    @SuppressWarnings("unchecked")
    private static UiNode toNode(Map<String, Object> map) {
        String type = stringField(map, "type");
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("layout type required");
        }
        if (!UiNodeRegistry.global().contains(type)) {
            throw new IllegalArgumentException("unknown layout type: " + type);
        }
        if (UiTypes.WINDOW.equals(type)) {
            String title = stringField(map, "title");
            if (title == null || title.isEmpty()) {
                Object propsTitle = map.get("props");
                if (propsTitle instanceof Map) {
                    Object t = ((Map<?, ?>) propsTitle).get("title");
                    if (t != null) {
                        title = String.valueOf(t);
                    }
                }
            }
            if (title == null || title.isEmpty()) {
                throw new IllegalArgumentException("window title required");
            }
        }

        UiNode.Builder b = UiNode.of(type).id(stringField(map, "id"));
        String ref = stringField(map, "ref");
        if (ref != null) {
            b.ref(ref);
        }

        Map<String, Object> props = new LinkedHashMap<String, Object>();
        Object rawProps = map.get("props");
        if (rawProps instanceof Map) {
            props.putAll((Map<String, Object>) rawProps);
        }
        for (String key : SHORTHAND_PROPS) {
            if (map.containsKey(key) && !props.containsKey(key)) {
                props.put(key, map.get(key));
            }
        }
        if (UiTypes.WINDOW.equals(type) && !props.containsKey("title")) {
            String title = stringField(map, "title");
            if (title != null) {
                props.put("title", title);
            }
        }
        b.props(props);
        b.layout(parseLayout(map));
        List<EffectDecl> effects = parseEffects(map.get("effects"));
        if (UiTypes.GLASS.equals(type) && effects.isEmpty()) {
            Map<String, Object> gp = new LinkedHashMap<String, Object>();
            if (map.containsKey("radius")) {
                gp.put("radius", map.get("radius"));
            }
            if (map.containsKey("tint")) {
                gp.put("tint", map.get("tint"));
            }
            if (map.containsKey("alpha")) {
                gp.put("alpha", map.get("alpha"));
            }
            effects.add(new EffectDecl("glass", gp));
        }
        b.effects(effects);
        if (map.containsKey("signals")) {
            b.signals(parseSignals(map.get("signals")));
        }
        b.children(childrenOf(map));
        b.slots(slotsOf(map));
        return b.build();
    }

    private static List<String> parseSignals(Object raw) {
        if (raw == null) {
            return new ArrayList<String>();
        }
        if (raw instanceof String) {
            String s = ((String) raw).trim();
            List<String> one = new ArrayList<String>();
            if (!s.isEmpty()) {
                String[] parts = s.split(",");
                for (String p : parts) {
                    String t = p.trim();
                    if (!t.isEmpty()) {
                        one.add(t);
                    }
                }
            }
            return one;
        }
        if (!(raw instanceof List)) {
            throw new IllegalArgumentException("signals must be array or string");
        }
        List<?> list = (List<?>) raw;
        List<String> out = new ArrayList<String>(list.size());
        for (Object item : list) {
            if (item == null) {
                throw new IllegalArgumentException("signal required");
            }
            String name = String.valueOf(item).trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("signal required");
            }
            out.add(name);
        }
        return out;
    }

    private static LayoutSpec parseLayout(Map<String, Object> map) {
        float width = floatField(map, "width", 0f);
        float height = floatField(map, "height", 0f);
        float minWidth = floatField(map, "minWidth", 0f);
        if (minWidth <= 0f) {
            minWidth = floatField(map, "minW", 0f);
        }
        float minHeight = floatField(map, "minHeight", 0f);
        if (minHeight <= 0f) {
            minHeight = floatField(map, "minH", 0f);
        }
        float pad = floatField(map, "pad", 0f);
        float gap = floatField(map, "gap", 0f);
        boolean grow = boolField(map, "grow", false);
        float stretchRatio = floatField(map, "stretchRatio", 1f);
        if (stretchRatio <= 0f) {
            stretchRatio = floatField(map, "ratio", 1f);
        }
        String align = stringField(map, "align");
        Integer flagsH = null;
        Integer flagsV = null;
        if (map.containsKey("sizeFlagsH")) {
            flagsH = Integer.valueOf((int) floatField(map, "sizeFlagsH", SizeFlags.DEFAULT));
        }
        if (map.containsKey("sizeFlagsV")) {
            flagsV = Integer.valueOf((int) floatField(map, "sizeFlagsV", SizeFlags.DEFAULT));
        }

        Object raw = map.get("layout");
        if (raw instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> lm = (Map<String, Object>) raw;
            if (lm.containsKey("width")) {
                width = floatField(lm, "width", width);
            }
            if (lm.containsKey("height")) {
                height = floatField(lm, "height", height);
            }
            if (lm.containsKey("minWidth") || lm.containsKey("minW")) {
                minWidth = floatField(lm, "minWidth", floatField(lm, "minW", minWidth));
            }
            if (lm.containsKey("minHeight") || lm.containsKey("minH")) {
                minHeight = floatField(lm, "minHeight", floatField(lm, "minH", minHeight));
            }
            if (lm.containsKey("pad")) {
                pad = floatField(lm, "pad", pad);
            }
            if (lm.containsKey("gap")) {
                gap = floatField(lm, "gap", gap);
            }
            if (lm.containsKey("grow")) {
                grow = boolField(lm, "grow", grow);
            }
            if (lm.containsKey("stretchRatio") || lm.containsKey("ratio")) {
                stretchRatio =
                        floatField(lm, "stretchRatio", floatField(lm, "ratio", stretchRatio));
            }
            if (lm.containsKey("align")) {
                align = stringField(lm, "align");
            }
            if (lm.containsKey("sizeFlagsH")) {
                flagsH = Integer.valueOf((int) floatField(lm, "sizeFlagsH", SizeFlags.DEFAULT));
            }
            if (lm.containsKey("sizeFlagsV")) {
                flagsV = Integer.valueOf((int) floatField(lm, "sizeFlagsV", SizeFlags.DEFAULT));
            }
        }
        int sfH = flagsH != null ? flagsH.intValue() : SizeFlags.fromGrow(grow);
        int sfV = flagsV != null ? flagsV.intValue() : SizeFlags.fromGrow(grow);
        if (width == 0f
                && height == 0f
                && minWidth == 0f
                && minHeight == 0f
                && pad == 0f
                && gap == 0f
                && !grow
                && flagsH == null
                && flagsV == null
                && stretchRatio == 1f
                && (align == null || align.isEmpty())) {
            return LayoutSpec.EMPTY;
        }
        return new LayoutSpec(
                width, height, minWidth, minHeight, pad, gap, sfH, sfV, stretchRatio, align);
    }

    @SuppressWarnings("unchecked")
    private static List<EffectDecl> parseEffects(Object raw) {
        if (raw == null) {
            return new ArrayList<EffectDecl>();
        }
        if (!(raw instanceof List)) {
            throw new IllegalArgumentException("effects must be array");
        }
        List<?> list = (List<?>) raw;
        List<EffectDecl> out = new ArrayList<EffectDecl>(list.size());
        for (Object item : list) {
            if (!(item instanceof Map)) {
                throw new IllegalArgumentException("effect must be object");
            }
            Map<String, Object> em = (Map<String, Object>) item;
            String id = stringField(em, "id");
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("effect id required");
            }
            Map<String, Object> params = new LinkedHashMap<String, Object>();
            Object p = em.get("params");
            if (p instanceof Map) {
                params.putAll((Map<String, Object>) p);
            }
            out.add(new EffectDecl(id, params));
        }
        return out;
    }

    private static List<UiNode> childrenOf(Map<String, Object> map) {
        Object raw = map.get("children");
        if (raw == null) {
            return new ArrayList<UiNode>();
        }
        if (!(raw instanceof List)) {
            throw new IllegalArgumentException("children must be array");
        }
        List<?> list = (List<?>) raw;
        List<UiNode> out = new ArrayList<UiNode>(list.size());
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

    @SuppressWarnings("unchecked")
    private static Map<String, List<UiNode>> slotsOf(Map<String, Object> map) {
        Object raw = map.get("slots");
        if (raw == null) {
            return new LinkedHashMap<String, List<UiNode>>();
        }
        if (!(raw instanceof Map)) {
            throw new IllegalArgumentException("slots must be object");
        }
        Map<String, Object> sm = (Map<String, Object>) raw;
        Map<String, List<UiNode>> out = new LinkedHashMap<String, List<UiNode>>();
        for (Map.Entry<String, Object> e : sm.entrySet()) {
            Object v = e.getValue();
            if (!(v instanceof List)) {
                throw new IllegalArgumentException("slot value must be array: " + e.getKey());
            }
            List<?> list = (List<?>) v;
            List<UiNode> nodes = new ArrayList<UiNode>(list.size());
            for (Object item : list) {
                if (!(item instanceof Map)) {
                    throw new IllegalArgumentException("slot child must be object");
                }
                nodes.add(toNode((Map<String, Object>) item));
            }
            out.put(e.getKey(), nodes);
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
        if (v instanceof Boolean) {
            throw new IllegalArgumentException(key + " must be number");
        }
        throw new IllegalArgumentException(key + " must be number");
    }

    private static boolean boolField(Map<String, Object> map, String key, boolean defaultValue) {
        Object v = map.get(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue();
        }
        throw new IllegalArgumentException(key + " must be boolean");
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
