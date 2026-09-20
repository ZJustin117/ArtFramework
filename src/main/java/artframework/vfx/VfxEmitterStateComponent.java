package artframework.vfx;

/** Immutable one-shot emission progress. */
public final class VfxEmitterStateComponent {
    public final int spawnedCount;
    public final boolean emissionComplete;

    public VfxEmitterStateComponent(int spawnedCount, boolean emissionComplete) {
        this.spawnedCount = spawnedCount;
        this.emissionComplete = emissionComplete;
    }
}
