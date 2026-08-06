package artframework.skeleton;

import java.util.Random;

/**
 * Trigger-driven animation graph runner mirroring STS2's CreatureAnimator behavior.
 */
public final class SkeletonAnimator {

    public static final String TRIGGER_IDLE = "Idle";
    public static final String TRIGGER_ATTACK = "Attack";
    public static final String TRIGGER_POWER_UP = "PowerUp";
    public static final String TRIGGER_CAST = "Cast";
    public static final String TRIGGER_DEAD = "Dead";
    public static final String TRIGGER_HIT = "Hit";
    public static final String TRIGGER_REVIVE = "Revive";

    private final SkeletonCommandProvider provider;
    private final SkeletonHandle handle;
    private final AnimGraph graph;
    private final SkeletonMixTable mixTable;
    private final Random random;
    private AnimState currentState;
    private String lastWarning;

    public SkeletonAnimator(
            SkeletonCommandProvider provider,
            SkeletonHandle handle,
            AnimGraph graph,
            SkeletonMixTable mixTable,
            Random random) {
        if (provider == null) {
            throw new IllegalArgumentException("provider required");
        }
        if (handle == null) {
            throw new IllegalArgumentException("handle required");
        }
        if (graph == null) {
            throw new IllegalArgumentException("graph required");
        }
        this.provider = provider;
        this.handle = handle;
        this.graph = graph;
        this.mixTable = mixTable != null ? mixTable : new SkeletonMixTable(0f, null);
        this.random = random != null ? random : new Random();
    }

    public void start() {
        applyState(graph.initialState(), true);
    }

    public boolean trigger(String trigger) {
        AnimState next = graph.stateForTrigger(trigger);
        if (next == null) {
            return false;
        }
        applyState(next, false);
        return true;
    }

    public AnimState currentState() {
        return currentState;
    }

    public String lastWarning() {
        return lastWarning;
    }

    private void applyState(AnimState state, boolean initial) {
        if (!provider.hasAnimation(handle, state.id)) {
            lastWarning = "missing animation: " + state.id;
            return;
        }
        if (currentState != null) {
            provider.setMix(handle, currentState.id, state.id, mixTable.mix(currentState.id, state.id));
        }
        currentState = state;
        provider.setAnimation(handle, 0, state.id, state.looping);
        if (state.looping) {
            offsetLoopingAnimation();
        }
        if (state.nextStateId != null) {
            queueNextState(state.nextStateId);
        } else if (!state.looping && !initial) {
            AnimState idle = graph.state(graph.initialStateId);
            if (idle != null && idle != state && provider.hasAnimation(handle, idle.id)) {
                provider.addAnimation(handle, 0, idle.id, idle.looping, 0f);
            }
        }
    }

    private void queueNextState(String stateId) {
        AnimState next = graph.state(stateId);
        if (next == null) {
            lastWarning = "missing next state: " + stateId;
            return;
        }
        if (!provider.hasAnimation(handle, next.id)) {
            lastWarning = "missing animation: " + next.id;
            return;
        }
        provider.addAnimation(handle, 0, next.id, next.looping, 0f);
        if (next.nextStateId != null) {
            queueNextState(next.nextStateId);
        }
    }

    private void offsetLoopingAnimation() {
        float end = provider.animationEnd(handle, 0);
        provider.setTimeScale(handle, 0, 0.9f + random.nextFloat() * 0.2f);
        if (end > 0f) {
            provider.setTrackTime(handle, 0, random.nextFloat() * end);
            provider.update(handle, 0f);
            provider.apply(handle);
        }
    }
}
