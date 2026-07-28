package artframework.context;

/** Primary Backend intent mode. */
public enum BackendMode {
    /** Snapshots + execute intents. */
    FULL_CONTROL,
    /** Snapshots only; intents rejected. */
    READ_ONLY
}
