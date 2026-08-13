package artframework.ecs;

/** Immutable data supplied to every system for one ECS tick. */
public final class EcsTick {
    public final float deltaSeconds;
    public final long sequence;

    public EcsTick(float deltaSeconds, long sequence) {
        if (deltaSeconds < 0f) throw new IllegalArgumentException("deltaSeconds must be non-negative");
        if (sequence < 0L) throw new IllegalArgumentException("sequence must be non-negative");
        this.deltaSeconds = deltaSeconds;
        this.sequence = sequence;
    }
}
