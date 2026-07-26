package spireui.core;

/**
 * Handler for {@link SignalHub} emissions. Payload is signal-specific (may be empty).
 */
public interface SignalHandler {
    void handle(Object... args);
}
