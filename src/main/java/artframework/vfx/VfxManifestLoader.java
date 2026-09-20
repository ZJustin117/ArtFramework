package artframework.vfx;

import artframework.component.MiniJson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Strict parser for the ART-owned version 1 VFX bundle projection. */
public final class VfxManifestLoader {
    public static final String FORMAT = "art.sts2-vfx-bundle";
    public static final String SCENE_FORMAT = "art.sts2-vfx-scene";
    public static final int SCHEMA_VERSION = 1;

    public interface SceneReader { String read(String relativePath); }

    public VfxBundleDefinition parseManifest(String json) {
        Map<String, Object> root = object(MiniJson.parse(json), "manifest");
        requireString(root, "format");
        if (!FORMAT.equals(root.get("format"))) fail("manifest format");
        int version = integer(root, "schemaVersion");
        if (version != SCHEMA_VERSION) fail("unsupported manifest schemaVersion");
        String bundleId = string(root, "bundleId");
        VfxCapability capability = VfxCapability.parse(string(root, "capability"));
        List<VfxBundleDefinition.VfxSceneEntry> scenes = new ArrayList<VfxBundleDefinition.VfxSceneEntry>();
        for (Object value : array(root, "scenes")) {
            Map<String, Object> scene = object(value, "scene entry");
            scenes.add(new VfxBundleDefinition.VfxSceneEntry(
                    string(scene, "id"), safePath(string(scene, "path")),
                    VfxCapability.parse(string(scene, "capability"))));
        }
        List<VfxResourceRef> resources = resources(root.get("resources"));
        return new VfxBundleDefinition(bundleId, version, capability, scenes, resources);
    }

    public VfxSceneDefinition parseScene(String json) {
        Map<String, Object> root = object(MiniJson.parse(json), "scene");
        if (!SCENE_FORMAT.equals(string(root, "format"))) fail("scene format");
        int version = integer(root, "schemaVersion");
        if (version != SCHEMA_VERSION) fail("unsupported scene schemaVersion");
        List<VfxNodeDefinition> nodes = new ArrayList<VfxNodeDefinition>();
        for (Object value : array(root, "typedNodes")) nodes.add(node(object(value, "typed node")));
        return new VfxSceneDefinition(string(root, "id"), version, floatValue(number(root, "duration"), "duration"),
                nodes, resources(root.get("resources")), VfxCapability.parse(string(root, "capability")));
    }

    public VfxBundleDefinition load(String manifestJson, SceneReader reader) {
        VfxBundleDefinition bundle = parseManifest(manifestJson);
        if (reader == null) throw new IllegalArgumentException("scene reader is required");
        for (VfxBundleDefinition.VfxSceneEntry entry : bundle.scenes) {
            if (reader.read(entry.path) == null) throw new IllegalArgumentException("missing scene: " + entry.path);
        }
        return bundle;
    }

    /** Selects an explicitly requested scene, the conventional default scene, or the sole scene. */
    public VfxBundleDefinition.VfxSceneEntry selectScene(VfxBundleDefinition bundle, String requestedId) {
        if (bundle == null || bundle.scenes.isEmpty()) throw new IllegalArgumentException("manifest has no scenes");
        if (requestedId != null && !requestedId.isEmpty()) {
            for (VfxBundleDefinition.VfxSceneEntry scene : bundle.scenes) {
                if (requestedId.equals(scene.id)) return scene;
            }
            throw new IllegalArgumentException("scene not found: " + requestedId);
        }
        if (bundle.scenes.size() == 1) return bundle.scenes.get(0);
        for (VfxBundleDefinition.VfxSceneEntry scene : bundle.scenes) {
            if ("default".equals(scene.id)) return scene;
        }
        throw new IllegalArgumentException("scene id required for multi-scene bundle");
    }

    private static VfxNodeDefinition node(Map<String, Object> value) {
        Map<String, Object> transform = optionalObject(value.get("transform"), "transform");
        return new VfxNodeDefinition(string(value, "id"), string(value, "nodePath"),
                optionalString(value.get("parentId"), "parentId"), string(value, "nodeType"),
                vec2(transform == null ? null : transform.get("position")),
                vec2(transform == null ? null : transform.get("scale")),
                optionalFloat(transform == null ? null : transform.get("rotationDegrees"), "rotationDegrees"),
                optionalFloat(transform == null ? null : transform.get("zIndex"), "zIndex"),
                optionalBoolean(transform == null ? null : transform.get("visible"), "visible"),
                color(transform == null ? null : transform.get("modulate")),
                color(transform == null ? null : transform.get("selfModulate")),
                emitter(value.get("particleEmitter")));
    }

