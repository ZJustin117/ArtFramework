package artframework.skeleton;

/** Current animation command/state for one presentation skeleton. */
public final class SkeletonAnimationComponent {
    public final int track;
    public final String animation;
    public final boolean loop;
    public final float timeScale;
    public final float trackTime;
    public final String mixFrom;
    public final String mixTo;
    public final float mixDuration;

    public SkeletonAnimationComponent(int track, String animation, boolean loop,
            float timeScale, float trackTime, String mixFrom, String mixTo, float mixDuration) {
        if (track < 0) throw new IllegalArgumentException("track must not be negative");
        if (!(timeScale >= 0f) || !(trackTime >= 0f) || !(mixDuration >= 0f)) {
            throw new IllegalArgumentException("animation values must not be negative");
        }
        this.track = track; this.animation = animation != null ? animation : "";
        this.loop = loop; this.timeScale = timeScale; this.trackTime = trackTime;
        this.mixFrom = mixFrom != null ? mixFrom : "";
        this.mixTo = mixTo != null ? mixTo : "";
        this.mixDuration = mixDuration;
    }

    public SkeletonAnimationComponent(int track, String animation, boolean loop) {
        this(track, animation, loop, 1f, 0f, "", "", 0f);
    }
}
