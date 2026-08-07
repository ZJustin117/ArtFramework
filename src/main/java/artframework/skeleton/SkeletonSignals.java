package artframework.skeleton;

/** Signal names and payloads for skeleton presentation lifecycle. */
public final class SkeletonSignals {
    public static final String CREATED = "skeleton/created";
    public static final String LOADED = "skeleton/loaded";
    public static final String ANIMATION_CHANGED = "skeleton/animation_changed";
    public static final String RENDER_FAILED = "skeleton/render_failed";
    public static final String REMOVED = "skeleton/removed";

    private SkeletonSignals() {}

    public static final class Event {
        public final String entityKey;
        public final String detail;
        public Event(String entityKey, String detail) {
            this.entityKey = entityKey != null ? entityKey : "";
            this.detail = detail != null ? detail : "";
        }
    }
}
