package artframework.context;

/** Optional lifecycle hook for a backend that subscribes to ordinary ART signals. */
public interface SignalBackend {
    void installSignals();
    void uninstallSignals();
}
