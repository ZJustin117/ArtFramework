package artframework.context;

/**
 * Pluggable Primary Backend: authority frames down, intents up.
 * Distinct from {@link artframework.core.HostBackend} (Stage inflate SPI).
 */
public interface PresentationBackend {

    String id();

    BackendMode mode();

    /**
     * Current authority snapshot, or unavailable frame when not ready.
     */
    ContextFrame snapshot();

    /**
     * Execute a UI intent. Read-only backends must reject.
     */
    IntentResult submitIntent(UiIntent intent);
}
