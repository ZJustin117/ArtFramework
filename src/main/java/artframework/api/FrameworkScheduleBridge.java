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

    void executeSurfaceIntents() {
        schedule.executeSurfaceIntents();
    }

    void executeSurfaceLifecycle() {
        schedule.executeSurfaceLifecycle();
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
