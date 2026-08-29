package artframework.sts1.skeleton;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.c2.EntityAnchorView;
import artframework.c2.EntityKind;
import artframework.component.Rect;
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
import artframework.presentation.PresentationRegistry;
import artframework.skeleton.SkeletonPresentationSystem;
import artframework.skeleton.SkeletonHostTickSystem;
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
            new SkeletonPresentationSystem(
                    PresentationRegistry.context("c2-skeleton").world(), ArtFramework.skeletons());
    private static SignalSubscription presentationSubscription;
    private static final Map<Object, NativeCreature> NATIVE_CREATURES =
            new IdentityHashMap<Object, NativeCreature>();
    private static final Map<Skeleton, String> NATIVE_SKELETONS =
            new IdentityHashMap<Skeleton, String>();
    private static final Set<String> PROJECTED_NATIVE_ENTITY_SLOTS = new HashSet<String>();
    private static long nextNativeEntityId;
    private static int nativeClaimAttempts;
    private static int nativeClaimReleases;
    private static int duplicateNativeClaims;

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
        projectNativeEntityAnchors();
    }

    /** Subscribe once to the host-neutral backend snapshot signal. */
    public static synchronized void installPresentationSignals() {
        if (presentationSubscription != null && presentationSubscription.isConnected()) return;
        presentationSubscription = artframework.core.SignalGroups.nativeGroup().connect(
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
        cleanupProjectedNativeEntitySlots();
        NATIVE_CREATURES.clear();
        NATIVE_SKELETONS.clear();
        Sts1NativeSkeletonRenderPolicy.clear();
    }

    /** Recreate provider-owned handles from ECS without discarding presentation state. */
    public static synchronized void onHostRecreated() {
        for (String id : new ArrayList<String>(LIVE.keySet())) stop(id);
        PRESENTATION.recreateHostBindings();
        reconcileNativeClaims(PRESENTATION.views());
        projectNativeEntityAnchors();
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
        updateNativeCreatureAnchor(current, creature);
    }

    /** Test-only helper to register a native creature mapping without instantiating AbstractCreature. */
    static synchronized void observeNativeSkeletonForTests(Skeleton skeleton, String entityKey,
            String atlasPath, String skeletonPath) {
        if (skeleton == null || entityKey == null || entityKey.isEmpty()) return;
        NATIVE_CREATURES.put(new Object(),
                new NativeCreature(entityKey, skeleton, atlasPath, skeletonPath));
    }

    /** Test-only helper to register a native skeleton with complete host-neutral anchor metadata. */
    static synchronized void observeNativeSkeletonForTests(Skeleton skeleton, String entityKey,
            EntityKind kind, String name, float x, float y, float width, float height,
            boolean visible, String atlasPath, String skeletonPath) {
        if (skeleton == null || entityKey == null || entityKey.isEmpty()) return;
        NativeCreature nativeCreature = new NativeCreature(entityKey, skeleton, atlasPath, skeletonPath);
        nativeCreature.kind = kind != null ? kind : EntityKind.MONSTER;
        nativeCreature.name = name != null ? name : "";
        nativeCreature.x = x;
        nativeCreature.y = y;
        nativeCreature.bounds = new Rect(x - width * 0.5f, y, Math.max(0f, width), Math.max(0f, height));
        nativeCreature.visible = visible;
        nativeCreature.assetId = assetIdFor(atlasPath, skeletonPath, entityKey);
        NATIVE_CREATURES.put(new Object(), nativeCreature);
    }

    /** Creates a host-neutral snapshot; null means the creature has no captured Spine source. */
    public static synchronized SkeletonPresentationView presentationView(AbstractCreature creature) {
        NativeCreature nativeCreature = NATIVE_CREATURES.get(creature);
        if (nativeCreature == null || nativeCreature.skeleton != nativeSkeleton(creature)) return null;
        updateNativeCreatureAnchor(nativeCreature, creature);
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

    /** Dependency-neutral observation anchor; claimed reflects exact per-instance skeleton ownership. */
    public static synchronized EntityAnchorView anchorView(AbstractCreature creature) {
        NativeCreature nativeCreature = NATIVE_CREATURES.get(creature);
        if (nativeCreature == null || nativeCreature.skeleton != nativeSkeleton(creature)) return null;
        updateNativeCreatureAnchor(nativeCreature, creature);
        return anchorFor(nativeCreature);
    }

    public static synchronized EntityAnchorView nativeAnchorView(Skeleton nativeSkeleton) {
        NativeCreature nativeCreature = nativeCreature(nativeSkeleton);
        return nativeCreature != null ? anchorFor(nativeCreature) : null;
    }

    public static synchronized List<EntityAnchorView> nativeAnchorViews() {
        List<EntityAnchorView> anchors = new ArrayList<EntityAnchorView>();
        for (NativeCreature creature : NATIVE_CREATURES.values()) {
            anchors.add(anchorFor(creature));
        }
        return java.util.Collections.unmodifiableList(anchors);
    }

    /** Called from the native mesh-render patch. Returns false to retain STS1's original draw. */
    public static synchronized boolean renderClaimedNative(Skeleton nativeSkeleton, Object activeBatch) {
        String entityKey = NATIVE_SKELETONS.get(nativeSkeleton);
        if (!shouldDraw() || entityKey == null || !Sts1NativeSkeletonRenderPolicy.suppress(nativeSkeleton)) return false;
        artframework.skeleton.SkeletonRuntimeBinding binding = PRESENTATION.binding(entityKey);
        if (binding == null || !binding.handle.isAlive()) return false;
        SkeletonProvider provider = ArtFramework.skeletons().get(binding.handle.providerId);
        return provider instanceof SkeletonNativeSlotRenderer
                && ((SkeletonNativeSlotRenderer) provider).renderAtNativeSlot(binding.handle, activeBatch);
    }

    /** Whether ART has a complete native-slot presentation for this exact Spine instance. */
    public static synchronized boolean canRenderClaimedNative(Skeleton nativeSkeleton) {
        String entityKey = NATIVE_SKELETONS.get(nativeSkeleton);
        if (!shouldDraw() || entityKey == null || !Sts1NativeSkeletonRenderPolicy.suppress(nativeSkeleton)) return false;
        artframework.skeleton.SkeletonRuntimeBinding binding = PRESENTATION.binding(entityKey);
        if (binding == null || !binding.handle.isAlive()) return false;
        SkeletonProvider provider = ArtFramework.skeletons().get(binding.handle.providerId);
        return provider instanceof SkeletonNativeSlotRenderer;
    }

    public static synchronized String nativeEntityKey(Skeleton nativeSkeleton) {
        return NATIVE_SKELETONS.get(nativeSkeleton);
    }

    private static void reconcileNativeClaims(List<SkeletonPresentationView> views) {
        Set<Skeleton> previouslyClaimed = java.util.Collections.newSetFromMap(
                new IdentityHashMap<Skeleton, Boolean>());
        previouslyClaimed.addAll(NATIVE_SKELETONS.keySet());
        Set<String> active = new HashSet<String>();
        if (views != null) {
            for (SkeletonPresentationView view : views) active.add(view.entityKey);
        }
        NATIVE_SKELETONS.clear();
        Sts1NativeSkeletonRenderPolicy.clear();
        for (java.util.Iterator<Map.Entry<Object, NativeCreature>> iterator =
                NATIVE_CREATURES.entrySet().iterator(); iterator.hasNext();) {
            if (!active.contains(iterator.next().getValue().entityKey)) iterator.remove();
        }
        if (shouldDraw()) {
            Set<String> claimedEntityKeys = new HashSet<String>();
            for (NativeCreature creature : NATIVE_CREATURES.values()) {
                if (active.contains(creature.entityKey) && PRESENTATION.binding(creature.entityKey) != null) {
                    if (!claimedEntityKeys.add(creature.entityKey)) {
                        duplicateNativeClaims++;
                        continue;
                    }
                    NATIVE_SKELETONS.put(creature.skeleton, creature.entityKey);
                    Sts1NativeSkeletonRenderPolicy.claim(creature.skeleton);
                    if (!previouslyClaimed.contains(creature.skeleton)) nativeClaimAttempts++;
                }
            }
        }
        for (Skeleton skeleton : previouslyClaimed) {
            if (!NATIVE_SKELETONS.containsKey(skeleton)) nativeClaimReleases++;
        }
        Sts1NativeSkeletonRenderPolicy.enable(!NATIVE_SKELETONS.isEmpty());
    }

    private static void projectNativeEntityAnchors() {
        Set<String> activeSlots = new HashSet<String>();
        for (EntityAnchorView anchor : nativeAnchorViews()) {
            String slotId = nativeEntitySlotId(anchor.entityId);
            activeSlots.add(slotId);
            try {
                ArtFramework.entities().present(slotId, anchor.kind.name().toLowerCase(),
                        anchor.entityId, anchor.toSnapshot(), anchor.x, anchor.y, 1f);
                PROJECTED_NATIVE_ENTITY_SLOTS.add(slotId);
            } catch (RuntimeException ignored) {
                // Anchor projection is non-authoritative observation; native pixels remain fail-open.
            }
        }
        for (String slotId : new HashSet<String>(PROJECTED_NATIVE_ENTITY_SLOTS)) {
            if (!activeSlots.contains(slotId)) {
                ArtFramework.entities().detach(slotId);
                PROJECTED_NATIVE_ENTITY_SLOTS.remove(slotId);
            }
        }
    }

    private static void cleanupProjectedNativeEntitySlots() {
        for (String slotId : new HashSet<String>(PROJECTED_NATIVE_ENTITY_SLOTS)) {
            ArtFramework.entities().detach(slotId);
        }
        PROJECTED_NATIVE_ENTITY_SLOTS.clear();
    }

    private static String nativeEntitySlotId(String entityKey) {
        return "sts1.native.skeleton/" + entityKey;
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

    private static NativeCreature nativeCreature(Skeleton skeleton) {
        if (skeleton == null) return null;
        for (NativeCreature creature : NATIVE_CREATURES.values()) {
            if (creature.skeleton == skeleton) return creature;
        }
        return null;
    }

    private static EntityAnchorView anchorFor(NativeCreature creature) {
        return new EntityAnchorView(creature.entityKey, creature.kind, creature.name,
                creature.x, creature.y, creature.bounds, creature.visible,
                NATIVE_SKELETONS.containsKey(creature.skeleton), creature.assetId);
    }

    private static void updateNativeCreatureAnchor(NativeCreature nativeCreature,
            AbstractCreature creature) {
        if (nativeCreature == null || creature == null) return;
        nativeCreature.kind = creature instanceof com.megacrit.cardcrawl.characters.AbstractPlayer
                ? EntityKind.PLAYER : EntityKind.MONSTER;
        nativeCreature.name = strField(creature, "name", creature.getClass().getSimpleName());
        nativeCreature.x = creature.drawX + creature.animX;
        nativeCreature.y = creature.drawY + creature.animY;
        nativeCreature.bounds = boundsFromCreature(creature, nativeCreature.x, nativeCreature.y);
        nativeCreature.visible = !creature.isDeadOrEscaped();
        nativeCreature.assetId = assetIdFor(nativeCreature.atlasPath, nativeCreature.skeletonPath,
                nativeCreature.entityKey);
    }

    private static Rect boundsFromCreature(AbstractCreature creature, float x, float y) {
        Object hb = field(creature, "hb");
        if (hb != null) {
            return new Rect(floatField(hb, "x", x - 60f), floatField(hb, "y", y),
                    Math.max(0f, floatField(hb, "width", 120f)),
                    Math.max(0f, floatField(hb, "height", 160f)));
        }
        return new Rect(x - 60f, y, 120f, 160f);
    }

    private static String assetIdFor(String atlasPath, String skeletonPath, String entityKey) {
        if (ResourceIds.isValid(atlasPath) && atlasPath.startsWith(ResourceIds.CHAR_PREFIX)) {
            return atlasPath;
        }
        String base = !empty(skeletonPath) ? skeletonPath : entityKey;
        if (empty(base)) return ResourceIds.CHAR_UNKNOWN;
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0 && slash + 1 < base.length()) base = base.substring(slash + 1);
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        base = base.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return !base.isEmpty() ? ResourceIds.CHAR_PREFIX + "skeleton." + base : ResourceIds.CHAR_UNKNOWN;
    }

    private static Object field(Object target, String name) {
        try {
            java.lang.reflect.Field f = target.getClass().getField(name);
            return f.get(target);
        } catch (Throwable ignored) {
            try {
                java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (Throwable ignoredAgain) {
                return null;
            }
        }
    }

    private static String strField(Object target, String name, String fallback) {
        Object value = field(target, name);
        return value != null ? String.valueOf(value) : fallback;
    }

    private static float floatField(Object target, String name, float fallback) {
        Object value = field(target, name);
        return value instanceof Number ? ((Number) value).floatValue() : fallback;
    }

    private static boolean empty(String value) { return value == null || value.isEmpty(); }

    private static final class NativeCreature {
        private final String entityKey;
        private final Skeleton skeleton;
        private String atlasPath;
        private String skeletonPath;
        private EntityKind kind = EntityKind.MONSTER;
        private String name = "";
        private float x;
        private float y;
        private Rect bounds = Rect.ZERO;
        private boolean visible = true;
        private String assetId = ResourceIds.CHAR_UNKNOWN;

        private NativeCreature(String entityKey, Skeleton skeleton, String atlasPath, String skeletonPath) {
            this.entityKey = entityKey;
            this.skeleton = skeleton;
            this.atlasPath = atlasPath;
            this.skeletonPath = skeletonPath;
            this.assetId = assetIdFor(atlasPath, skeletonPath, entityKey);
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
        m.put("nativeAnchorCount", Integer.valueOf(NATIVE_CREATURES.size()));
        m.put("nativeClaimAttempts", Integer.valueOf(nativeClaimAttempts));
        m.put("nativeClaimReleases", Integer.valueOf(nativeClaimReleases));
        m.put("duplicateNativeClaims", Integer.valueOf(duplicateNativeClaims));
        List<Map<String, Object>> anchors = new ArrayList<Map<String, Object>>();
        for (EntityAnchorView anchor : nativeAnchorViews()) anchors.add(anchor.toMap());
        m.put("nativeAnchors", anchors);
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
        nextNativeEntityId = 0L;
        nativeClaimAttempts = 0;
        nativeClaimReleases = 0;
        duplicateNativeClaims = 0;
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
