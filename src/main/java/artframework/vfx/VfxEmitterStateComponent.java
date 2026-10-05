package artframework.vfx;

/** Immutable one-shot emission progress plus explicit stop/restart control. */
public final class VfxEmitterStateComponent {
    public final int spawnedCount;
    public final boolean emissionComplete;
    /** True while explicitly stopped: no new particles spawn and existing ones keep ageing out. */
    public final boolean stopped;

    public VfxEmitterStateComponent(int spawnedCount, boolean emissionComplete) {
        this(spawnedCount, emissionComplete, false);
    }

    public VfxEmitterStateComponent(int spawnedCount, boolean emissionComplete, boolean stopped) {
        this.spawnedCount = spawnedCount;
        this.emissionComplete = emissionComplete;
        this.stopped = stopped;
    }

    /** Explicit stop: block further spawning without clearing already-emitted particles. */
    public VfxEmitterStateComponent requestStop() {
        if (stopped) return this;
        return new VfxEmitterStateComponent(spawnedCount, emissionComplete, true);
    }

    /** Deterministic restart: reset one-shot progress so the unchanged seed re-spawns identically. */
    public VfxEmitterStateComponent restart() {
        return new VfxEmitterStateComponent(0, false, false);
    }
}
