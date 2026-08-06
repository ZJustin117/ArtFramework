package artframework.skeleton;

/**
 * Spine animation state descriptor inspired by STS2's AnimState graph.
 */
public final class AnimState {

    public final String id;
    public final boolean looping;
    public final String nextStateId;

    public AnimState(String id, boolean looping) {
        this(id, looping, null);
    }

    public AnimState(String id, boolean looping, String nextStateId) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("state id required");
        }
        this.id = id;
        this.looping = looping;
        this.nextStateId = nextStateId != null && !nextStateId.isEmpty() ? nextStateId : null;
    }
}
