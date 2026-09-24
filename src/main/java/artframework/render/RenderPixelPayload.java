package artframework.render;

import artframework.component.Rect;

/**
 * Immutable, host-neutral pixel payload for one render frame entry.
 *
 * <p>This is data only: it describes which resource to draw and how, but it never owns or
 * references a host object (no {@code Texture}, {@code AssetManager}, {@code SpriteBatch},
 * {@code Actor}, or callback). Resolution and submission stay in the ART backend. The payload is
 * deliberately <em>not</em> part of frame ordering or identity: phase/z/stableKey decide order,
 * and the payload must never be read as an ownership or delegation claim.</p>
 *
 * <p>All numeric values are finite; source and destination dimensions are non-negative. Resource
 * and label strings may be {@code null}, which is normalized to the empty string. The source
 * rectangle is expressed in normalized UV space so a producer does not need texture dimensions.</p>
 */
public final class RenderPixelPayload {
    private final String resourceId;
    private final String label;
    private final String blendMode;
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final float sourceX;
    private final float sourceY;
    private final float sourceWidth;
    private final float sourceHeight;
    private final boolean flipX;
    private final boolean flipY;
    private final float rotationDegrees;
    private final float scaleX;
    private final float scaleY;
    private final float r;
    private final float g;
    private final float b;
    private final float a;
    private final int flipbookFrame;
    private final int flipbookColumns;
    private final int flipbookRows;

    public RenderPixelPayload(String resourceId, String label,
            float x, float y, float width, float height,
            float sourceX, float sourceY, float sourceWidth, float sourceHeight,
            boolean flipX, boolean flipY, float rotationDegrees, float scaleX, float scaleY,
            float r, float g, float b, float a, String blendMode,
            int flipbookFrame, int flipbookColumns, int flipbookRows) {
        this.resourceId = normalize(resourceId);
        this.label = normalize(label);
        this.blendMode = normalize(blendMode);
        this.x = finite(x, "x");
        this.y = finite(y, "y");
        this.width = nonNegative(width, "width");
        this.height = nonNegative(height, "height");
        this.sourceX = finite(sourceX, "source x");
        this.sourceY = finite(sourceY, "source y");
        this.sourceWidth = nonNegative(sourceWidth, "source width");
        this.sourceHeight = nonNegative(sourceHeight, "source height");
        this.flipX = flipX;
        this.flipY = flipY;
        this.rotationDegrees = finite(rotationDegrees, "rotation");
        this.scaleX = finite(scaleX, "scale x");
        this.scaleY = finite(scaleY, "scale y");
        this.r = finite(r, "red");
        this.g = finite(g, "green");
        this.b = finite(b, "blue");
        this.a = finite(a, "alpha");
        if (flipbookFrame < 0) throw new IllegalArgumentException("flipbook frame must be non-negative");
        if (flipbookColumns < 0) throw new IllegalArgumentException("flipbook columns must be non-negative");
        if (flipbookRows < 0) throw new IllegalArgumentException("flipbook rows must be non-negative");
        this.flipbookFrame = flipbookFrame;
        this.flipbookColumns = flipbookColumns;
        this.flipbookRows = flipbookRows;
    }

    public String resourceId() { return resourceId; }
    public String label() { return label; }
    public String blendMode() { return blendMode; }
    public float x() { return x; }
    public float y() { return y; }
    public float width() { return width; }
    public float height() { return height; }
    public float sourceX() { return sourceX; }
    public float sourceY() { return sourceY; }
    public float sourceWidth() { return sourceWidth; }
    public float sourceHeight() { return sourceHeight; }
    public boolean flipX() { return flipX; }
    public boolean flipY() { return flipY; }
    public float rotationDegrees() { return rotationDegrees; }
    public float scaleX() { return scaleX; }
    public float scaleY() { return scaleY; }
    public float r() { return r; }
    public float g() { return g; }
    public float b() { return b; }
    public float a() { return a; }
    public int flipbookFrame() { return flipbookFrame; }
    public int flipbookColumns() { return flipbookColumns; }
    public int flipbookRows() { return flipbookRows; }

    /** Defensive copy of the normalized source rectangle (UV space). */
    public Rect sourceRect() {
        return new Rect(sourceX, sourceY, sourceWidth, sourceHeight);
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }

    private static float finite(float value, String name) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        return value;
    }

    private static float nonNegative(float value, String name) {
        finite(value, name);
        if (value < 0f) throw new IllegalArgumentException(name + " must be non-negative");
        return value;
    }
}
