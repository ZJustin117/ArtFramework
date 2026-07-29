package artframework.sts1;

/**
 * Resolves requested full-present policy into the effective host capability. A FULL request never
 * hides native UI until the surface is mounted, in its scene, and has a ready input executor.
 */
public final class FullPresentCapability {

    public enum State {
        OFF,
        OBSERVING,
        FULL_READY,
        FULL_FALLBACK_NATIVE
    }

    public final State state;
    public final String reason;

    private FullPresentCapability(State state, String reason) {
        this.state = state;
        this.reason = reason;
    }

    public boolean shouldDraw() {
        return state == State.FULL_READY;
    }

    public boolean shouldSuppressNative() {
        return state == State.FULL_READY;
    }

    public boolean ownsInput() {
        return state == State.FULL_READY;
    }

    public static FullPresentCapability resolve(
            PresentLevel level,
            boolean mounted,
            boolean sceneReady,
            boolean executorReady,
            boolean overlayObserve,
            boolean panic) {
        if (panic) {
            return new FullPresentCapability(State.OFF, "panic");
        }
        if (level == null || level == PresentLevel.OFF) {
            return new FullPresentCapability(State.OFF, "off");
        }
        if (level == PresentLevel.OBSERVE) {
            return new FullPresentCapability(State.OBSERVING, "observe");
        }
        if (!mounted) {
            return new FullPresentCapability(State.FULL_FALLBACK_NATIVE, "unmounted");
        }
        if (!sceneReady) {
            return new FullPresentCapability(State.FULL_FALLBACK_NATIVE, "scene_unavailable");
        }
        if (overlayObserve) {
            return new FullPresentCapability(State.OBSERVING, "overlay_observe");
        }
        if (!executorReady) {
            return new FullPresentCapability(State.FULL_FALLBACK_NATIVE, "executor_unavailable");
        }
        return new FullPresentCapability(State.FULL_READY, "ready");
    }
}
