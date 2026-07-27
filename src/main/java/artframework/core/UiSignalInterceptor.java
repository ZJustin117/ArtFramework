package artframework.core;

/** Intercepts a C1 signal before its state change and emission. */
public interface UiSignalInterceptor {

    enum Result {
        ALLOW,
        BLOCK
    }

    Result intercept(String windowId, String controlId, String signal, Object... args);
}
