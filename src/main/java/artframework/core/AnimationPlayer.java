package artframework.core;

import artframework.presentation.NodeStateComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRuntime;
import artframework.presentation.NodeHierarchyComponent;
import artframework.presentation.NodeIdentityComponent;
import artframework.ecs.EntityId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure timeline player for a behavior entity. Drives target presentation properties.
 * States: idle / playing / paused via thin {@link NodeStateMachine} fields.
 */
public final class AnimationPlayer {

    public static final String SIGNAL_STARTED = SignalNames.STARTED;
    public static final String SIGNAL_FINISHED = SignalNames.FINISHED;
    public static final String SIGNAL_CANCELLED = SignalNames.CANCELLED;
    public static final String SIGNAL_PAUSED = SignalNames.PAUSED;
    public static final String SIGNAL_RESUMED = SignalNames.RESUMED;
    public static final String SIGNAL_LOOPED = SignalNames.LOOPED;

    public static final String MODE_ONCE = "once";
    public static final String MODE_LOOP = "loop";

    public static final class Track {
        public final String property;
        public final float from;
        public final float to;
        /** When true, {@link #from} is ignored and the target prop at play start is used. */
        public final boolean fromCurrent;

        public Track(String property, float from, float to) {
            this(property, from, to, false);
        }

        public Track(String property, float from, float to, boolean fromCurrent) {
            if (property == null || property.isEmpty()) {
                throw new IllegalArgumentException("property required");
            }
            this.property = property;
            this.from = from;
            this.to = to;
            this.fromCurrent = fromCurrent;
        }
    }

    public static final class Animation {
        public final String name;
        public final String targetId;
        public final float duration;
        public final List<Track> tracks;
        /** once (default) or loop */
        public final String mode;
        /** 0 = infinite when mode=loop */
        public final int loopCount;
        /** Optional animation started directly when this animation completes. */
        public final String next;
        public final String nextMode;

        public Animation(String name, String targetId, float duration, List<Track> tracks) {
            this(name, targetId, duration, tracks, MODE_ONCE, 0, "", "");
        }

        public Animation(
                String name,
                String targetId,
                float duration,
                List<Track> tracks,
                String mode,
                int loopCount) {
            this(name, targetId, duration, tracks, mode, loopCount, "", "");
        }

