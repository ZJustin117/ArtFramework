package artframework.core;

/**
 * Registered UI action invoked from declarative {@code connections} or Java.
 * No scripts in LML — only registered ids.
 */
public interface UiAction {

    /**
     * @param ctx owner tree / node / signal payload context
     * @return true if the action handled the event
     */
    boolean run(UiActionContext ctx);
}
