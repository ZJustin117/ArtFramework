package artframework.sts1.skeleton;

import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.skeleton.SkeletonHandle;
import artframework.skeleton.SkeletonProvider;
import artframework.skeleton.SkeletonProviders;
import artframework.skeleton.SkeletonSource;
import artframework.sts1.FullPresentMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * STS1 skeleton surface bridge (16.8): load/play/stop via SkeletonProvider SPI; lifecycle clears
 * on host recreate / panic.
 */
public final class Sts1SkeletonBridge {

    private static String providerId = "fake";
    private static final Map<String, SkeletonHandle> LIVE = new LinkedHashMap<String, SkeletonHandle>();
    private static final List<String> EVENTS = new ArrayList<String>();

    private Sts1SkeletonBridge() {}

    public static void setProviderId(String id) {
        providerId = id != null && !id.isEmpty() ? id : "fake";
    }

    public static String providerId() {
        return providerId;
    }

    public static SkeletonHandle play(String skeletonId, String atlasPath, String skeletonPath) {
        if (artframework.sts1.PresentSafety.isPanic()) {
            return null;
        }
        if (!FullPresentMode.skeletonLevel().allowsObserve()
                && !FullPresentMode.skeletonLevel().allowsFullPresent()) {
            // still allow load for tests when surface mounted
        }
        SkeletonProviders reg = ArtFramework.skeletons();
        SkeletonProvider p = reg.get(providerId);
        if (p == null) {
            throw new IllegalStateException("skeleton provider not registered: " + providerId);
        }
        SkeletonSource src = new SkeletonSource(skeletonId, atlasPath, skeletonPath, null);
        SkeletonHandle h = p.load(src);
        LIVE.put(skeletonId, h);
        EVENTS.add("play:" + skeletonId);
        trimEvents();
        return h;
    }

    public static void stop(String skeletonId) {
        SkeletonHandle h = LIVE.remove(skeletonId);
        if (h == null) {
            return;
        }
        SkeletonProvider p = ArtFramework.skeletons().get(h.providerId);
        if (p != null) {
            p.unload(h);
        } else {
            h.markDisposed();
        }
        EVENTS.add("stop:" + skeletonId);
        trimEvents();
    }

    public static void stopAll() {
        List<String> ids = new ArrayList<String>(LIVE.keySet());
        for (String id : ids) {
            stop(id);
        }
    }

    public static int liveCount() {
        return LIVE.size();
    }

    public static boolean shouldDraw() {
        return FullPresentMode.skeletonLevel().allowsFullPresent()
                && ArtFramework.component(SurfaceIds.SKELETON) != null
                && ArtFramework.component(SurfaceIds.SKELETON).isMounted()
                && !artframework.sts1.PresentSafety.isPanic();
    }

    public static Map<String, Object> probeSlice() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("providerId", providerId);
        m.put("liveCount", Integer.valueOf(LIVE.size()));
        m.put("shouldDraw", Boolean.valueOf(shouldDraw()));
        m.put("presentLevel", FullPresentMode.skeletonLevel().name());
        m.put("events", new ArrayList<String>(EVENTS));
        List<String> ids = new ArrayList<String>(LIVE.keySet());
        m.put("liveIds", ids);
        return m;
    }

    public static void resetForTests() {
        stopAll();
        LIVE.clear();
        EVENTS.clear();
        providerId = "fake";
    }

    private static void trimEvents() {
        while (EVENTS.size() > 32) {
            EVENTS.remove(0);
        }
    }
}
