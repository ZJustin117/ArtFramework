package artframework.context;

/** Immutable viewport contract for a presentation frame. */
public final class ViewportView {

    public final int logicalWidth;
    public final int logicalHeight;
    public final int pixelWidth;
    public final int pixelHeight;

    public ViewportView(int logicalWidth, int logicalHeight, int pixelWidth, int pixelHeight) {
        this.logicalWidth = Math.max(0, logicalWidth);
        this.logicalHeight = Math.max(0, logicalHeight);
        this.pixelWidth = Math.max(0, pixelWidth);
        this.pixelHeight = Math.max(0, pixelHeight);
    }

    public static ViewportView unavailable() {
        return new ViewportView(0, 0, 0, 0);
    }
}
