package artframework.sts1.render;

/** Immutable proof that ART drew the result of a delegated native invocation. */
public final class PresentationDrawEvidence {
    public final long invocationId;
    public final String entityId;
    public final long frameId;
    public final int drawCount;
    public final String cleanupState;

    public PresentationDrawEvidence(long invocationId, String entityId, long frameId,
            int drawCount, String cleanupState) {
        this.invocationId = invocationId;
        this.entityId = entityId != null ? entityId : "";
        this.frameId = frameId;
        this.drawCount = drawCount;
        this.cleanupState = cleanupState != null ? cleanupState : "active";
    }
}
