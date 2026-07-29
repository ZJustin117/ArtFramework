package artframework.core;

/** Handle for a single bus subscription. */
public interface SignalSubscription {
    void disconnect();
    boolean isConnected();
}
