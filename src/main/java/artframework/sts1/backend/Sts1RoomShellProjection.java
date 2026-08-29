package artframework.sts1.backend;

import artframework.context.RoomShellView;

/** Non-authoritative host-neutral room-shell observation cache. */
public final class Sts1RoomShellProjection {
    private static volatile RoomShellView current = RoomShellView.empty();
    private Sts1RoomShellProjection() {}
    public static RoomShellView current() { return current; }
    public static void publish(RoomShellView view) { current = view != null ? view : RoomShellView.empty(); }
    public static void clear() { current = RoomShellView.empty(); }
    public static void resetForTests() { clear(); }
}
