package artframework.render;

import artframework.component.Rect;

/**
 * One drawable region owned by ArtFramework (not full STS frame unless FULL_FRAME).
 */
public final class RenderTarget {

    public final String id;
    public final RenderTargetKind kind;
    private float x;
    private float y;
    private float width;
    private float height;
    private float z;
    private boolean enabled = true;

    public RenderTarget(String id, RenderTargetKind kind) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id required");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind required");
        }
        this.id = id;
        this.kind = kind;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public float z() {
        return z;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setBounds(Rect rect) {
        if (rect == null) {
            return;
        }
        setBounds(rect.x, rect.y, rect.width, rect.height);
    }

    public void setZ(float z) {
        this.z = z;
    }

    public Rect bounds() {
        return new Rect(x, y, width, height);
    }
}