    private static ParticleEmitterDefinition emitter(Object raw) {
        if (raw == null) return null;
        Map<String, Object> value = object(raw, "particleEmitter");
        return new ParticleEmitterDefinition(
                optionalInteger(value.get("amount"), "amount"), optionalFloat(value.get("lifetime"), "lifetime"),
                optionalFloat(value.get("lifetimeRandomness"), "lifetimeRandomness"),
                vec2(value.get("direction")), optionalFloat(value.get("spreadDegrees"), "spreadDegrees"),
                range(value.get("initialVelocity")), vec2(value.get("gravity")), range(value.get("scale")),
                curve(value.get("scaleCurve")), curve(value.get("alphaCurve")), gradient(value.get("colorRamp")),
                range(value.get("angularVelocity")), flipbook(value.get("flipbook")),
                optionalString(value.get("blendMode"), "blendMode"),
                optionalLong(value.get("randomSeed"), "randomSeed"),
                typedResourceReference(value.get("texture"), "texture"));
    }

    private static String typedResourceReference(Object raw, String key) {
        if (raw == null) return null;
        Map<String, Object> value = object(raw, key);
        if (!"ExtResource".equals(string(value, "ref"))) fail(key + " must reference ExtResource");
        Object id = required(value, "id");
        if (id instanceof String) {
            if (((String) id).isEmpty()) fail(key + " id must be non-empty");
            return (String) id;
        }
        if (id instanceof Number && !(id instanceof Float) && !(id instanceof Double)) return String.valueOf(id);
        fail(key + " id must be a string or integer");
        return null;
    }

    private static VfxCurveDefinition curve(Object raw) {
        if (raw == null) return null;
        Map<String, Object> value = object(raw, "curve");
        List<VfxCurveDefinition.VfxCurvePoint> points = new ArrayList<VfxCurveDefinition.VfxCurvePoint>();
        for (Object rawPoint : array(value, "controlPoints")) {
            Map<String, Object> point = object(rawPoint, "curve point");
            points.add(new VfxCurveDefinition.VfxCurvePoint(floatValue(number(point, "position"), "position"),
                    floatValue(number(point, "value"), "value"), floatValue(number(point, "leftTangent"), "leftTangent"),
                    floatValue(number(point, "rightTangent"), "rightTangent"), integer(point, "leftMode"), integer(point, "rightMode")));
        }
        List<Float> limits = new ArrayList<Float>();
        Object rawLimits = value.get("limits");
        if (rawLimits != null) for (Object limit : list(rawLimits, "limits")) limits.add(floatValue(finite(limit, "limit"), "limit"));
        return new VfxCurveDefinition(points, limits);
    }

    private static VfxGradientDefinition gradient(Object raw) {
        if (raw == null) return null;
        List<VfxGradientDefinition.VfxGradientStop> stops = new ArrayList<VfxGradientDefinition.VfxGradientStop>();
        for (Object rawStop : array(object(raw, "gradient"), "stops")) {
            Map<String, Object> stop = object(rawStop, "gradient stop");
            stops.add(new VfxGradientDefinition.VfxGradientStop(floatValue(number(stop, "offset"), "offset"), colorRequired(stop.get("color"))));
        }
        return new VfxGradientDefinition(stops);
    }

    private static VfxFlipbookDefinition flipbook(Object raw) {
        if (raw == null) return null;
        Map<String, Object> value = object(raw, "flipbook");
        return new VfxFlipbookDefinition(integer(value, "hFrames"), integer(value, "vFrames"),
                bool(value, "loop"), optionalFloat(value.get("animationSpeedMin"), "animationSpeedMin") == null ? 0f : optionalFloat(value.get("animationSpeedMin"), "animationSpeedMin"),
                optionalFloat(value.get("animationSpeedMax"), "animationSpeedMax") == null ? 0f : optionalFloat(value.get("animationSpeedMax"), "animationSpeedMax"));
    }

    private static List<VfxResourceRef> resources(Object raw) {
        if (raw == null) return Collections.emptyList();
        List<VfxResourceRef> result = new ArrayList<VfxResourceRef>();
        for (Object item : list(raw, "resources")) {
            Map<String, Object> value = object(item, "resource");
            String source = safePath(string(value, "sourcePath"));
            Object output = value.get("outputPath");
            result.add(new VfxResourceRef(string(value, "id"), string(value, "kind"), source,
                    output == null ? null : safePath(string(value, "outputPath")), string(value, "status")));
        }
        return Collections.unmodifiableList(result);
    }

