package artframework.vfx;

/** Immutable graph lifecycle clock. */
public final class VfxLifecycleComponent {
    public final float ageSeconds;
    public final float sceneDuration;

    public VfxLifecycleComponent(float ageSeconds, float sceneDuration) {
        this.ageSeconds = Math.max(0f, ageSeconds);
        this.sceneDuration = Math.max(0f, sceneDuration);
    }

    public VfxLifecycleComponent advance(float deltaSeconds) {
        return new VfxLifecycleComponent(ageSeconds + deltaSeconds, sceneDuration);
    }
}
