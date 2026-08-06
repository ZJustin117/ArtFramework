package artframework.sts1.skeleton;

import artframework.sts1.assets.Spine42AtlasMaterializer;
import artframework.sts1.assets.Sts2AssetBundle;
import artframework.skeleton.BoneTransform;
import artframework.skeleton.SkeletonCommandProvider;
import artframework.skeleton.SkeletonHandle;
import artframework.skeleton.SkeletonSource;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.files.FileHandle;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
/**
 * Safe shell for a user-provided shaded Spine 4.2 runtime.
 */
public final class Sts1Spine42Provider implements SkeletonCommandProvider {

    public static final String ID = "spine42";
    public static final String DEFAULT_RUNTIME_CLASS = "artframework.shaded.spine42.com.esotericsoftware.spine.Skeleton";

    private final String runtimeClassName;
    private volatile boolean available;
    private volatile String unavailableReason;
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
            return new SkeletonHandle(ID, source.skeletonId, instance);
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
    public void render(SkeletonHandle handle, Object batch) {
        // The source-built 4.2 runtime currently supplies the data/animation path only.
        // Rendering is withheld until the libGDX 1.9.5 mesh/clipping adapter is validated.
    }

    private Class<?> type(String simpleName) throws ClassNotFoundException {
        return Class.forName(packagePrefix + "." + simpleName, true, runtimeClassLoader);
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
