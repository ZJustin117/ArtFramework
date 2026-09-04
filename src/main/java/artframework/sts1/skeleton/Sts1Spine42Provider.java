package artframework.sts1.skeleton;

import artframework.sts1.assets.Spine42AtlasMaterializer;
import artframework.sts1.assets.Sts2AssetBundle;
import artframework.skeleton.BoneTransform;
import artframework.skeleton.SkeletonCommandProvider;
import artframework.skeleton.SkeletonHandle;
import artframework.skeleton.SkeletonSource;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/**
 * Safe shell for a user-provided shaded Spine 4.2 runtime.
 */
public final class Sts1Spine42Provider implements SkeletonCommandProvider, SkeletonNativeSlotRenderer {

    public static final String ID = "spine42";
    public static final String DEFAULT_RUNTIME_CLASS = "artframework.shaded.spine42.com.esotericsoftware.spine.Skeleton";

    private final String runtimeClassName;
    private volatile boolean available;
    private volatile String unavailableReason;
    private volatile String lastRenderError = "";
    private final String packagePrefix;
    private Sts2AssetBundle configuredBundle;
    private ClassLoader runtimeClassLoader;

    public Sts1Spine42Provider() {
        this(DEFAULT_RUNTIME_CLASS);
    }

    public Sts1Spine42Provider(String runtimeClassName) {
        this.runtimeClassName = runtimeClassName != null ? runtimeClassName : DEFAULT_RUNTIME_CLASS;
        int skeletonIndex = this.runtimeClassName.lastIndexOf(".Skeleton");
        this.packagePrefix = skeletonIndex >= 0 ? this.runtimeClassName.substring(0, skeletonIndex) : "";
        this.runtimeClassLoader = getClass().getClassLoader();
        String reason = null;
        boolean ok = false;
        try {
            Class.forName(this.runtimeClassName, false, Sts1Spine42Provider.class.getClassLoader());
            ok = true;
        } catch (Throwable t) {
            reason = t.getClass().getSimpleName() + ": " + t.getMessage();
        }
        this.available = ok;
        this.unavailableReason = reason;
    }

    @Override
    public String id() {
        return ID;
    }

    public boolean isAvailable() {
        return available;
    }

    public String unavailableReason() {
        return unavailableReason;
    }

    public String runtimeClassName() {
        return runtimeClassName;
    }

    /** Legacy Batch has no scoped polygon clip or stencil host capability. */
    public boolean supportsClipping() {
        return false;
    }

    /**
     * The legacy Batch vertex contract is position, packed light color, and UV only.
     * It has no dark-color attribute or indexed two-color draw entry.
     */
    public boolean supportsTwoColor(Object batch) {
        return false;
    }

    public synchronized void setRuntimeClassLoader(ClassLoader loader) {
        if (loader == null) {
            throw new IllegalArgumentException("runtime classloader required");
        }
        this.runtimeClassLoader = loader;
        refreshAvailability();
    }

    public synchronized void setAssetBundle(Sts2AssetBundle bundle) {
        this.configuredBundle = bundle;
    }

    public synchronized Sts2AssetBundle assetBundle() {
        return configuredBundle;
    }

