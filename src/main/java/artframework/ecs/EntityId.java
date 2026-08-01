package artframework.ecs;

/** Stable identity for an entity inside one presentation world. */
public final class EntityId {
    private final String scope;
    private final long value;

    EntityId(String scope, long value) {
        this.scope = scope;
        this.value = value;
    }

    public String scope() {
        return scope;
    }

    public long value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EntityId)) return false;
        EntityId that = (EntityId) other;
        return value == that.value && scope.equals(that.scope);
    }

    @Override
    public int hashCode() {
        return 31 * scope.hashCode() + (int) (value ^ (value >>> 32));
    }

    @Override
    public String toString() {
        return scope + ":" + value;
    }
}
