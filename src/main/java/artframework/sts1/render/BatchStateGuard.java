package artframework.sts1.render;

/**
 * Records whether a SpriteBatch was drawing so host code can end/begin around ART draws and
 * restore afterward (milestone 16.3). Pure bookkeeping — no GL types on this API.
 */
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
