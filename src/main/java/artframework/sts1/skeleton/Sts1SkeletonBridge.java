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
import artframework.ecs.PresentationWorld;
import artframework.skeleton.SkeletonPresentationSystem;
import artframework.skeleton.SkeletonPresentationView;
import artframework.skeleton.SkeletonPresentationFrames;
import artframework.core.SignalDecision;
import artframework.core.SignalListener;
import artframework.core.SignalSubscription;
import artframework.core.UiSignal;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.Skeleton;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.Set;

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
    private static final SkeletonPresentationSystem PRESENTATION =
            new SkeletonPresentationSystem(new PresentationWorld("sts1-skeleton"), ArtFramework.skeletons());
    private static SignalSubscription presentationSubscription;
    private static final Map<AbstractCreature, NativeCreature> NATIVE_CREATURES =
            new IdentityHashMap<AbstractCreature, NativeCreature>();
    private static final Map<Skeleton, String> NATIVE_SKELETONS =
            new IdentityHashMap<Skeleton, String>();
    private static long nextNativeEntityId;

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

    /** Load a host-bundled STS1 Spine asset through the selected provider. */
    public static SkeletonHandle sts1Load(String id, String atlasPath, String skeletonPath) {
        setProviderId(Sts1Spine34Provider.ID);
        SkeletonHandle handle = play(id, atlasPath, skeletonPath, null);
        SkeletonCommandProvider provider = commandProvider(handle);
        if (provider != null) {
            provider.setPose(handle,
                    com.megacrit.cardcrawl.core.Settings.WIDTH * 0.5f,
                    com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.48f,
                    0f, 1f, 1f, false, false);
        }
        return handle;
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
        PRESENTATION.renderAllExcept(batch, new HashSet<String>(NATIVE_SKELETONS.values()));
    }

    /** Apply a backend snapshot without exposing Spine or libGDX types to the backend. */
    public static void syncPresentation(long frameId, List<SkeletonPresentationView> views) {
        if (artframework.sts1.PresentSafety.isPanic()) return;
        PRESENTATION.sync(frameId, views);
        reconcileNativeClaims(views);
    }

    /** Subscribe once to the host-neutral backend snapshot signal. */
    public static synchronized void installPresentationSignals() {
        if (presentationSubscription != null && presentationSubscription.isConnected()) return;
        presentationSubscription = artframework.core.SignalBuses.get().connect(
                SkeletonPresentationFrames.UPDATED, new SignalListener() {
                    @Override public SignalDecision onSignal(UiSignal signal) {
                        if (!(signal.payload instanceof SkeletonPresentationFrames.Frame)) {
                            return SignalDecision.stopRejected("skeleton presentation frame required");
                        }
                        SkeletonPresentationFrames.Frame frame =
                                (SkeletonPresentationFrames.Frame) signal.payload;
                        syncPresentation(frame.frameId, frame.views);
                        return SignalDecision.continueSignal();
                    }
                });
    }

    public static SkeletonPresentationSystem presentationSystem() {
        return PRESENTATION;
    }

    public static void tick(float deltaSeconds) {
        PRESENTATION.tick(deltaSeconds);
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
        PRESENTATION.clear();
        NATIVE_CREATURES.clear();
        NATIVE_SKELETONS.clear();
        Sts1NativeSkeletonRenderPolicy.clear();
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

    /** Records the host asset source immediately after STS1 creates a creature's native skeleton. */
    public static synchronized void observeNativeCreature(AbstractCreature creature, String atlasPath,
            String skeletonPath) {
        if (creature == null || empty(atlasPath) || empty(skeletonPath)) return;
        Skeleton skeleton = nativeSkeleton(creature);
        if (skeleton == null) return;
        NativeCreature current = NATIVE_CREATURES.get(creature);
        if (current == null || current.skeleton != skeleton) {
            current = new NativeCreature("sts1-creature-" + (++nextNativeEntityId), skeleton,
                    atlasPath, skeletonPath);
            NATIVE_CREATURES.put(creature, current);
        } else {
            current.atlasPath = atlasPath;
            current.skeletonPath = skeletonPath;
        }
    }

    /** Creates a host-neutral snapshot; null means the creature has no captured Spine source. */
    public static synchronized SkeletonPresentationView presentationView(AbstractCreature creature) {
        NativeCreature nativeCreature = NATIVE_CREATURES.get(creature);
        if (nativeCreature == null || nativeCreature.skeleton != nativeSkeleton(creature)) return null;
        AnimationState state = creature.state;
        String animation = "";
        boolean loop = true;
        float trackTime = 0f;
        if (state != null && state.getCurrent(0) != null) {
            AnimationState.TrackEntry entry = state.getCurrent(0);
            if (entry.getAnimation() != null) animation = entry.getAnimation().getName();
            loop = entry.getLoop();
            trackTime = Math.max(0f, entry.getTime());
        }
        com.badlogic.gdx.graphics.Color color = creature.tint != null && creature.tint.color != null
                ? creature.tint.color : com.badlogic.gdx.graphics.Color.WHITE;
        return new SkeletonPresentationView(nativeCreature.entityKey,
                new artframework.skeleton.SkeletonAssetComponent(
                        Sts1Spine34Provider.ID, nativeCreature.atlasPath, nativeCreature.skeletonPath,
                        "", 1f),
                new artframework.skeleton.SkeletonPoseComponent(creature.drawX + creature.animX,
                        creature.drawY + creature.animY, 0f, 1f, 1f, creature.flipHorizontal,
                        creature.flipVertical, 0),
                new artframework.skeleton.SkeletonAnimationComponent(0, animation, loop, 1f,
                        trackTime, "", "", 0f),
                new artframework.skeleton.SkeletonVisualComponent(!creature.isDeadOrEscaped(), color.r,
                        color.g, color.b, color.a));
    }

    /** Called from the native mesh-render patch. Returns false to retain STS1's original draw. */
    public static synchronized boolean renderClaimedNative(Skeleton nativeSkeleton, Object activeBatch) {
        String entityKey = NATIVE_SKELETONS.get(nativeSkeleton);
        if (entityKey == null || !Sts1NativeSkeletonRenderPolicy.suppress(nativeSkeleton)) return false;
        artframework.skeleton.SkeletonRuntimeBinding binding = PRESENTATION.binding(entityKey);
        if (binding == null || !binding.handle.isAlive()) return false;
        SkeletonProvider provider = ArtFramework.skeletons().get(binding.handle.providerId);
        return provider instanceof Sts1Spine34Provider
                && ((Sts1Spine34Provider) provider).renderAtNativeSlot(binding.handle, activeBatch);
    }

    private static void reconcileNativeClaims(List<SkeletonPresentationView> views) {
        Set<String> active = new HashSet<String>();
        if (views != null) {
            for (SkeletonPresentationView view : views) active.add(view.entityKey);
        }
        NATIVE_SKELETONS.clear();
        Sts1NativeSkeletonRenderPolicy.clear();
        if (!shouldDraw()) return;
        for (java.util.Iterator<Map.Entry<AbstractCreature, NativeCreature>> iterator =
                NATIVE_CREATURES.entrySet().iterator(); iterator.hasNext();) {
            if (!active.contains(iterator.next().getValue().entityKey)) iterator.remove();
        }
        for (NativeCreature creature : NATIVE_CREATURES.values()) {
            if (active.contains(creature.entityKey) && PRESENTATION.binding(creature.entityKey) != null) {
                NATIVE_SKELETONS.put(creature.skeleton, creature.entityKey);
                Sts1NativeSkeletonRenderPolicy.claim(creature.skeleton);
            }
        }
        Sts1NativeSkeletonRenderPolicy.enable(!NATIVE_SKELETONS.isEmpty());
    }

    private static Skeleton nativeSkeleton(AbstractCreature creature) {
        try {
            java.lang.reflect.Field field = AbstractCreature.class.getDeclaredField("skeleton");
            field.setAccessible(true);
            Object value = field.get(creature);
            return value instanceof Skeleton ? (Skeleton) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean empty(String value) { return value == null || value.isEmpty(); }

    private static final class NativeCreature {
        private final String entityKey;
        private final Skeleton skeleton;
        private String atlasPath;
        private String skeletonPath;

        private NativeCreature(String entityKey, Skeleton skeleton, String atlasPath, String skeletonPath) {
            this.entityKey = entityKey;
            this.skeleton = skeleton;
            this.atlasPath = atlasPath;
            this.skeletonPath = skeletonPath;
        }
    }

    public static Map<String, Object> probeSlice() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("providerId", providerId);
        m.put("liveCount", Integer.valueOf(LIVE.size()));
        m.put("shouldDraw", Boolean.valueOf(shouldDraw()));
        m.put("presentLevel", FullPresentMode.skeletonLevel().name());
        m.put("nativeCreatureCount", Integer.valueOf(NATIVE_CREATURES.size()));
        m.put("nativeClaimedCount", Integer.valueOf(Sts1NativeSkeletonRenderPolicy.claimedCount()));
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
        if (!LIVE.isEmpty()) {
            SkeletonHandle first = LIVE.values().iterator().next();
            SkeletonProvider firstProvider = ArtFramework.skeletons().get(first.providerId);
            if (firstProvider instanceof SkeletonCommandProvider) {
                m.put("currentAnimation", ((SkeletonCommandProvider) firstProvider).currentAnimation(first, 0));
            }
        }
        SkeletonProvider p = ArtFramework.skeletons().get(providerId);
        m.put("commandProvider", Boolean.valueOf(p instanceof SkeletonCommandProvider));
        if (p instanceof Sts1Spine42Provider) {
            Sts1Spine42Provider s42 = (Sts1Spine42Provider) p;
            m.put("spine42Available", Boolean.valueOf(s42.isAvailable()));
            m.put("spine42BundleConfigured", Boolean.valueOf(s42.assetBundle() != null));
            m.put("spine42RuntimeClass", s42.runtimeClassName());
            m.put("spine42UnavailableReason", s42.unavailableReason());
        }
        SkeletonProvider spine34 = ArtFramework.skeletons().get(Sts1Spine34Provider.ID);
        if (spine34 instanceof Sts1Spine34Provider) {
            Sts1Spine34Provider s34 = (Sts1Spine34Provider) spine34;
            m.put("spine34RendererAvailable", Boolean.valueOf(s34.rendererAvailable()));
            m.put("spine34LastRenderError", s34.lastRenderError());
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