    @Override
    public SkeletonHandle load(SkeletonSource source) {
        if (!available) {
            throw new IllegalStateException("spine42 runtime unavailable: " + unavailableReason);
        }
        if (source == null) {
            throw new IllegalArgumentException("source required");
        }
        Object bundleValue = source.params.get("assetBundle");
        if (bundleValue == null) {
            bundleValue = configuredBundle;
        }
        if (!(bundleValue instanceof Sts2AssetBundle)) {
            throw new IllegalArgumentException("spine42 source requires params.assetBundle");
        }
        String stage = "validate";
        try {
            Sts2AssetBundle bundle = (Sts2AssetBundle) bundleValue;
            String atlasEntry = stringParam(source.params, "atlasEntry", source.atlasPath);
            String skeletonEntry = stringParam(source.params, "skeletonEntry", source.skeletonPath);
            stage = "materialize-atlas";
            File legacyAtlas = Spine42AtlasMaterializer.materialize(bundle, atlasEntry);
            stage = "materialize-skeleton";
            File skeletonFile = bundle.materialize(skeletonEntry);
            stage = "load-atlas";
            TextureAtlas atlas = new TextureAtlas(new FileHandle(legacyAtlas));
            stage = "construct-binary";
            Class<?> binaryClass = type("SkeletonBinary");
            Object binary = binaryClass.getConstructor(TextureAtlas.class).newInstance(atlas);
            stage = "read-skeleton";
            Object data = binaryClass.getMethod("readSkeletonData", FileHandle.class).invoke(binary, new FileHandle(skeletonFile));
            stage = "construct-state";
            Object skeleton = type("Skeleton").getConstructor(type("SkeletonData")).newInstance(data);
            Object stateData = type("AnimationStateData").getConstructor(type("SkeletonData")).newInstance(data);
            Object state = type("AnimationState").getConstructor(type("AnimationStateData")).newInstance(stateData);
            applyScale(source.params, skeleton);
            RuntimeInstance instance = new RuntimeInstance(atlas, skeleton, stateData, state);
            SkeletonHandle handle = new SkeletonHandle(ID, source.skeletonId, instance);
            instance.handle = handle;
            return handle;
        } catch (Exception e) {
            throw new IllegalStateException("spine42 skeleton load failed at " + stage + ": " + describe(e), e);
        }
    }

    @Override
    public void unload(SkeletonHandle handle) {
        RuntimeInstance instance = instance(handle);
        if (instance != null && instance.atlas != null) {
            instance.atlas.dispose();
        }
        if (handle != null) {
            handle.markDisposed();
        }
    }

