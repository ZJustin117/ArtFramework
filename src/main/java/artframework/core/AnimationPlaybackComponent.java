package artframework.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Data-only playback state for an animation-player entity. */
public final class AnimationPlaybackComponent {
    public final String playing;
    public final float elapsed;
    public final boolean active;
    public final boolean paused;
    public final String playMode;
    public final int loopsDone;
    public final Map<String, Float> fromSnapshot;

    public AnimationPlaybackComponent(
            String playing,
            float elapsed,
            boolean active,
            boolean paused,
            String playMode,
            int loopsDone,
            Map<String, Float> fromSnapshot) {
        this.playing = playing;
        this.elapsed = elapsed < 0f ? 0f : elapsed;
        this.active = active;
        this.paused = paused;
        this.playMode = AnimationPlayer.MODE_LOOP.equals(playMode)
                ? AnimationPlayer.MODE_LOOP : AnimationPlayer.MODE_ONCE;
        this.loopsDone = loopsDone < 0 ? 0 : loopsDone;
        this.fromSnapshot = fromSnapshot == null || fromSnapshot.isEmpty()
                ? Collections.<String, Float>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Float>(fromSnapshot));
    }

    public static AnimationPlaybackComponent idle() {
        return new AnimationPlaybackComponent(null, 0f, false, false,
                AnimationPlayer.MODE_ONCE, 0, null);
    }
}
