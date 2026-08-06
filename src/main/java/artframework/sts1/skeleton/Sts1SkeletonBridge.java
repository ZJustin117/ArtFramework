package artframework.sts1.skeleton;

import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.skeleton.SkeletonHandle;
import artframework.skeleton.BoneTransform;
import artframework.skeleton.SkeletonCommandProvider;
import artframework.skeleton.SkeletonProvider;
import artframework.skeleton.SkeletonProviders;
import artframework.skeleton.SkeletonSource;
import artframework.sts1.assets.Sts2AssetBundle;
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
    private static Sts2AssetBundle devBundle;
    private static String lastError = "";
    private static String lastDevCommand = "";
    private static BoneTransform lastBoneTransform;

    private Sts1SkeletonBridge() {}

    public static void setProviderId(String id) {
        providerId = id != null && !id.isEmpty() ? id : "fake";
    }

    public static String providerId() {
        return providerId;
    }

    public static SkeletonHandle play(String skeletonId, String atlasPath, String skeletonPath) {
        return play(skeletonId, atlasPath, skeletonPath, null);
    }

    public static SkeletonHandle play(String skeletonId, String atlasPath, String skeletonPath, Map<String, Object> params) {
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
        SkeletonSource src = new SkeletonSource(skeletonId, atlasPath, skeletonPath, params);
        SkeletonHandle h;
        try {
            h = p.load(src);
        } catch (RuntimeException e) {
            lastError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            EVENTS.add("error:" + skeletonId);
            trimEvents();
            throw e;
        }
        LIVE.put(skeletonId, h);
        EVENTS.add("play:" + skeletonId);
        trimEvents();
        return h;
    }

    public static void setDeveloperBundle(Sts2AssetBundle bundle) {
        if (devBundle != null && devBundle != bundle) {
            try {
                devBundle.close();
            } catch (Exception ignored) {
            }
        }
        devBundle = bundle;
        SkeletonProvider p = ArtFramework.skeletons().get("spine42");
        if (p instanceof Sts1Spine42Provider) {
            ((Sts1Spine42Provider) p).setAssetBundle(bundle);
        }
    }

    public static SkeletonHandle devLoad(String id, String atlasEntry, String skeletonEntry) {
        lastDevCommand = "load:" + id;
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("assetBundle", devBundle);
        params.put("atlasEntry", atlasEntry);
        params.put("skeletonEntry", skeletonEntry);
        setProviderId(Sts1Spine42Provider.ID);
        SkeletonHandle handle = play(id, atlasEntry, skeletonEntry, params);
        lastDevCommand = "loaded:" + id;
        return handle;
    }

    public static boolean devBundleConfigured() {
        return devBundle != null;
    }

    public static void noteDevCommand(String command) {
        lastDevCommand = command != null ? command : "";
    }

    public static BoneTransform devBone(String skeletonId, String boneName) {
        lastDevCommand = "bone:" + skeletonId + ":" + boneName;
        SkeletonHandle h = LIVE.get(skeletonId);
        SkeletonCommandProvider p = commandProvider(h);
        lastBoneTransform = p == null ? null : p.boneTransform(h, boneName);
        return lastBoneTransform;
    }

    public static boolean setAnimation(String skeletonId, String animationId, boolean loop) {
        SkeletonHandle h = LIVE.get(skeletonId);
        SkeletonCommandProvider p = commandProvider(h);
        if (p == null) {
            return false;
        }
        p.setAnimation(h, 0, animationId, loop);
        EVENTS.add("setAnimation:" + skeletonId + ":" + animationId);
        trimEvents();
        return true;
    }

    public static boolean addAnimation(String skeletonId, String animationId, boolean loop, float delaySeconds) {
        SkeletonHandle h = LIVE.get(skeletonId);
        SkeletonCommandProvider p = commandProvider(h);
        if (p == null) {
            return false;
        }
        p.addAnimation(h, 0, animationId, loop, delaySeconds);
        EVENTS.add("addAnimation:" + skeletonId + ":" + animationId);
        trimEvents();
        return true;
    }

    public static boolean setMix(String skeletonId, String from, String to, float seconds) {
        SkeletonHandle h = LIVE.get(skeletonId);
        SkeletonCommandProvider p = commandProvider(h);
        if (p == null) {
            return false;
        }
        p.setMix(h, from, to, seconds);
        EVENTS.add("setMix:" + skeletonId + ":" + from + "->" + to);
        trimEvents();
        return true;
    }

    public static String currentAnimation(String skeletonId) {
        SkeletonHandle h = LIVE.get(skeletonId);
        SkeletonCommandProvider p = commandProvider(h);
        return p == null ? null : p.currentAnimation(h, 0);
    }

    public static void renderAll(Object batch) {
        if (!shouldDraw() || batch == null) {
            return;
        }
        for (SkeletonHandle handle : new ArrayList<SkeletonHandle>(LIVE.values())) {
            SkeletonProvider provider = ArtFramework.skeletons().get(handle.providerId);
            if (provider instanceof SkeletonCommandProvider) {
                ((SkeletonCommandProvider) provider).render(handle, batch);
            }
        }
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
        m.put("lastError", lastError);
        m.put("lastDevCommand", lastDevCommand);
        if (lastBoneTransform != null) {
            Map<String, Object> bone = new LinkedHashMap<String, Object>();
            bone.put("x", Float.valueOf(lastBoneTransform.x));
            bone.put("y", Float.valueOf(lastBoneTransform.y));
            bone.put("rotationDegrees", Float.valueOf(lastBoneTransform.rotationDegrees));
            bone.put("scaleX", Float.valueOf(lastBoneTransform.scaleX));
            bone.put("scaleY", Float.valueOf(lastBoneTransform.scaleY));
            m.put("lastBoneTransform", bone);
        }
        List<String> ids = new ArrayList<String>(LIVE.keySet());
        m.put("liveIds", ids);
        Map<String, Object> live = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, SkeletonHandle> entry : LIVE.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("providerId", entry.getValue().providerId);
            item.put("alive", Boolean.valueOf(entry.getValue().isAlive()));
            SkeletonProvider liveProvider = ArtFramework.skeletons().get(entry.getValue().providerId);
            if (liveProvider instanceof SkeletonCommandProvider) {
                item.put("currentAnimation", ((SkeletonCommandProvider) liveProvider).currentAnimation(entry.getValue(), 0));
            }
            live.put(entry.getKey(), item);
        }
        m.put("live", live);
        SkeletonProvider p = ArtFramework.skeletons().get(providerId);
        m.put("commandProvider", Boolean.valueOf(p instanceof SkeletonCommandProvider));
        if (p instanceof Sts1Spine42Provider) {
            Sts1Spine42Provider s42 = (Sts1Spine42Provider) p;
            m.put("spine42Available", Boolean.valueOf(s42.isAvailable()));
            m.put("spine42BundleConfigured", Boolean.valueOf(s42.assetBundle() != null));
            m.put("spine42RuntimeClass", s42.runtimeClassName());
            m.put("spine42UnavailableReason", s42.unavailableReason());
        }
        return m;
    }

    public static void resetForTests() {
        stopAll();
        LIVE.clear();
        EVENTS.clear();
        lastError = "";
        lastDevCommand = "";
        lastBoneTransform = null;
        if (devBundle != null) {
            try {
                devBundle.close();
            } catch (Exception ignored) {
            }
        }
        devBundle = null;
        providerId = "fake";
    }

    private static void trimEvents() {
        while (EVENTS.size() > 32) {
            EVENTS.remove(0);
        }
    }

    private static SkeletonCommandProvider commandProvider(SkeletonHandle h) {
        if (h == null) {
            return null;
        }
        SkeletonProvider p = ArtFramework.skeletons().get(h.providerId);
        return p instanceof SkeletonCommandProvider ? (SkeletonCommandProvider) p : null;
    }
}
