package artframework.ecs;

/**
 * Stateless processor for ART component data.
 *
 * <p>Implementations must retain no cross-tick mutable state. Persistent facts belong to
 * components; host input and host capabilities are supplied through {@link EcsTick}.</p>
 */
public interface EcsSystem {
    void run(PresentationWorld world, EcsTick tick);
}
