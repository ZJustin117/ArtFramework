package artframework.skeleton;

import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import artframework.core.SignalBuses;
import artframework.core.UiSignal;

/** Applies immutable skeleton presentation views to provider-owned runtime instances. */
public final class SkeletonPresentationSystem {
    private final PresentationWorld world;
    private final SkeletonProviders providers;
    // Native handles are provider-owned implementation cache, keyed solely by ART entity identity.
    private final Map<EntityId, SkeletonRuntimeBinding> bindings =
            new LinkedHashMap<EntityId, SkeletonRuntimeBinding>();

    public SkeletonPresentationSystem(PresentationWorld world, SkeletonProviders providers) {
        if (world == null || providers == null) throw new IllegalArgumentException("world and providers required");
        this.world = world; this.providers = providers;
    }

    public void sync(long frameId, List<SkeletonPresentationView> views) {
        EntityId syncEntity = syncEntity();
        SkeletonSyncFrameComponent last = world.get(syncEntity, SkeletonSyncFrameComponent.class);
        if (last != null && frameId < last.frameId) return;
        world.put(syncEntity, SkeletonSyncFrameComponent.class, new SkeletonSyncFrameComponent(frameId));
        List<SkeletonPresentationView> safe = views != null ? views : new ArrayList<SkeletonPresentationView>();
        Set<EntityId> seen = new HashSet<EntityId>();
        for (SkeletonPresentationView view : safe) {
            EntityId id = entity(view.entityKey);
            SkeletonFrameComponent previous = id != null
                    ? world.get(id, SkeletonFrameComponent.class) : null;
            if (previous != null && frameId < previous.frameId) continue;
            if (id == null) {
                id = world.createEntity();
                world.put(id, SkeletonIdentityComponent.class,
                        new SkeletonIdentityComponent(view.entityKey));
            }
            seen.add(id);
            world.put(id, SkeletonAssetComponent.class, view.asset);
            world.put(id, SkeletonPoseComponent.class, view.pose);
            world.put(id, SkeletonAnimationComponent.class, view.animation);
            world.put(id, SkeletonVisualComponent.class, view.visual);
            world.put(id, SkeletonFrameComponent.class, new SkeletonFrameComponent(frameId));
            SkeletonRuntimeBinding binding = bindings.get(id);
            String assetKey = view.asset.providerId + "|" + view.asset.atlasResource + "|" + view.asset.skeletonResource;
            if (binding == null || !assetKey.equals(binding.assetKey) || !binding.handle.isAlive()) {
                if (binding != null) release(binding);
                SkeletonProvider provider = providers.get(view.asset.providerId);
                if (provider == null) throw new IllegalStateException("skeleton provider not registered: " + view.asset.providerId);
                Map<String, Object> params = new LinkedHashMap<String, Object>();
                params.put("scale", Float.valueOf(view.asset.scale));
                SkeletonHandle handle = provider.load(new SkeletonSource(
                        view.entityKey, view.asset.atlasResource, view.asset.skeletonResource, params));
                binding = new SkeletonRuntimeBinding(id, handle, assetKey);
                bindings.put(id, binding);
                emit(SkeletonSignals.CREATED, view.entityKey, "created");
                emit(SkeletonSignals.LOADED, view.entityKey, "loaded");
            }
            apply(view, binding);
        }
        for (EntityId id : new ArrayList<EntityId>(bindings.keySet())) {
            if (!seen.contains(id)) {
                SkeletonRuntimeBinding binding = bindings.remove(id);
                unload(id, binding);
            }
        }
    }

    private void apply(SkeletonPresentationView view, SkeletonRuntimeBinding binding) {
        SkeletonCommandProvider provider = commandProvider(binding.handle);
        if (provider == null) return;
        provider.setPose(binding.handle, view.pose.x, view.pose.y, view.pose.rotation,
                view.pose.scaleX, view.pose.scaleY, view.pose.flipX, view.pose.flipY);
        provider.setVisual(binding.handle, view.visual.visible, view.visual.red,
                view.visual.green, view.visual.blue, view.visual.alpha);
        if (!view.animation.animation.isEmpty()) {
            String current = provider.currentAnimation(binding.handle, view.animation.track);
            if (!view.animation.animation.equals(current)) {
                provider.setAnimation(binding.handle, view.animation.track, view.animation.animation, view.animation.loop);
                emit(SkeletonSignals.ANIMATION_CHANGED, view.entityKey, view.animation.animation);
            }
            provider.setTimeScale(binding.handle, view.animation.track, view.animation.timeScale);
            provider.setTrackTime(binding.handle, view.animation.track, view.animation.trackTime);
        }
    }

    private SkeletonCommandProvider commandProvider(SkeletonHandle handle) {
        SkeletonProvider provider = providers.get(handle.providerId);
        return provider instanceof SkeletonCommandProvider ? (SkeletonCommandProvider) provider : null;
    }

