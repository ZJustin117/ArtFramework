package artframework.vfx;

/** Immutable identity and invalidation state for one instantiated VFX graph root. */
public final class VfxSceneRuntimeComponent {
    public final String sceneId;
    public final long instantiatedEpoch;
    public final long currentEpoch;
    public final boolean cleanupRequested;

    public VfxSceneRuntimeComponent(String sceneId, long instantiatedEpoch,
            long currentEpoch, boolean cleanupRequested) {
        if (sceneId == null || sceneId.isEmpty()) throw new IllegalArgumentException("sceneId required");
        if (instantiatedEpoch < 0L || currentEpoch < 0L) throw new IllegalArgumentException("epoch must be non-negative");
        this.sceneId = sceneId;
        this.instantiatedEpoch = instantiatedEpoch;
        this.currentEpoch = currentEpoch;
        this.cleanupRequested = cleanupRequested;
    }

    public VfxSceneRuntimeComponent withCurrentEpoch(long epoch) {
        return new VfxSceneRuntimeComponent(sceneId, instantiatedEpoch, epoch, cleanupRequested);
    }

    public VfxSceneRuntimeComponent requestingCleanup() {
        return new VfxSceneRuntimeComponent(sceneId, instantiatedEpoch, currentEpoch, true);
    }
}
