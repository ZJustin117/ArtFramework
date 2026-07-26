package artframework.api;

/**
 * Outcome of a {@link UiOps} command. No multiplayer protocol semantics.
 */
public final class UiOpResult {

    public enum Status {
        OK,
        BLOCKED,
        NOT_BOUND,
        UNAVAILABLE
    }

    public final Status status;
    public final String message;

    private UiOpResult(Status status, String message) {
        this.status = status;
        this.message = message != null ? message : "";
    }

    public static UiOpResult ok() {
        return new UiOpResult(Status.OK, "");
    }

    public static UiOpResult ok(String message) {
        return new UiOpResult(Status.OK, message);
    }

    public static UiOpResult blocked(String message) {
        return new UiOpResult(Status.BLOCKED, message);
    }

    public static UiOpResult notBound(String message) {
        return new UiOpResult(Status.NOT_BOUND, message);
    }

    public static UiOpResult unavailable(String message) {
        return new UiOpResult(Status.UNAVAILABLE, message);
    }

    public boolean isOk() {
        return status == Status.OK;
    }

    @Override
    public String toString() {
        return "UiOpResult{" + status + (message.isEmpty() ? "" : "," + message) + "}";
    }
}
