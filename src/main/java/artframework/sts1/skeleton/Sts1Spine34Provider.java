package artframework.sts1.skeleton;

import artframework.skeleton.BoneTransform;
import artframework.skeleton.SkeletonCommandProvider;
import artframework.skeleton.SkeletonHandle;
import artframework.skeleton.SkeletonSource;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonJson;

import java.util.LinkedHashMap;
import java.util.Map;
import java.lang.reflect.InvocationTargetException;

/**
 * Provider for STS1's host-bundled Spine 3.4 runtime.
 */
public final class Sts1Spine34Provider implements SkeletonCommandProvider, SkeletonNativeSlotRenderer {

    public static final String ID = "spine34";

    private final Map<String, Instance> live = new LinkedHashMap<String, Instance>();
    // Keep the provider constructible in pure JUnit where the host's libGDX runtime is absent.
    private Object renderer;
    private Object meshBatch;
    private volatile boolean rendererAvailable = true;
    private volatile String lastRenderError = "";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public SkeletonHandle load(SkeletonSource source) {
        if (source == null) {
            throw new IllegalArgumentException("source required");
        }
        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal(source.atlasPath));
        SkeletonJson json = new SkeletonJson(atlas);
        Object scale = source.params.get("scale");
        if (scale instanceof Number) {
            json.setScale(((Number) scale).floatValue());
        }
        SkeletonData data = json.readSkeletonData(Gdx.files.internal(source.skeletonPath));
        Skeleton skeleton = new Skeleton(data);
        skeleton.setColor(Color.WHITE);
        AnimationStateData stateData = new AnimationStateData(data);
        AnimationState state = new AnimationState(stateData);
        Instance instance = new Instance(atlas, skeleton, stateData, state);
        SkeletonHandle handle = new SkeletonHandle(ID, source.skeletonId, instance);
        live.put(source.skeletonId, instance);
        return handle;
    }

    @Override
    public void unload(SkeletonHandle handle) {
        Instance instance = instance(handle);
        if (instance != null) {
            instance.atlas.dispose();
            live.remove(handle.skeletonId);
        }
        if (handle != null) {
            handle.markDisposed();
        }
    }

    @Override
    public boolean hasAnimation(SkeletonHandle handle, String animationId) {
        Instance i = instance(handle);
        return i != null && i.skeleton.getData().findAnimation(animationId) != null;
    }

    @Override
    public void setAnimation(SkeletonHandle handle, int trackId, String animationId, boolean loop) {
        Instance i = instance(handle);
        if (i != null) {
            i.state.setAnimation(trackId, animationId, loop);
        }
    }

    @Override
    public void addAnimation(SkeletonHandle handle, int trackId, String animationId, boolean loop, float delaySeconds) {
        Instance i = instance(handle);
        if (i != null) {
            i.state.addAnimation(trackId, animationId, loop, delaySeconds);
        }
    }

    @Override
    public String currentAnimation(SkeletonHandle handle, int trackId) {
        Instance i = instance(handle);
        if (i == null || i.state.getCurrent(trackId) == null || i.state.getCurrent(trackId).getAnimation() == null) {
            return null;
        }
        return i.state.getCurrent(trackId).getAnimation().getName();
    }

    @Override
    public void setMix(SkeletonHandle handle, String from, String to, float seconds) {
        Instance i = instance(handle);
        if (i != null && from != null && to != null) {
            i.stateData.setMix(from, to, seconds);
        }
    }

    @Override
    public void setTimeScale(SkeletonHandle handle, int trackId, float scale) {
        Instance i = instance(handle);
        if (i != null && i.state.getCurrent(trackId) != null) {
            i.state.getCurrent(trackId).setTimeScale(scale);
        }
    }

    @Override
    public void setTrackTime(SkeletonHandle handle, int trackId, float seconds) {
        Instance i = instance(handle);
        if (i != null && i.state.getCurrent(trackId) != null) {
            i.state.getCurrent(trackId).setTime(seconds);
        }
    }

    @Override
    public float animationEnd(SkeletonHandle handle, int trackId) {
        Instance i = instance(handle);
        if (i == null || i.state.getCurrent(trackId) == null) {
            return 0f;
        }
        return i.state.getCurrent(trackId).getEndTime();
    }

    @Override
    public void update(SkeletonHandle handle, float deltaSeconds) {
        Instance i = instance(handle);
        if (i != null) {
            i.state.update(deltaSeconds);
        }
    }

    @Override
    public void apply(SkeletonHandle handle) {
        Instance i = instance(handle);
        if (i != null) {
            i.state.apply(i.skeleton);
            i.skeleton.updateWorldTransform();
        }
    }

    @Override
    public BoneTransform boneTransform(SkeletonHandle handle, String boneName) {
        Instance i = instance(handle);
        if (i == null) {
            return null;
        }
        Bone bone = i.skeleton.findBone(boneName);
        if (bone == null) {
            return null;
        }
        return new BoneTransform(bone.getWorldX(), bone.getWorldY(), bone.getWorldRotationX(), bone.getWorldScaleX(), bone.getWorldScaleY());
    }

    @Override
    public void setPose(SkeletonHandle handle, float x, float y, float rotation,
            float scaleX, float scaleY, boolean flipX, boolean flipY) {
        Instance i = instance(handle);
        if (i == null) return;
        i.skeleton.setPosition(x, y);
        i.skeleton.setFlip(flipX, flipY);
        i.skeleton.getRootBone().setRotation(rotation);
        i.skeleton.getRootBone().setScale(scaleX, scaleY);
        i.skeleton.updateWorldTransform();
    }

    @Override
    public void setVisual(SkeletonHandle handle, boolean visible, float red, float green,
            float blue, float alpha) {
        Instance i = instance(handle);
        if (i != null) i.skeleton.setColor(new Color(red, green, blue, visible ? alpha : 0f));
    }

    @Override
    public void render(SkeletonHandle handle, Object batch) {
        Instance i = instance(handle);
        if (i == null || i.skeleton.getColor().a <= 0f || batch == null) {
            return;
        }
        try {
            if (renderer == null) {
                renderer = Class.forName("com.esotericsoftware.spine.SkeletonMeshRenderer")
                        .getConstructor().newInstance();
            }
            Class<?> polygonBatchType = Class.forName("com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch");
            if (meshBatch == null) meshBatch = polygonBatchType.getConstructor().newInstance();
            // ART is called with SpriteBatch, while STS1's mesh renderer requires PolygonSpriteBatch.
            // Copy the active projection so both batches address the same world coordinates.
            Object projection = batch.getClass().getMethod("getProjectionMatrix").invoke(batch);
            Class<?> matrixType = Class.forName("com.badlogic.gdx.math.Matrix4");
            polygonBatchType.getMethod("setProjectionMatrix", matrixType).invoke(meshBatch, projection);
            polygonBatchType.getMethod("begin").invoke(meshBatch);
            try {
                renderer.getClass().getMethod("draw", polygonBatchType, Skeleton.class)
                        .invoke(renderer, meshBatch, i.skeleton);
            } finally {
                polygonBatchType.getMethod("end").invoke(meshBatch);
            }
            rendererAvailable = true;
            lastRenderError = "";
        } catch (Throwable t) {
            Throwable root = t;
            if (t instanceof InvocationTargetException
                    && ((InvocationTargetException) t).getCause() != null) {
                root = ((InvocationTargetException) t).getCause();
            }
            rendererAvailable = false;
            lastRenderError = root.getClass().getSimpleName() + ": " + root.getMessage();
        }
    }

    public boolean rendererAvailable() { return rendererAvailable; }
    public String lastRenderError() { return lastRenderError; }

    /** Draw inside STS1's already-active PolygonSpriteBatch native creature render slot. */
    public boolean renderAtNativeSlot(SkeletonHandle handle, Object batch) {
        Instance i = instance(handle);
        if (i == null || i.skeleton.getColor().a <= 0f || batch == null) return false;
        try {
            if (renderer == null) {
                renderer = Class.forName("com.esotericsoftware.spine.SkeletonMeshRenderer")
                        .getConstructor().newInstance();
            }
            Class<?> polygonBatchType = Class.forName("com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch");
            if (!polygonBatchType.isInstance(batch)) return false;
            renderer.getClass().getMethod("draw", polygonBatchType, Skeleton.class)
                    .invoke(renderer, batch, i.skeleton);
            rendererAvailable = true;
            lastRenderError = "";
            return true;
        } catch (Throwable t) {
            Throwable root = t;
            if (t instanceof InvocationTargetException
                    && ((InvocationTargetException) t).getCause() != null) {
                root = ((InvocationTargetException) t).getCause();
            }
            rendererAvailable = false;
            lastRenderError = root.getClass().getSimpleName() + ": " + root.getMessage();
            return false;
        }
    }

    private Instance instance(SkeletonHandle handle) {
        if (handle == null || handle.nativeRef == null) {
            return null;
        }
        if (handle.nativeRef instanceof Instance) {
            return (Instance) handle.nativeRef;
        }
        return live.get(handle.skeletonId);
    }

    private static final class Instance {
        private final TextureAtlas atlas;
        private final Skeleton skeleton;
        private final AnimationStateData stateData;
        private final AnimationState state;

        private Instance(TextureAtlas atlas, Skeleton skeleton, AnimationStateData stateData, AnimationState state) {
            this.atlas = atlas;
            this.skeleton = skeleton;
            this.stateData = stateData;
            this.state = state;
        }
    }
}