    @Override
    public boolean hasAnimation(SkeletonHandle handle, String animationId) {
        RuntimeInstance i = instance(handle);
        try {
            return i != null && i.skeletonData.getClass().getMethod("findAnimation", String.class).invoke(i.skeletonData, animationId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setAnimation(SkeletonHandle handle, int trackId, String animationId, boolean loop) {
        invokeState(handle, "setAnimation", new Class<?>[] {int.class, String.class, boolean.class}, trackId, animationId, loop);
    }

    @Override
    public void addAnimation(SkeletonHandle handle, int trackId, String animationId, boolean loop, float delaySeconds) {
        invokeState(handle, "addAnimation", new Class<?>[] {int.class, String.class, boolean.class, float.class}, trackId, animationId, loop, delaySeconds);
    }

    @Override
    public String currentAnimation(SkeletonHandle handle, int trackId) {
        RuntimeInstance i = instance(handle);
        try {
            Object entry = i == null ? null : i.state.getClass().getMethod("getCurrent", int.class).invoke(i.state, trackId);
            Object animation = entry == null ? null : entry.getClass().getMethod("getAnimation").invoke(entry);
            return animation == null ? null : (String) animation.getClass().getMethod("getName").invoke(animation);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void setMix(SkeletonHandle handle, String from, String to, float seconds) {
        RuntimeInstance i = instance(handle);
        if (i != null) {
            try {
                i.stateData.getClass().getMethod("setMix", String.class, String.class, float.class).invoke(i.stateData, from, to, seconds);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void setTimeScale(SkeletonHandle handle, int trackId, float scale) {
        invokeTrack(handle, trackId, "setTimeScale", float.class, scale);
    }

    @Override
    public void setTrackTime(SkeletonHandle handle, int trackId, float seconds) {
        invokeTrack(handle, trackId, "setTrackTime", float.class, seconds);
    }

    @Override
    public float animationEnd(SkeletonHandle handle, int trackId) {
        Object value = invokeTrack(handle, trackId, "getAnimationEnd");
        return value instanceof Number ? ((Number) value).floatValue() : 0f;
    }

    @Override
    public void update(SkeletonHandle handle, float deltaSeconds) {
        RuntimeInstance i = instance(handle);
        if (i != null) {
            invoke(i.state, "update", new Class<?>[] {float.class}, deltaSeconds);
            invoke(i.skeleton, "update", new Class<?>[] {float.class}, deltaSeconds);
        }
    }

    @Override
    public void apply(SkeletonHandle handle) {
        RuntimeInstance i = instance(handle);
        if (i != null) {
            invoke(i.state, "apply", new Class<?>[] {i.skeleton.getClass()}, i.skeleton);
            try {
                Class<?> physics = type("Skeleton$Physics");
                Object update = Enum.valueOf((Class) physics, "update");
                invoke(i.skeleton, "updateWorldTransform", new Class<?>[] {physics}, update);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public BoneTransform boneTransform(SkeletonHandle handle, String boneName) {
        RuntimeInstance i = instance(handle);
        if (i == null) {
            return null;
        }
        try {
            Object bone = i.skeleton.getClass().getMethod("findBone", String.class).invoke(i.skeleton, boneName);
            if (bone == null) {
                return null;
            }
            return new BoneTransform(
                    number(bone, "getWorldX"), number(bone, "getWorldY"), number(bone, "getWorldRotationX"),
                    number(bone, "getWorldScaleX"), number(bone, "getWorldScaleY"));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void setPose(SkeletonHandle handle, float x, float y, float rotation,
            float scaleX, float scaleY, boolean flipX, boolean flipY) {
        RuntimeInstance i = instance(handle);
        if (i == null) return;
        invoke(i.skeleton, "setPosition", new Class<?>[] {float.class, float.class}, x, y);
        invoke(i.skeleton, "setScale", new Class<?>[] {float.class, float.class}, scaleX, scaleY);
        invoke(i.skeleton, "setFlip", new Class<?>[] {boolean.class, boolean.class}, flipX, flipY);
        try {
            Object root = i.skeleton.getClass().getMethod("getRootBone").invoke(i.skeleton);
            invoke(root, "setRotation", new Class<?>[] {float.class}, rotation);
            apply(i.handle);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void setVisual(SkeletonHandle handle, boolean visible, float red, float green,
            float blue, float alpha) {
        RuntimeInstance i = instance(handle);
        if (i == null) return;
        try {
            Object color = i.skeleton.getClass().getMethod("getColor").invoke(i.skeleton);
            color.getClass().getMethod("set", float.class, float.class, float.class, float.class)
                    .invoke(color, red, green, blue, visible ? alpha : 0f);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void render(SkeletonHandle handle, Object batch) {
        renderInternal(handle, batch, false);
    }

    /** Draws the supported subset without changing the host batch lifecycle. */
    @Override
    public boolean renderAtNativeSlot(SkeletonHandle handle, Object batch) {
        RuntimeInstance instance = instance(handle);
        if (instance != null) {
            // Evidence is a per-render observation. Reset before entering the renderer so every
            // zero/error path is observable as zero rather than inheriting an earlier success.
            instance.lastNativeSlotDrawCount = 0;
        }
        int rendered = 0;
        try {
            rendered = renderInternal(handle, batch, true);
            return rendered > 0;
        } finally {
            if (instance != null) {
                instance.lastNativeSlotDrawCount = rendered;
            }
        }
    }

    /** Host-only diagnostic count from the most recent native-slot render for this handle. */
    public int lastNativeSlotDrawCount(SkeletonHandle handle) {
        RuntimeInstance instance = instance(handle);
        return instance != null ? instance.lastNativeSlotDrawCount : 0;
    }

    /** Number of quads submitted; indexed meshes are expanded into degenerate quads. */
    private int renderInternal(SkeletonHandle handle, Object batch, boolean nativeSlot) {
        RuntimeInstance i = instance(handle);
        if (i == null || batch == null || !isVisible(i.skeleton)) return 0;
        lastRenderError = "";
        try {
            Object drawOrder = i.skeleton.getClass().getMethod("getDrawOrder").invoke(i.skeleton);
            if (drawOrder == null) throw new IllegalArgumentException("missing skeleton draw order");
            int size = ((Number) drawOrder.getClass().getField("size").get(drawOrder)).intValue();
            Object items = drawOrder.getClass().getField("items").get(drawOrder);
            Class<?> regionType = type("attachments.RegionAttachment");
            Class<?> meshType = type("attachments.MeshAttachment");
            Class<?> clippingType = optionalType("attachments.ClippingAttachment");
            Method draw = batch.getClass().getMethod("draw", Texture.class, float[].class, int.class, int.class);
            if (!(items instanceof Object[]) || size < 0 || size > ((Object[]) items).length) {
                throw new IllegalArgumentException("malformed skeleton draw order");
            }
            Object[] slots = (Object[]) items;
            List<PreparedDraw> prepared = new ArrayList<PreparedDraw>();
            for (int slotIndex = 0; slotIndex < size; slotIndex++) {
                Object slot = slots[slotIndex];
                if (slot == null) throw new IllegalArgumentException("null slot in skeleton draw order");
                try {
                    // Preserve whole-skeleton two-color fail-open, including slots whose
                    // attachment is currently null.
                    rejectTwoColor(slot);
                    Object attachment = slot.getClass().getMethod("getAttachment").invoke(slot);
                    if (attachment == null) continue;
                    if (isClippingAttachment(attachment.getClass(), clippingType)) {
                        throw new IllegalArgumentException("ClippingAttachment unsupported by legacy Batch; fail-open");
                    }
                    if (!regionType.isInstance(attachment) && !meshType.isInstance(attachment)) {
                        throw new IllegalArgumentException("unsupported attachment in skeleton draw order; fail-open");
                    }
                    Object region = attachment.getClass().getMethod("getRegion").invoke(attachment);
                    if (region == null || !(region.getClass().getMethod("getTexture").invoke(region) instanceof Texture)) {
                        throw new IllegalArgumentException("attachment has no valid texture");
                    }
                    Object slotColor = slot.getClass().getMethod("getColor").invoke(slot);
                    Object attachmentColor = attachment.getClass().getMethod("getColor").invoke(attachment);
                    float[] rgba = multipliedColor(i.skeleton, slotColor, attachmentColor);
                    validateFinite(rgba, "attachment colors");
                    float packed = Color.toFloatBits(rgba[0], rgba[1], rgba[2], rgba[3]);
                    Object texture = region.getClass().getMethod("getTexture").invoke(region);
                    rejectTwoColor(attachment);
                    if (regionType.isInstance(attachment)) {
                        float[] world = new float[8];
                        attachment.getClass().getMethod("computeWorldVertices", slot.getClass(), float[].class, int.class, int.class)
                                .invoke(attachment, slot, world, 0, 2);
                        float[] uvs = (float[]) attachment.getClass().getMethod("getUVs").invoke(attachment);
                        prepared.add(new PreparedDraw((Texture) texture, regionVerticesChecked(world, uvs, packed)));
                    } else {
                        float[] uvs = (float[]) attachment.getClass().getMethod("getUVs").invoke(attachment);
                        short[] triangles = (short[]) attachment.getClass().getMethod("getTriangles").invoke(attachment);
                        int vertexCount = ((Number) attachment.getClass().getMethod("getWorldVerticesLength").invoke(attachment)).intValue();
                        validateMeshData(vertexCount, uvs, triangles);
                        float[] world = new float[vertexCount];
                        attachment.getClass().getMethod("computeWorldVertices", slot.getClass(), int.class, int.class,
                                float[].class, int.class, int.class).invoke(attachment, slot, 0, vertexCount, world, 0, 2);
                        for (int triangle = 0; triangle < triangles.length; triangle += 3) {
                            prepared.add(new PreparedDraw((Texture) texture,
                                    meshTriangleVerticesChecked(world, uvs, triangles, triangle, packed)));
                        }
                    }
                } catch (Throwable t) {
                    lastRenderError = describe(t.getCause() != null ? t.getCause() : t);
                    return 0;
                }
            }
            // Commit only after the complete draw order has been prepared. A Batch failure cannot
            // roll back earlier submissions; return zero conservatively so native suppression is
            // not claimed (the already-submitted prefix remains the host limitation).
            int rendered = 0;
            try {
                for (PreparedDraw command : prepared) {
                    draw.invoke(batch, command.texture, command.vertices, 0, command.vertices.length);
                    rendered++;
                }
            } catch (Throwable t) {
                lastRenderError = describe(t.getCause() != null ? t.getCause() : t);
                return 0;
            }
            return rendered;
        } catch (Throwable t) {
            Throwable root = t;
            if (t.getCause() != null) root = t.getCause();
            lastRenderError = describe(root);
            return 0;
        }
    }

    private static void rejectTwoColor(Object value) throws Exception {
        Method method;
        try {
            method = value.getClass().getMethod("getDarkColor");
        } catch (NoSuchMethodException absentOnLegacyRuntime) {
            return;
        }
        if (method.invoke(value) != null) {
            throw new IllegalArgumentException("two-color attachment unsupported by legacy Batch; fail-open");
        }
    }

    private static float[] regionVerticesChecked(float[] world, float[] uvs, float packedColor) {
        validateFinite(world, "region world vertices");
        validateFinite(uvs, "region UVs");
        if (world.length != 8 || uvs.length != 8) throw new IllegalArgumentException("region vertices require exactly four positions and UVs");
        return regionVertices(world, uvs, packedColor);
    }

    private static float[] meshTriangleVerticesChecked(float[] world, float[] uvs, short[] triangles,
            int triangleOffset, float packedColor) {
        validateFinite(world, "mesh world vertices");
        validateFinite(uvs, "mesh UVs");
        float[] vertices = meshTriangleVertices(world, uvs, triangles, triangleOffset, packedColor);
        validateBatchVertices(vertices);
        return vertices;
    }

    private static void validateMeshData(int vertexCount, float[] uvs, short[] triangles) {
        if (vertexCount <= 0 || (vertexCount & 1) != 0) {
            throw new IllegalArgumentException("invalid mesh world vertex count");
        }
        if (uvs == null || uvs.length != vertexCount) {
            throw new IllegalArgumentException("mesh UV count does not match world vertex count");
        }
        validateFinite(uvs, "mesh UVs");
        if (triangles == null || triangles.length == 0 || triangles.length % 3 != 0) {
            throw new IllegalArgumentException("mesh triangles are incomplete");
        }
        int vertexTotal = vertexCount / 2;
        for (short triangle : triangles) {
            if ((triangle & 0xffff) >= vertexTotal) {
                throw new IllegalArgumentException("mesh triangle index outside world vertices");
            }
        }
    }

    static void validateBatchVertices(float[] vertices) {
        if (vertices == null || vertices.length != 20) {
            throw new IllegalArgumentException("legacy Batch requires one four-vertex command");
        }
        validateFinite(vertices, "legacy Batch vertices");
    }

    private static void validateFinite(float[] values, String name) {
        if (values == null) throw new IllegalArgumentException(name + " missing");
        for (float value : values) if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new IllegalArgumentException(name + " contains non-finite data");
        }
    }

    private static final class PreparedDraw {
        private final Texture texture;
        private final float[] vertices;

        private PreparedDraw(Texture texture, float[] vertices) {
            this.texture = texture;
            this.vertices = vertices;
        }
    }

    private static boolean containsUnsupportedClipping(Object[] slots, int size, Class<?> clippingType) {
        if (slots == null || clippingType == null) return false;
        int limit = Math.min(size, slots.length);
        for (int index = 0; index < limit; index++) {
            Object slot = slots[index];
            if (slot == null) continue;
            try {
                Object attachment = slot.getClass().getMethod("getAttachment").invoke(slot);
                if (isClippingAttachment(attachment == null ? null : attachment.getClass(), clippingType)) return true;
            } catch (Throwable ignored) {
                // The normal attachment loop owns malformed-slot fail-open handling.
            }
        }
        return false;
    }

    static boolean isClippingAttachment(Class<?> attachmentType, Class<?> clippingType) {
        return attachmentType != null && clippingType != null && clippingType.isAssignableFrom(attachmentType);
    }

    static boolean containsUnsupportedTwoColor(Object[] slots, int size) {
        if (slots == null) return false;
        int limit = Math.min(size, slots.length);
        for (int index = 0; index < limit; index++) {
            Object slot = slots[index];
            if (slot == null) continue;
            if (hasNonNullDarkColor(slot)) return true;
            try {
                Object attachment = slot.getClass().getMethod("getAttachment").invoke(slot);
                if (attachment != null && hasNonNullDarkColor(attachment)) return true;
            } catch (Throwable ignored) {
                // The normal attachment loop owns malformed-slot fail-open handling.
            }
        }
        return false;
    }

    private static boolean hasNonNullDarkColor(Object value) {
        try {
            Method method = value.getClass().getMethod("getDarkColor");
            return method.invoke(value) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isVisible(Object skeleton) {
        try {
            Object color = skeleton.getClass().getMethod("getColor").invoke(skeleton);
            return ((Number) color.getClass().getField("a").get(color)).floatValue() > 0f;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Converts Spine's BR, BL, UL, UR order to libGDX Batch's BL, TL, TR, BR order. */
    static float[] regionVertices(float[] world, float[] uvs, float packedColor) {
        if (world == null || uvs == null || world.length < 8 || uvs.length < 8) {
            throw new IllegalArgumentException("region vertices require four positions and UVs");
        }
        int[] order = {1, 2, 3, 0};
        float[] result = new float[20];
        for (int out = 0; out < 4; out++) {
            int in = order[out];
            result[out * 5] = world[in * 2];
            result[out * 5 + 1] = world[in * 2 + 1];
            result[out * 5 + 2] = packedColor;
            result[out * 5 + 3] = uvs[in * 2];
            result[out * 5 + 4] = uvs[in * 2 + 1];
        }
        return result;
    }

    /** Expands one indexed mesh triangle into the four-vertex format accepted by legacy Batch.draw. */
    static float[] meshTriangleVertices(float[] world, float[] uvs, short[] triangles,
            int triangleOffset, float packedColor) {
        if (world == null || uvs == null || triangles == null || triangleOffset < 0
                || triangleOffset + 2 >= triangles.length || world.length == 0
                || (world.length & 1) != 0 || uvs.length != world.length
                || (triangles.length % 3) != 0) {
            throw new IllegalArgumentException("mesh vertices, UVs, and triangle indices are incomplete");
        }
        int a = triangles[triangleOffset] & 0xffff;
        int b = triangles[triangleOffset + 1] & 0xffff;
        int c = triangles[triangleOffset + 2] & 0xffff;
        int vertexCount = world.length / 2;
        if (a >= vertexCount || b >= vertexCount || c >= vertexCount) {
            throw new IllegalArgumentException("mesh triangle index outside world vertices");
        }
        int[] order = {a, b, c, c};
        float[] result = new float[20];
        for (int out = 0; out < 4; out++) {
            int in = order[out];
            result[out * 5] = world[in * 2];
            result[out * 5 + 1] = world[in * 2 + 1];
            result[out * 5 + 2] = packedColor;
            result[out * 5 + 3] = uvs[in * 2];
            result[out * 5 + 4] = uvs[in * 2 + 1];
        }
        return result;
    }

    private static float[] multipliedColor(Object skeleton, Object slot, Object attachment) throws Exception {
        float[] result = {1f, 1f, 1f, 1f};
        Object[] colors = {skeleton.getClass().getMethod("getColor").invoke(skeleton), slot, attachment};
        for (Object color : colors) {
            result[0] *= numberField(color, "r");
            result[1] *= numberField(color, "g");
            result[2] *= numberField(color, "b");
            result[3] *= numberField(color, "a");
        }
        return result;
    }

    private static float numberField(Object object, String name) throws Exception {
        return ((Number) object.getClass().getField(name).get(object)).floatValue();
    }

    private Class<?> type(String simpleName) throws ClassNotFoundException {
        return Class.forName(packagePrefix + "." + simpleName, true, runtimeClassLoader);
    }

    private Class<?> optionalType(String simpleName) {
        try {
            return type(simpleName);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private synchronized void refreshAvailability() {
        try {
            Class.forName(runtimeClassName, false, runtimeClassLoader);
            available = true;
            unavailableReason = null;
        } catch (Throwable t) {
            available = false;
            unavailableReason = t.getClass().getSimpleName() + ": " + t.getMessage();
        }
    }

    private RuntimeInstance instance(SkeletonHandle handle) {
        return handle != null && handle.nativeRef instanceof RuntimeInstance ? (RuntimeInstance) handle.nativeRef : null;
    }

    private void invokeState(SkeletonHandle handle, String method, Class<?>[] types, Object... args) {
        RuntimeInstance i = instance(handle);
        if (i != null) {
            invoke(i.state, method, types, args);
        }
    }

    private Object invokeTrack(SkeletonHandle handle, int trackId, String method, Object... args) {
        RuntimeInstance i = instance(handle);
        try {
            Object entry = i == null ? null : i.state.getClass().getMethod("getCurrent", int.class).invoke(i.state, trackId);
            if (entry == null) return null;
            if (args.length == 0) return entry.getClass().getMethod(method).invoke(entry);
            Class<?>[] types = new Class<?>[] {args[0].getClass() == Float.class ? float.class : args[0].getClass()};
            return entry.getClass().getMethod(method, types).invoke(entry, args);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object invoke(Object target, String method, Class<?>[] types, Object... args) {
        try {
            return target.getClass().getMethod(method, types).invoke(target, args);
        } catch (Exception e) {
            return null;
        }
    }

    private static float number(Object object, String method) throws Exception {
        return ((Number) object.getClass().getMethod(method).invoke(object)).floatValue();
    }

    private static String stringParam(Map<String, Object> params, String key, String fallback) {
        Object value = params.get(key);
        return value instanceof String && !((String) value).isEmpty() ? (String) value : fallback;
    }

    private static void applyScale(Map<String, Object> params, Object skeleton) {
        Object scale = params.get("scale");
        if (scale instanceof Number) {
            float value = ((Number) scale).floatValue();
            invoke(skeleton, "setScale", new Class<?>[] {float.class, float.class}, value, value);
        }
    }

    private static String describe(Throwable error) {
        StringBuilder text = new StringBuilder();
        Throwable current = error;
        int depth = 0;
        while (current != null && depth++ < 5) {
            if (text.length() > 0) {
                text.append(" <- ");
            }
            text.append(current.getClass().getSimpleName());
            if (current.getMessage() != null) {
                text.append(": ").append(current.getMessage());
            }
            current = current.getCause();
        }
        return text.toString();
    }

    private static final class RuntimeInstance {
        private final TextureAtlas atlas;
        private final Object skeletonData;
        private final Object skeleton;
        private final Object stateData;
        private final Object state;
        private volatile int lastNativeSlotDrawCount;
        private SkeletonHandle handle;

        private RuntimeInstance(TextureAtlas atlas, Object skeleton, Object stateData, Object state) {
            this.atlas = atlas;
            this.skeleton = skeleton;
            this.skeletonData = invokeNoThrow(skeleton, "getData");
            this.stateData = stateData;
            this.state = state;
        }

        private static Object invokeNoThrow(Object object, String method) {
            try {
                return object.getClass().getMethod(method).invoke(object);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
