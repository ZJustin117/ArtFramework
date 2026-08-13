package artframework.ecs;

/**
 * ART-owned entity identity.
 *
 * <p>An entity deliberately has no type, source, scope, or behavior. Its components are the
 * complete description of its presentation state.</p>
 */
public final class EntityId {
    private final long value;

    EntityId(long value) {
        this.value = value;
    }

    public long value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EntityId)) return false;
        EntityId that = (EntityId) other;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return (int) (value ^ (value >>> 32));
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
