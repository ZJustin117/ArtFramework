package artframework.skeleton;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory skeleton provider for pure JUnit.
 */
public final class FakeSkeletonProvider implements SkeletonProvider {

    public static final String ID = "fake";

    private final Map<String, SkeletonHandle> loaded = new LinkedHashMap<String, SkeletonHandle>();
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
        return h;
    }

    @Override
    public void unload(SkeletonHandle handle) {
        if (handle != null) {
            handle.markDisposed();
            loaded.remove(handle.skeletonId);
        }
    }

    public int loadCount() {
        return loadCount;
    }

    public int liveCount() {
        return loaded.size();
    }
}
