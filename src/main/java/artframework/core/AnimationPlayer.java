package artframework.core;

import artframework.presentation.Node;
import artframework.presentation.NodeTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure timeline player for a behavior node. Drives target presentation Node props.
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

        public Animation(String name, String targetId, float duration, List<Track> tracks) {
            this(name, targetId, duration, tracks, MODE_ONCE, 0);
        }

        public Animation(
                String name,
                String targetId,
                float duration,
                List<Track> tracks,
                String mode,
                int loopCount) {
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
        }
    }

    private final Node owner;
    private final Map<String, Animation> animations = new LinkedHashMap<String, Animation>();
    private final NodeStateMachine fsm;
    private String playing;
    private float elapsed;
    private boolean active;
    private boolean paused;
    private String playMode = MODE_ONCE;
    private int loopsDone;
    private final Map<String, Float> fromSnapshot = new LinkedHashMap<String, Float>();

    public AnimationPlayer(Node owner) {
        if (owner == null) {
            throw new IllegalArgumentException("owner required");
        }
        this.owner = owner;
        this.fsm = new NodeStateMachine(owner, NodeStateMachine.STATE_IDLE);
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
        return playing;
    }

    public boolean isPlaying() {
        return active && !paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public void play(String name) {
        play(name, null);
    }

    public void play(String name, String modeOverride) {
        Animation anim = animations.get(name);
        if (anim == null) {
            throw new IllegalArgumentException("unknown animation: " + name);
        }
        if (active && playing != null) {
            emitOwn(SIGNAL_CANCELLED, playing);
        }
        playing = name;
        elapsed = 0f;
        active = true;
        paused = false;
        loopsDone = 0;
        if (modeOverride != null && !modeOverride.isEmpty()) {
            playMode =
                    MODE_LOOP.equals(modeOverride.trim().toLowerCase()) ? MODE_LOOP : MODE_ONCE;
        } else {
            playMode = anim.mode;
        }
        captureFromSnapshot(anim);
        fsm.setState(NodeStateMachine.STATE_PLAYING, false);
        apply(anim, 0f);
        emitOwn(SIGNAL_STARTED, name);
        if (anim.duration <= 0.001f && anim.tracks.isEmpty() && !MODE_LOOP.equals(playMode)) {
            finish(anim);
        }
    }

    public void pause() {
        if (!active || paused || playing == null) {
            return;
        }
        paused = true;
        fsm.setState(NodeStateMachine.STATE_PAUSED, false);
        emitOwn(SIGNAL_PAUSED, playing);
    }

    public void resume() {
        if (!active || !paused || playing == null) {
            return;
        }
        paused = false;
        fsm.setState(NodeStateMachine.STATE_PLAYING, false);
        emitOwn(SIGNAL_RESUMED, playing);
    }

    public void stop() {
        if (!active && !paused) {
            return;
        }
        String name = playing;
        active = false;
        paused = false;
        playing = null;
        elapsed = 0f;
        loopsDone = 0;
        fsm.setState(NodeStateMachine.STATE_IDLE, false);
        if (name != null) {
            emitOwn(SIGNAL_CANCELLED, name);
        }
    }

    public void tick(float deltaSeconds) {
        if (!active || paused || playing == null) {
            return;
        }
        Animation anim = animations.get(playing);
        if (anim == null) {
            active = false;
            fsm.setState(NodeStateMachine.STATE_IDLE, false);
            return;
        }
        if (deltaSeconds < 0f) {
            deltaSeconds = 0f;
        }
        elapsed += deltaSeconds;
        float t = elapsed / anim.duration;
        if (t >= 1f) {
            apply(anim, 1f);
            if (MODE_LOOP.equals(playMode)) {
                loopsDone++;
                emitOwn(SIGNAL_LOOPED, playing, Integer.valueOf(loopsDone));
                if (anim.loopCount > 0 && loopsDone >= anim.loopCount) {
                    finish(anim);
                } else {
                    elapsed = 0f;
                    apply(anim, 0f);
                }
            } else {
                finish(anim);
            }
        } else {
            apply(anim, t);
        }
    }

    private void finish(Animation anim) {
        active = false;
        paused = false;
        String name = anim.name;
        playing = null;
        elapsed = 0f;
        loopsDone = 0;
        fsm.setState(NodeStateMachine.STATE_IDLE, false);
        emitOwn(SIGNAL_FINISHED, name);
    }

    /** Behavior signals: hub emit so undeclared optional names (paused/looped) still propagate. */
    private void emitOwn(String signal, Object... args) {
        if (signal == null || owner.name().isEmpty()) {
            return;
        }
        try {
            if (owner.declaresSignal(signal)) {
                owner.emitSignal(signal, args);
            } else {
                artframework.core.SignalBuses.get().emit(
                        new UiSignal(SignalHub.name(owner.name(), signal), owner.name(), args));
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void captureFromSnapshot(Animation anim) {
        fromSnapshot.clear();
        Node target = resolveTarget(anim.targetId);
        if (target == null) {
            return;
        }
        for (Track track : anim.tracks) {
            if (!track.fromCurrent) {
                continue;
            }
            Object cur = target.get(track.property);
            float f = track.from;
            if (cur instanceof Number) {
                f = ((Number) cur).floatValue();
            }
            fromSnapshot.put(track.property, Float.valueOf(f));
        }
    }

    private float trackFrom(Track track) {
        if (track.fromCurrent && fromSnapshot.containsKey(track.property)) {
            return fromSnapshot.get(track.property).floatValue();
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
        Node target = resolveTarget(anim.targetId);
        if (target == null) {
            return;
        }
        for (Track track : anim.tracks) {
            float from = trackFrom(track);
            float v = from + (track.to - from) * t;
            PropEffectBridge.applyProp(owner.tree(), target, track.property, Float.valueOf(v));
        }
    }

    private Node resolveTarget(String targetId) {
        if (targetId == null || targetId.isEmpty()) {
            return owner.parent();
        }
        Node found = owner.tree().get(targetId);
        if (found != null) {
            return found;
        }
        return owner.tree().find(targetId);
    }
}
