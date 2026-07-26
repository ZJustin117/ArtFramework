package spireui.api;

/**
 * Opaque handle for an opened or bound window (C1 or C2).
 */
public interface WindowHandle {

    String id();

    WindowClass windowClass();

    boolean isOpen();

    void close();
}
