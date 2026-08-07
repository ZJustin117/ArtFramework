package artframework.sts1.skeleton;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Decides whether a particular native Spine draw is replaced by ART. */
public final class Sts1NativeSkeletonRenderPolicy {
    private static final Set<Object> ART_OWNED =
            Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
    private static boolean enabled;

    private Sts1NativeSkeletonRenderPolicy() {}

    public static synchronized void enable(boolean value) { enabled = value; }

    public static synchronized void claim(Object nativeSkeleton) {
        if (nativeSkeleton != null) ART_OWNED.add(nativeSkeleton);
    }

    public static synchronized void release(Object nativeSkeleton) {
        if (nativeSkeleton != null) ART_OWNED.remove(nativeSkeleton);
    }

    public static synchronized boolean suppress(Object nativeSkeleton) {
        return enabled && nativeSkeleton != null && ART_OWNED.contains(nativeSkeleton);
    }

    public static synchronized void clear() {
        enabled = false;
        ART_OWNED.clear();
    }

    public static synchronized int claimedCount() { return ART_OWNED.size(); }
}
