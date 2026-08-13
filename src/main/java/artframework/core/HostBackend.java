package artframework.core;

import artframework.presentation.PresentationMount;

/**
 * Host SPI for inflate / input / draw (STS1 Stage, future STS2).
 * Pure tests use a no-op or fake; production installs Stage-backed impl.
 */
public interface HostBackend {

    /** Whether the host can attach ECS presentation mounts (e.g. Stage ready). */
    boolean isReady();

    default void attach(PresentationMount mount) {}

    default void detach(PresentationMount mount) {}

    /**
     * Optional: push pure layout rects into host actors. Default no-op.
     */
    default void applyLayout(PresentationMount mount) {}

    /** Capabilities available from this host. */
    default HostCapabilities capabilities() {
        return HostCapabilities.none();
    }

    /** Host-side frame tick. Default is a no-op for headless implementations. */
    default void tick(float deltaSeconds) {}
}