    private void unload(EntityId id, SkeletonRuntimeBinding binding) {
        release(binding);
        SkeletonIdentityComponent identity = world.contains(id)
                ? world.get(id, SkeletonIdentityComponent.class) : null;
        if (world.contains(id)) world.destroyEntity(id);
        emit(SkeletonSignals.REMOVED, identity != null ? identity.entityKey : binding.handle.skeletonId, "removed");
    }

    private void release(SkeletonRuntimeBinding binding) {
        SkeletonProvider provider = providers.get(binding.handle.providerId);
        if (provider != null) provider.unload(binding.handle);
    }

    public SkeletonRuntimeBinding binding(String entityKey) {
        EntityId id = entity(entityKey);
        return id != null ? bindings.get(id) : null;
    }
    PresentationWorld world() { return world; }
    public int size() { return bindings.size(); }
    public long lastFrameId() {
        for (EntityId id : world.query(SkeletonSyncFrameComponent.class)) {
            return world.get(id, SkeletonSyncFrameComponent.class).frameId;
        }
        return -1L;
    }

    public void renderAll(Object batch) {
        renderAllExcept(batch, java.util.Collections.<String>emptySet());
    }

    /** Renders only bindings not owned by a host-native draw slot. */
    public void renderAllExcept(Object batch, Set<String> excludedEntityKeys) {
        for (SkeletonRuntimeBinding binding : new ArrayList<SkeletonRuntimeBinding>(bindings.values())) {
            if (excludedEntityKeys != null && excludedEntityKeys.contains(binding.handle.skeletonId)) continue;
            SkeletonProvider provider = providers.get(binding.handle.providerId);
            if (provider instanceof SkeletonCommandProvider) {
                ((SkeletonCommandProvider) provider).render(binding.handle, batch);
            }
        }
    }

    /** Advances every live animation on the host thread before the render pass. */
    public void tick(float deltaSeconds) {
        if (deltaSeconds < 0f) throw new IllegalArgumentException("deltaSeconds must not be negative");
        for (SkeletonRuntimeBinding binding : new ArrayList<SkeletonRuntimeBinding>(bindings.values())) {
            SkeletonProvider provider = providers.get(binding.handle.providerId);
            if (provider instanceof SkeletonCommandProvider) {
                SkeletonCommandProvider command = (SkeletonCommandProvider) provider;
                command.update(binding.handle, deltaSeconds);
                command.apply(binding.handle);
            }
        }
    }

    public void clear() {
        for (Map.Entry<EntityId, SkeletonRuntimeBinding> entry : new ArrayList<Map.Entry<EntityId, SkeletonRuntimeBinding>>(bindings.entrySet())) {
            unload(entry.getKey(), entry.getValue());
        }
        bindings.clear();
        for (EntityId id : new ArrayList<EntityId>(world.query(SkeletonSyncFrameComponent.class))) {
            world.destroyEntity(id);
        }
    }

    /** Release provider handles and rebuild them from the authoritative ECS snapshot. */
    public void recreateHostBindings() {
        List<SkeletonPresentationView> views = views();
        for (SkeletonRuntimeBinding binding
                : new ArrayList<SkeletonRuntimeBinding>(bindings.values())) {
            release(binding);
        }
        bindings.clear();
        if (!views.isEmpty()) sync(lastFrameId(), views);
    }

    public List<SkeletonPresentationView> views() {
        List<SkeletonPresentationView> result = new ArrayList<SkeletonPresentationView>();
        for (EntityId id : world.query(SkeletonIdentityComponent.class)) {
            SkeletonIdentityComponent identity = world.get(id, SkeletonIdentityComponent.class);
            SkeletonAssetComponent asset = world.get(id, SkeletonAssetComponent.class);
            SkeletonPoseComponent pose = world.get(id, SkeletonPoseComponent.class);
            SkeletonAnimationComponent animation = world.get(id, SkeletonAnimationComponent.class);
            SkeletonVisualComponent visual = world.get(id, SkeletonVisualComponent.class);
            if (identity != null && asset != null && pose != null && animation != null
                    && visual != null) {
                result.add(new SkeletonPresentationView(
                        identity.entityKey, asset, pose, animation, visual));
            }
        }
        return result;
    }

    private EntityId entity(String entityKey) {
        if (entityKey == null) return null;
        for (EntityId id : world.query(SkeletonIdentityComponent.class)) {
            SkeletonIdentityComponent identity = world.get(id, SkeletonIdentityComponent.class);
            if (entityKey.equals(identity.entityKey)) return id;
        }
        return null;
    }

    private EntityId syncEntity() {
        for (EntityId id : world.query(SkeletonSyncFrameComponent.class)) return id;
        return world.createEntity();
    }

    private static void emit(String name, String entityKey, String detail) {
        SignalBuses.get().emit(new UiSignal(name, "skeleton", new SkeletonSignals.Event(entityKey, detail)));
    }
}
