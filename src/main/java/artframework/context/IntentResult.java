package artframework.context;

/**
 * Immediate return-channel outcome. Final pixels come from the next authority frame only.
 */
public final class IntentResult {

    public enum Status {
        ACCEPTED,
        REJECTED,
        QUEUED
    }

    public final Status status;
    public final String message;

    private IntentResult(Status status, String message) {
        this.status = status;
        this.message = message != null ? message : "";
    }

    public static IntentResult accepted() {
        return new IntentResult(Status.ACCEPTED, "");
    }

    public static IntentResult accepted(String message) {
        return new IntentResult(Status.ACCEPTED, message);
    }

    public static IntentResult rejected(String message) {
        return new IntentResult(Status.REJECTED, message);
    }

    public static IntentResult queued(String message) {
        return new IntentResult(Status.QUEUED, message);
    }

    static IntentResult of(Status status, String message) {
        return new IntentResult(status, message);
    }

    public boolean isAccepted() {
        return status == Status.ACCEPTED;
    }

    @Override
    public String toString() {
        return "IntentResult{" + status + (message.isEmpty() ? "" : "," + message) + "}";
    }
}
