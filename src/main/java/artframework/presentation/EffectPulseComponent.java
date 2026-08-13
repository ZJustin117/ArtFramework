package artframework.presentation;

/** Data-only one-shot effect envelope attached to its target presentation entity. */
public final class EffectPulseComponent {
    public final String windowId;
    public final String targetId;
    public final String effectId;
    public final float baseline;
    public final float duration;
    public final float elapsed;
    public final boolean closeAfter;

    public EffectPulseComponent(
            String windowId,
            String targetId,
            String effectId,
            float baseline,
            float duration,
            float elapsed,
            boolean closeAfter) {
        this.windowId = windowId != null ? windowId : "";
        this.targetId = targetId != null ? targetId : "";
        this.effectId = effectId != null ? effectId : "";
        this.baseline = baseline;
        this.duration = duration > 0.05f ? duration : 0.4f;
        this.elapsed = elapsed < 0f ? 0f : elapsed;
        this.closeAfter = closeAfter;
    }
}