    private static VfxVec2 vec2(Object raw) {
        if (raw == null) return null;
        Map<String, Object> value = object(raw, "vector");
        return new VfxVec2(floatValue(number(value, "x"), "x"), floatValue(number(value, "y"), "y"));
    }

    private static VfxColor color(Object raw) { return raw == null ? null : colorRequired(raw); }
    private static VfxColor colorRequired(Object raw) {
        Map<String, Object> value = object(raw, "color");
        return new VfxColor(floatValue(number(value, "r"), "r"), floatValue(number(value, "g"), "g"),
                floatValue(number(value, "b"), "b"), floatValue(number(value, "a"), "a"));
    }

    private static VfxRange range(Object raw) {
        if (raw == null) return null;
        Map<String, Object> value = object(raw, "range");
        return new VfxRange(number(value, "min").floatValue(), number(value, "max").floatValue());
    }

    private static Map<String, Object> object(Object raw, String label) {
        if (!(raw instanceof Map)) fail(label + " must be an object");
        return (Map<String, Object>) raw;
    }
    private static Map<String, Object> optionalObject(Object raw, String label) { return raw == null ? null : object(raw, label); }
    private static List<Object> list(Object raw, String label) {
        if (!(raw instanceof List)) fail(label + " must be an array");
        return (List<Object>) raw;
    }
    private static List<Object> array(Map<String, Object> value, String key) { return list(required(value, key), key); }
    private static Object required(Map<String, Object> value, String key) { if (!value.containsKey(key)) fail("missing " + key); return value.get(key); }
    private static String string(Map<String, Object> value, String key) { return requireString(value, key); }
    private static String requireString(Map<String, Object> value, String key) {
        Object raw = required(value, key); if (!(raw instanceof String) || ((String) raw).isEmpty()) fail(key + " must be a non-empty string"); return (String) raw;
    }
    private static String optionalString(Object raw, String key) { if (raw == null) return null; if (!(raw instanceof String)) fail(key + " must be a string"); return (String) raw; }
    private static Number number(Map<String, Object> value, String key) { return finite(required(value, key), key); }
    private static Number finite(Object raw, String key) { if (!(raw instanceof Number) || raw instanceof Boolean) fail(key + " must be numeric"); Number n = (Number) raw; if (Double.isNaN(n.doubleValue()) || Double.isInfinite(n.doubleValue())) fail(key + " must be finite"); return n; }
    private static Integer optionalInteger(Object raw, String key) { return raw == null ? null : Integer.valueOf(integer(raw, key)); }
    private static int integer(Map<String, Object> value, String key) { return integer(required(value, key), key); }
    private static int integer(Object raw, String key) { if (!(raw instanceof Number) || raw instanceof Double || raw instanceof Float || raw instanceof Boolean) fail(key + " must be an integer"); long value = ((Number) raw).longValue(); if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) fail(key + " is out of range"); return (int) value; }
    private static Float optionalFloat(Object raw, String key) { return raw == null ? null : Float.valueOf(floatValue(finite(raw, key), key)); }
    private static Boolean optionalBoolean(Object raw, String key) { if (raw == null) return null; if (!(raw instanceof Boolean)) fail(key + " must be boolean"); return (Boolean) raw; }
    private static boolean bool(Map<String, Object> value, String key) { Object raw = required(value, key); if (!(raw instanceof Boolean)) fail(key + " must be boolean"); return (Boolean) raw; }
    private static Long optionalLong(Object raw, String key) { return raw == null ? null : Long.valueOf(finite(raw, key).longValue()); }
    private static float floatValue(Number value, String key) { float result = value.floatValue(); if (Float.isNaN(result) || Float.isInfinite(result)) fail(key + " must fit a finite float"); return result; }
    private static String safePath(String path) { if (path.startsWith("/") || path.indexOf('\\') >= 0 || (path.length() > 1 && path.charAt(1) == ':')) fail("unsafe relative path: " + path); String[] parts = path.split("/"); for (String part : parts) if (part.isEmpty() || ".".equals(part) || "..".equals(part)) fail("unsafe relative path: " + path); return path; }
    private static void fail(String message) { throw new IllegalArgumentException(message); }
}
