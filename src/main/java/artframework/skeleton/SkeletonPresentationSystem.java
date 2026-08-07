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
    private final Map<String, SkeletonRuntimeBinding> bindings =
            new LinkedHashMap<String, SkeletonRuntimeBinding>();
    private long lastFrameId = -1L;

    public SkeletonPresentationSystem(PresentationWorld world, SkeletonProviders providers) {
        if (world == null || providers == null) throw new IllegalArgumentException("world and providers required");
        this.world = world; this.providers = providers;
    }

    public void sync(long frameId, List<SkeletonPresentationView> views) {
        if (frameId < lastFrameId) return;
        List<SkeletonPresentationView> safe = views != null ? views : new ArrayList<SkeletonPresentationView>();
        Set<String> seen = new HashSet<String>();
        for (SkeletonPresentationView view : safe) {
            seen.add(view.entityKey);
            SkeletonRuntimeBinding binding = bindings.get(view.entityKey);
            String assetKey = view.asset.providerId + "|" + view.asset.atlasResource + "|" + view.asset.skeletonResource;
            if (binding == null || !assetKey.equals(binding.assetKey) || !binding.handle.isAlive()) {
                if (binding != null) unload(view.entityKey, binding);
                EntityId id = world.createEntity();
                SkeletonProvider provider = providers.get(view.asset.providerId);
                if (provider == null) throw new IllegalStateException("skeleton provider not registered: " + view.asset.providerId);
                Map<String, Object> params = new LinkedHashMap<String, Object>();
                params.put("scale", Float.valueOf(view.asset.scale));
                SkeletonHandle handle = provider.load(new SkeletonSource(
                        view.entityKey, view.asset.atlasResource, view.asset.skeletonResource, params));
                binding = new SkeletonRuntimeBinding(id, handle, assetKey);
                bindings.put(view.entityKey, binding);
                emit(SkeletonSignals.CREATED, view.entityKey, "created");
                emit(SkeletonSignals.LOADED, view.entityKey, "loaded");
            }
            apply(view, binding);
        }
        for (String key : new ArrayList<String>(bindings.keySet())) {
            if (!seen.contains(key)) {
                SkeletonRuntimeBinding binding = bindings.remove(key);
                unload(key, binding);
            }
        }
        lastFrameId = frameId;
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

    private void unload(String key, SkeletonRuntimeBinding binding) {
        SkeletonProvider provider = providers.get(binding.handle.providerId);
        if (provider != null) provider.unload(binding.handle);
        if (world.contains(binding.entityId)) world.destroyEntity(binding.entityId);
        emit(SkeletonSignals.REMOVED, key, "removed");
    }

    public SkeletonRuntimeBinding binding(String entityKey) { return bindings.get(entityKey); }
    public int size() { return bindings.size(); }
    public long lastFrameId() { return lastFrameId; }

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
        for (Map.Entry<String, SkeletonRuntimeBinding> entry : new ArrayList<Map.Entry<String, SkeletonRuntimeBinding>>(bindings.entrySet())) {
            unload(entry.getKey(), entry.getValue());
        }
        bindings.clear();
        lastFrameId = -1L;
    }

    private static void emit(String name, String entityKey, String detail) {
        SignalBuses.get().emit(new UiSignal(name, "skeleton", new SkeletonSignals.Event(entityKey, detail)));
    }
}
