package artframework.core;

import artframework.component.EffectDecl;
import artframework.component.MiniJson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Load {@link PresentPack} from classpath JSON manifest. */
public final class PresentPackLoader {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private PresentPackLoader() {}

    public static PresentPack loadClasspath(String resource) {
        if (resource == null || resource.isEmpty()) {
            throw new IllegalArgumentException("pack manifest resource required");
        }
        InputStream in = PresentPackLoader.class.getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalArgumentException("pack manifest not found: " + resource);
        }
        try {
            return parse(readFully(in));
        } catch (IOException e) {
            throw new IllegalArgumentException("pack manifest read failed: " + resource, e);
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static PresentPack parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("pack json required");
        }
        Object root = MiniJson.parse(json);
        if (!(root instanceof Map)) {
            throw new IllegalArgumentException("pack root must be object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) root;
        String id = stringField(map, "id");
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("pack id required");
        }
        PresentPack.Builder b =
                PresentPack.builder(id)
                        .profileId(stringField(map, "profileId"))
                        .version(stringField(map, "version"))
                        .provider(stringField(map, "provider"));
        Object templates = map.get("templates");
        if (templates instanceof List) {
            for (Object o : (List<?>) templates) {
                if (!(o instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> t = (Map<String, Object>) o;
                String name = stringField(t, "name");
                String res = stringField(t, "resource");
                if (name != null && res != null) {
                    b.template(name, res);
                }
            }
        }
        Object windows = map.get("windows");
        if (windows instanceof List) {
            for (Object o : (List<?>) windows) {
                if (!(o instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> w = (Map<String, Object>) o;
                String wid = stringField(w, "id");
                String res = stringField(w, "resource");
                if (wid != null && res != null) {
                    b.window(wid, res);
                }
            }
        }
        Object autoOpen = map.get("autoOpen");
        if (autoOpen instanceof List) {
            for (Object o : (List<?>) autoOpen) {
                if (o != null) {
                    b.autoOpen(String.valueOf(o));
                }
            }
        }
        if (map.containsKey("unregisterTemplatesOnDeactivate")) {
            b.unregisterTemplatesOnDeactivate(boolField(map, "unregisterTemplatesOnDeactivate", true));
        }
        if (map.containsKey("unregisterWindowsOnDeactivate")) {
            b.unregisterWindowsOnDeactivate(boolField(map, "unregisterWindowsOnDeactivate", false));
        }
        if (map.containsKey("autoCloseOnDeactivate")) {
            b.autoCloseOnDeactivate(boolField(map, "autoCloseOnDeactivate", false));
        }
        Object effectDefaults = map.get("effectDefaults");
        if (effectDefaults instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> ed = (Map<String, Object>) effectDefaults;
            for (Map.Entry<String, Object> e : ed.entrySet()) {
                if (e.getKey() == null || !(e.getValue() instanceof List)) {
                    continue;
                }
                for (EffectDecl d : parseEffectList((List<?>) e.getValue())) {
                    b.effectDefault(e.getKey(), d);
                }
            }
        }
        Object fullFrame = map.get("fullFrame");
        if (fullFrame instanceof List) {
            for (EffectDecl d : parseEffectList((List<?>) fullFrame)) {
                b.fullFrameEffect(d);
            }
        }
        Object bindSurfaces = map.get("bindSurfaces");
        if (bindSurfaces instanceof List) {
            for (Object o : (List<?>) bindSurfaces) {
                if (o != null) {
                    b.bindSurface(String.valueOf(o));
                }
            }
        }
        Object surfaceEffects = map.get("surfaceEffects");
        if (surfaceEffects instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> se = (Map<String, Object>) surfaceEffects;
            for (Map.Entry<String, Object> e : se.entrySet()) {
                if (e.getKey() == null || !(e.getValue() instanceof List)) {
                    continue;
                }
                for (EffectDecl d : parseEffectList((List<?>) e.getValue())) {
                    b.surfaceEffect(e.getKey(), d);
                }
            }
        }
        return b.build();
    }

    private static List<EffectDecl> parseEffectList(List<?> raw) {
        List<EffectDecl> out = new ArrayList<EffectDecl>();
        for (Object o : raw) {
            if (!(o instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> em = (Map<String, Object>) o;
            String eid = stringField(em, "id");
            if (eid == null) {
                eid = stringField(em, "effect");
            }
            if (eid == null) {
                continue;
            }
            Map<String, Object> params = new LinkedHashMap<String, Object>();
            Object p = em.get("params");
            if (p instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> pm = (Map<String, Object>) p;
                params.putAll(pm);
            } else {
                for (Map.Entry<String, Object> en : em.entrySet()) {
                    String k = en.getKey();
                    if ("id".equals(k) || "effect".equals(k) || "params".equals(k)) {
                        continue;
                    }
                    params.put(k, en.getValue());
                }
            }
            out.add(new EffectDecl(eid, params));
        }
        return out;
    }

    private static String stringField(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static boolean boolField(Map<String, Object> map, String key, boolean def) {
        Object v = map.get(key);
        if (v == null) {
            return def;
        }
        if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue();
        }
        return Boolean.parseBoolean(String.valueOf(v));
    }

    private static String readFully(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
        }
        return new String(out.toByteArray(), UTF_8);
    }
}
