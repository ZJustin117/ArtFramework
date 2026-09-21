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
    private RenderPhase phase;
    private String stableKey;
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
        this.phase = defaultPhase(kind);
        this.stableKey = id;
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

    public RenderPhase phase() {
        return phase;
    }

    public String stableKey() {
        return stableKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    void setBounds(Rect rect) {
        if (rect == null) {
            return;
        }
        setBounds(rect.x, rect.y, rect.width, rect.height);
    }

    void setZ(float z) {
        this.z = z;
    }

    void setOrder(RenderPhase phase, String stableKey) {
        if (phase == null) throw new IllegalArgumentException("render phase required");
        if (stableKey == null || stableKey.isEmpty()) {
            throw new IllegalArgumentException("render stable key required");
        }
        this.phase = phase;
        this.stableKey = stableKey;
    }

    private static RenderPhase defaultPhase(RenderTargetKind kind) {
        if (kind == RenderTargetKind.SYNTHETIC_WINDOW
                || kind == RenderTargetKind.SYNTHETIC_WIDGET) return RenderPhase.C1_CONTENT;
        if (kind == RenderTargetKind.ENTITY_SLOT) return RenderPhase.ENTITY_CONTENT;
        if (kind == RenderTargetKind.FULL_FRAME || kind == RenderTargetKind.C2_SURFACE) {
            return RenderPhase.C2_CONTENT;
        }
        return RenderPhase.ART_EFFECTS;
    }

    public Rect bounds() {
        return new Rect(x, y, width, height);
    }
}
