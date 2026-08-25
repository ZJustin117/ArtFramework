package artframework.sts1.render;

/** One explicit continuation decision for a native render invocation. */
public final class RenderDisposition {
    public enum Mode {
        PASS_THROUGH,
        CAPTURE_AND_PASS,
        DELEGATE_TO_ART,
        FAIL_OPEN
    }

    public final long invocationId;
    public final Mode mode;
    public final String reason;
    public final boolean nativeContinuation;
    public final String presentationEntityId;

    private RenderDisposition(long invocationId, Mode mode, String reason,
            boolean nativeContinuation, String presentationEntityId) {
        this.invocationId = invocationId;
        this.mode = mode;
        this.reason = reason != null ? reason : "";
        this.nativeContinuation = nativeContinuation;
        this.presentationEntityId = presentationEntityId;
    }

    public static RenderDisposition pass(long id, String reason) {
        return new RenderDisposition(id, Mode.PASS_THROUGH, reason, true, null);
    }

    public static RenderDisposition capture(long id, String reason) {
        return new RenderDisposition(id, Mode.CAPTURE_AND_PASS, reason, true, null);
    }

    public static RenderDisposition delegate(long id, String reason, String entityId) {
        if (entityId == null || entityId.isEmpty()) {
            throw new IllegalArgumentException("delegated presentation entity required");
        }
        return new RenderDisposition(id, Mode.DELEGATE_TO_ART, reason, false, entityId);
    }

    public static RenderDisposition failOpen(long id, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("fail-open reason required");
        }
        return new RenderDisposition(id, Mode.FAIL_OPEN, reason, true, null);
    }
}
