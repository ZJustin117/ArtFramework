package artframework.api;

import artframework.context.ContextFrame;
import artframework.context.FrameDiff;

/** Package-private compatibility bridge from the public facade to the production schedule. */
final class FrameworkScheduleBridge {
    private final PresentationSchedule schedule;

    FrameworkScheduleBridge(PresentationSchedule schedule) {
        this.schedule = schedule;
    }

    void advance(float deltaSeconds, ContextFrame authorityFrame) {
        schedule.advance(deltaSeconds, authorityFrame);
    }

    artframework.core.SignalDispatchResult dispatchSurfaceIntent(
            String name, String surfaceId, Object... args) {
        return schedule.dispatchSurfaceIntent(name, surfaceId, args);
    }

    void dispatchSurfaceLifecycle(String surfaceId, boolean mounted) {
        schedule.dispatchSurfaceLifecycle(surfaceId, mounted);
    }

    artframework.core.SignalDispatchResult dispatchNativeIntentLifecycle(String surfaceId,
            String name, artframework.context.NativeIntentLifecycleComponent.State state,
            String message) {
        return schedule.dispatchNativeIntentLifecycle(surfaceId, name, state, message);
    }

    void processNativeIntentLifecycle() {
        schedule.processNativeIntentLifecycle();
    }

    void executeTransientEffectProjections() {
        schedule.executeTransientEffectProjections();
    }

    void executeEffectPulses(float deltaSeconds) {
        schedule.executeEffectPulses(deltaSeconds);
    }

    void setHostPresentationSystem(HostPresentationSystem system) {
        schedule.setHostPresentationSystem(system);
    }

    FrameDiff publishFrame(ContextFrame frame) {
        return schedule.publishFrame(frame);
    }

    void resetForTests() {
        schedule.resetForTests();
    }
}
