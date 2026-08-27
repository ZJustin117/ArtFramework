package artframework.sts1.render;

/**
 * Kept for historical probe compatibility. Production batch begin/end bookkeeping was removed
 * in Slice E1; the host SpriteBatch lifecycle is now managed by the caller and the skeleton
 * renderer.
 */
@Deprecated
public final class BatchStateGuard {

    private boolean wasDrawing;
    private boolean armed;
    private int nest;

    public void beginCapture(boolean batchWasDrawing) {
        if (nest == 0) {
            wasDrawing = batchWasDrawing;
            armed = true;
        }
        nest++;
    }

    public void endCapture() {
        if (nest <= 0) {
            return;
        }
        nest--;
        if (nest == 0) {
            armed = false;
            wasDrawing = false;
        }
    }

    public boolean isArmed() {
        return armed && nest > 0;
    }

    public boolean wasDrawing() {
        return wasDrawing;
    }

    /** True if caller should call batch.end() before ART work. */
    public boolean shouldEndBatch() {
        return isArmed() && wasDrawing;
    }

    /** True if caller should call batch.begin() after ART work to restore. */
    public boolean shouldRestoreBegin() {
        return isArmed() && wasDrawing;
    }

    public void reset() {
        wasDrawing = false;
        armed = false;
        nest = 0;
    }
}
