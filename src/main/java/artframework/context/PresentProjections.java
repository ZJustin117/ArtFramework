package artframework.context;


/** Global presentation projection, advanced directly by authority frame ingestion. */
public final class PresentProjections {
    private static final PresentProjection PROJECTION = new PresentProjection();
    private static FrameDiff last = FrameDiff.skipped("no frame signal received");

    private PresentProjections() {}

    public static PresentProjection get() {
        return PROJECTION;
    }

    public static FrameDiff publish(ContextFrame frame) {
        if (frame == null) return FrameDiff.skipped("frame required");
        last = PROJECTION.applyFrame(frame);
        return last;
    }

    /** ECS schedule entry point; bypasses the compatibility frame signal. */
    static FrameDiff publishDirect(ContextFrame frame) {
        if (frame == null) return FrameDiff.skipped("frame required");
        last = PROJECTION.applyFrame(frame, false);
        return last;
    }

    public static void resetForTests() {
        PROJECTION.reset();
    }
}
