package artframework.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure timeline player for a behavior node. Drives target {@link UiInstance} props.
 */
public final class AnimationPlayer {

    public static final String SIGNAL_STARTED = "started";
    public static final String SIGNAL_FINISHED = "finished";
    public static final String SIGNAL_CANCELLED = "cancelled";

    public static final class Track {
        public final String property;
        public final float from;
        public final float to;

        public Track(String property, float from, float to) {
            if (property == null || property.isEmpty()) {
                throw new IllegalArgumentException("property required");
            }
            this.property = property;
            this.from = from;
            this.to = to;
        }
    }

    public static final class Animation {
        public final String name;
        public final String targetId;
        public final float duration;
        public final List<Track> tracks;

        public Animation(String name, String targetId, float duration, List<Track> tracks) {
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
        }
    }

    private final UiInstance owner;
    private final Map<String, Animation> animations = new LinkedHashMap<String, Animation>();
    private String playing;
    private float elapsed;
    private boolean active;

    public AnimationPlayer(UiInstance owner) {
        if (owner == null) {
            throw new IllegalArgumentException("owner required");
        }
        this.owner = owner;
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
        return active;
    }

    public void play(String name) {
        Animation anim = animations.get(name);
        if (anim == null) {
            throw new IllegalArgumentException("unknown animation: " + name);
        }
        if (active && playing != null) {
            owner.emit(SIGNAL_CANCELLED, playing);
        }
        playing = name;
        elapsed = 0f;
        active = true;
        apply(anim, 0f);
        owner.emit(SIGNAL_STARTED, name);
        if (anim.duration <= 0.001f && anim.tracks.isEmpty()) {
            finish(anim);
        }
    }

    public void stop() {
        if (!active) {
            return;
        }
        String name = playing;
        active = false;
        playing = null;
        elapsed = 0f;
        if (name != null) {
            owner.emit(SIGNAL_CANCELLED, name);
        }
    }

    public void tick(float deltaSeconds) {
        if (!active || playing == null) {
            return;
        }
        Animation anim = animations.get(playing);
        if (anim == null) {
            active = false;
            return;
        }
        if (deltaSeconds < 0f) {
            deltaSeconds = 0f;
        }
        elapsed += deltaSeconds;
        float t = elapsed / anim.duration;
        if (t >= 1f) {
            apply(anim, 1f);
            finish(anim);
        } else {
            apply(anim, t);
        }
    }

    private void finish(Animation anim) {
        active = false;
        String name = anim.name;
        playing = null;
        elapsed = 0f;
        owner.emit(SIGNAL_FINISHED, name);
    }

    private void apply(Animation anim, float t) {
        if (t < 0f) {
            t = 0f;
        }
        if (t > 1f) {
            t = 1f;
        }
        UiInstance target = resolveTarget(anim.targetId);
        if (target == null) {
            return;
        }
        for (Track track : anim.tracks) {
            float v = track.from + (track.to - track.from) * t;
            target.setProp(track.property, Float.valueOf(v));
        }
    }

    private UiInstance resolveTarget(String targetId) {
        if (targetId == null || targetId.isEmpty()) {
            return owner.parent();
        }
        UiInstance found = owner.tree().get(targetId);
        if (found != null) {
            return found;
        }
        return owner.tree().find(targetId);
    }
}
