package artframework.api;

/** Host-specific presentation advancement invoked at one declared production schedule phase. */
public interface HostPresentationSystem {
    void tick(float deltaSeconds);
}
