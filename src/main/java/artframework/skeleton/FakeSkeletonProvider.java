package artframework.skeleton;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * In-memory skeleton provider for pure JUnit.
 */
public final class FakeSkeletonProvider implements SkeletonCommandProvider {

    public static final String ID = "fake";

    private final Map<String, SkeletonHandle> loaded = new LinkedHashMap<String, SkeletonHandle>();
    private final Map<String, FakeState> states = new LinkedHashMap<String, FakeState>();
    private int loadCount;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public SkeletonHandle load(SkeletonSource source) {
        loadCount++;
        SkeletonHandle h = new SkeletonHandle(ID, source.skeletonId, source);
        loaded.put(source.skeletonId, h);
        states.put(source.skeletonId, new FakeState(source.params));
        return h;
    }

    @Override
    public void unload(SkeletonHandle handle) {
        if (handle != null) {
            handle.markDisposed();
            loaded.remove(handle.skeletonId);
            states.remove(handle.skeletonId);
        }
    }

    public int loadCount() {
        return loadCount;
    }

    public int liveCount() {
        return loaded.size();
    }

    @Override
    public boolean hasAnimation(SkeletonHandle handle, String animationId) {
        FakeState s = state(handle);
        return s != null && (s.animations.isEmpty() || s.animations.contains(animationId));
    }

    @Override
    public void setAnimation(SkeletonHandle handle, int trackId, String animationId, boolean loop) {
        FakeState s = state(handle);
        if (s != null) {
            s.current = animationId;
            s.loop = loop;
            s.events.append("set:").append(animationId).append(':').append(loop).append(';');
        }
    }

    @Override
    public void addAnimation(SkeletonHandle handle, int trackId, String animationId, boolean loop, float delaySeconds) {
        FakeState s = state(handle);
        if (s != null) {
            s.events.append("add:").append(animationId).append(':').append(loop).append(':').append(delaySeconds).append(';');
        }
    }

    @Override
    public String currentAnimation(SkeletonHandle handle, int trackId) {
        FakeState s = state(handle);
        return s != null ? s.current : null;
    }

    @Override
    public void setMix(SkeletonHandle handle, String from, String to, float seconds) {
        FakeState s = state(handle);
        if (s != null) {
            s.mixes.put(SkeletonMixTable.key(from, to), Float.valueOf(seconds));
        }
    }

    @Override
    public void setTimeScale(SkeletonHandle handle, int trackId, float scale) {
        FakeState s = state(handle);
        if (s != null) {
            s.timeScale = scale;
        }
    }

    @Override
    public void setTrackTime(SkeletonHandle handle, int trackId, float seconds) {
        FakeState s = state(handle);
        if (s != null) {
            s.trackTime = seconds;
        }
    }

    @Override
    public void setPose(SkeletonHandle handle, float x, float y, float rotation,
            float scaleX, float scaleY, boolean flipX, boolean flipY) {
        FakeState s = state(handle);
        if (s != null) {
            s.x = x; s.y = y; s.rotation = rotation;
            s.scaleX = scaleX; s.scaleY = scaleY;
            s.flipX = flipX; s.flipY = flipY;
        }
    }

    public float x(String id) { return states.get(id).x; }
    public float y(String id) { return states.get(id).y; }

    @Override
    public float animationEnd(SkeletonHandle handle, int trackId) {
        FakeState s = state(handle);
        return s != null ? s.animationEnd : 0f;
    }

    @Override
    public void update(SkeletonHandle handle, float deltaSeconds) {
        FakeState s = state(handle);
        if (s != null) {
            s.updated = true;
        }
    }

    @Override
    public void apply(SkeletonHandle handle) {
        FakeState s = state(handle);
        if (s != null) {
            s.applied = true;
        }
    }

    @Override
    public BoneTransform boneTransform(SkeletonHandle handle, String boneName) {
        return new BoneTransform(0f, 0f, 0f, 1f, 1f);
    }

    public String events(String skeletonId) {
        FakeState s = states.get(skeletonId);
        return s != null ? s.events.toString() : "";
    }

    public Float mix(String skeletonId, String from, String to) {
        FakeState s = states.get(skeletonId);
        return s != null ? s.mixes.get(SkeletonMixTable.key(from, to)) : null;
    }

    public float timeScale(String skeletonId) {
        FakeState s = states.get(skeletonId);
        return s != null ? s.timeScale : 0f;
    }

    public float trackTime(String skeletonId) {
        FakeState s = states.get(skeletonId);
        return s != null ? s.trackTime : 0f;
    }

    public boolean applied(String skeletonId) {
        FakeState s = states.get(skeletonId);
        return s != null && s.applied;
    }

    private FakeState state(SkeletonHandle handle) {
        return handle == null ? null : states.get(handle.skeletonId);
    }

    private static final class FakeState {
        private final Set<String> animations = new LinkedHashSet<String>();
        private final Map<String, Float> mixes = new LinkedHashMap<String, Float>();
        private final StringBuilder events = new StringBuilder();
        private String current;
        private boolean loop;
        private float timeScale = 1f;
        private float trackTime;
        private float animationEnd = 2f;
        private boolean updated;
        private boolean applied;
        private float x, y, rotation, scaleX = 1f, scaleY = 1f;
        private boolean flipX, flipY;

        @SuppressWarnings("unchecked")
        private FakeState(Map<String, Object> params) {
            if (params != null) {
                Object anims = params.get("animations");
                if (anims instanceof Iterable) {
                    for (Object a : (Iterable<Object>) anims) {
                        if (a != null) {
                            animations.add(String.valueOf(a));
                        }
                    }
                }
                Object end = params.get("animationEnd");
                if (end instanceof Number) {
                    animationEnd = ((Number) end).floatValue();
                }
            }
        }
    }
}