        public Animation(
                String name,
                String targetId,
                float duration,
                List<Track> tracks,
                String mode,
                int loopCount,
                String next,
                String nextMode) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("animation name required");
            }
            this.name = name;
            this.targetId = targetId != null ? targetId : "";
            this.duration = duration > 0f ? duration : 0.001f;
            if (tracks == null || tracks.isEmpty()) {
                this.tracks = Collections.emptyList();
            } else {
                this.tracks = Collections.unmodifiableList(new ArrayList<Track>(tracks));
            }
            String m = mode != null ? mode.trim().toLowerCase() : MODE_ONCE;
            this.mode = MODE_LOOP.equals(m) ? MODE_LOOP : MODE_ONCE;
            this.loopCount = loopCount > 0 ? loopCount : 0;
            this.next = next != null ? next.trim() : "";
            this.nextMode = nextMode != null ? nextMode.trim() : "";
        }
    }

    private final PresentationContext context;
    private final EntityId owner;
    private final Map<String, Animation> animations = new LinkedHashMap<String, Animation>();
    private final NodeStateMachine fsm;

    public AnimationPlayer(PresentationContext context, EntityId owner) {
        if (context == null || owner == null) throw new IllegalArgumentException("context and owner required");
        this.context = context;
        this.owner = owner;
        this.fsm = new NodeStateMachine(context, owner, NodeStateMachine.STATE_IDLE);
        context.world().put(owner, AnimationPlaybackComponent.class,
                AnimationPlaybackComponent.idle());
    }

    public NodeStateMachine stateMachine() {
        return fsm;
    }

    public String state() {
        return fsm.state();
    }

    public void register(Animation animation) {
        if (animation == null) {
            throw new IllegalArgumentException("animation required");
        }
        animations.put(animation.name, animation);
    }

    public boolean has(String name) {
        return name != null && animations.containsKey(name);
    }

    public String playing() {
        return playback().playing;
    }

    public boolean isPlaying() {
        AnimationPlaybackComponent state = playback();
        return state.active && !state.paused;
    }

    public boolean isPaused() {
        return playback().paused;
    }

    /** Current completed loop count from the ECS-backed playback state. */
    public int loopsDone() {
        return playback().loopsDone;
    }

    public void play(String name) {
        play(name, null);
    }

    public void play(String name, String modeOverride) {
        Animation anim = animations.get(name);
        if (anim == null) {
            throw new IllegalArgumentException("unknown animation: " + name);
        }
        AnimationPlaybackComponent state = playback();
        if (state.active && state.playing != null) {
        }
        String playMode;
        if (modeOverride != null && !modeOverride.isEmpty()) {
            playMode =
                    MODE_LOOP.equals(modeOverride.trim().toLowerCase()) ? MODE_LOOP : MODE_ONCE;
        } else {
            playMode = anim.mode;
        }
        Map<String, Float> fromSnapshot = captureFromSnapshot(anim);
        putPlayback(new AnimationPlaybackComponent(
                name, 0f, true, false, playMode, 0, fromSnapshot));
        fsm.setState(NodeStateMachine.STATE_PLAYING, false);
        apply(anim, 0f);
        if (anim.duration <= 0.001f && anim.tracks.isEmpty() && !MODE_LOOP.equals(playMode)) {
            finish(anim);
        }
    }

    public void pause() {
        AnimationPlaybackComponent state = playback();
        if (!state.active || state.paused || state.playing == null) {
            return;
        }
        putPlayback(new AnimationPlaybackComponent(state.playing, state.elapsed, true, true,
                state.playMode, state.loopsDone, state.fromSnapshot));
        fsm.setState(NodeStateMachine.STATE_PAUSED, false);
    }

    public void resume() {
        AnimationPlaybackComponent state = playback();
        if (!state.active || !state.paused || state.playing == null) {
            return;
        }
        putPlayback(new AnimationPlaybackComponent(state.playing, state.elapsed, true, false,
                state.playMode, state.loopsDone, state.fromSnapshot));
        fsm.setState(NodeStateMachine.STATE_PLAYING, false);
    }

    public void stop() {
        AnimationPlaybackComponent state = playback();
        if (!state.active && !state.paused) {
            return;
        }
        String name = state.playing;
        putPlayback(AnimationPlaybackComponent.idle());
        fsm.setState(NodeStateMachine.STATE_IDLE, false);
        if (name != null) {
        }
    }

    public void tick(float deltaSeconds) {
        AnimationPlaybackComponent state = playback();
        if (!state.active || state.paused || state.playing == null) {
            return;
        }
        Animation anim = animations.get(state.playing);
        if (anim == null) {
            putPlayback(AnimationPlaybackComponent.idle());
            fsm.setState(NodeStateMachine.STATE_IDLE, false);
            return;
        }
        if (deltaSeconds < 0f) {
            deltaSeconds = 0f;
        }
        float elapsed = state.elapsed + deltaSeconds;
        float t = elapsed / anim.duration;
        if (t >= 1f) {
            apply(anim, 1f);
            if (MODE_LOOP.equals(state.playMode)) {
                int loopsDone = state.loopsDone + 1;
                if (anim.loopCount > 0 && loopsDone >= anim.loopCount) {
                    finish(anim);
                } else {
                    putPlayback(new AnimationPlaybackComponent(state.playing, 0f, true, false,
                            state.playMode, loopsDone, state.fromSnapshot));
                    apply(anim, 0f);
                }
            } else {
                finish(anim);
            }
        } else {
            putPlayback(new AnimationPlaybackComponent(state.playing, elapsed, true, false,
                    state.playMode, state.loopsDone, state.fromSnapshot));
            apply(anim, t);
        }
    }

    private void finish(Animation anim) {
        putPlayback(AnimationPlaybackComponent.idle());
        fsm.setState(NodeStateMachine.STATE_IDLE, false);
        if (!anim.next.isEmpty()) {
            play(anim.next, anim.nextMode);
        }
    }

    private Map<String, Float> captureFromSnapshot(Animation anim) {
        Map<String, Float> snapshot = new LinkedHashMap<String, Float>();
        EntityId target = resolveTarget(anim.targetId);
        if (target == null) {
            return snapshot;
        }
        for (Track track : anim.tracks) {
            if (!track.fromCurrent) {
                continue;
            }
            Object cur = PresentationRuntime.property(context, target, track.property);
            float f = track.from;
            if (cur instanceof Number) {
                f = ((Number) cur).floatValue();
            }
            snapshot.put(track.property, Float.valueOf(f));
        }
        return snapshot;
    }

    private float trackFrom(Track track) {
        Map<String, Float> snapshot = playback().fromSnapshot;
        if (track.fromCurrent && snapshot.containsKey(track.property)) {
            return snapshot.get(track.property).floatValue();
        }
        return track.from;
    }

    private void apply(Animation anim, float t) {
        if (t < 0f) {
            t = 0f;
        }
        if (t > 1f) {
            t = 1f;
        }
        EntityId target = resolveTarget(anim.targetId);
        if (target == null) {
            return;
        }
        for (Track track : anim.tracks) {
            float from = trackFrom(track);
            float v = from + (track.to - from) * t;
            PropEffectBridge.applyProp(context, target, track.property, Float.valueOf(v));
        }
    }

    private EntityId resolveTarget(String targetId) {
        if (targetId == null || targetId.isEmpty()) {
            NodeHierarchyComponent hierarchy = PresentationRuntime.hierarchy(context, owner);
            return hierarchy != null ? hierarchy.parent : null;
        }
        return PresentationRuntime.find(context, targetId);
    }

    private AnimationPlaybackComponent playback() {
        AnimationPlaybackComponent state = context.world().get(owner, AnimationPlaybackComponent.class);
        return state != null ? state : AnimationPlaybackComponent.idle();
    }

    private void putPlayback(AnimationPlaybackComponent state) {
        context.world().put(owner, AnimationPlaybackComponent.class, state);
    }
}
