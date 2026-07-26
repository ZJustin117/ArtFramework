package artframework.api;

/**
 * Dual-track window kinds.
 * <ul>
 *   <li>{@link #SYNTHETIC} — scene2d.ui built windows (lobby, settings, …)</li>
 *   <li>{@link #NATIVE_TEMPLATE} — wrapped/intercepted STS native UI or entity presenters</li>
 * </ul>
 */
public enum WindowClass {
    SYNTHETIC,
    NATIVE_TEMPLATE
}
