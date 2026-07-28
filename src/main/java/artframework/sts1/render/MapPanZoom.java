package artframework.sts1.render;

import java.util.LinkedHashMap;
import java.util.Map;

/** Mutable pan/zoom for map full-present (16.7). Present-space only; not dungeon authority. */
public final class MapPanZoom {

    private float panX;
    private float panY;
    private float zoom = 1f;
    private float minZoom = 0.5f;
    private float maxZoom = 2.5f;

    public float panX() {
        return panX;
    }

    public float panY() {
        return panY;
    }

    public float zoom() {
        return zoom;
    }

    public void panBy(float dx, float dy) {
        panX += dx;
        panY += dy;
    }

    public void setPan(float x, float y) {
        panX = x;
        panY = y;
    }

    public void setZoom(float z) {
        if (z < minZoom) {
            z = minZoom;
        }
        if (z > maxZoom) {
            z = maxZoom;
        }
        zoom = z;
    }

    public void zoomBy(float factor) {
        setZoom(zoom * factor);
    }

    public void setZoomLimits(float min, float max) {
        minZoom = min > 0f ? min : 0.1f;
        maxZoom = max >= minZoom ? max : minZoom;
        setZoom(zoom);
    }

    /** Map frame local → present screen. */
    public float toScreenX(float localX) {
        return localX * zoom + panX;
    }

    public float toScreenY(float localY) {
        return localY * zoom + panY;
    }

    public float toLocalX(float screenX) {
        return (screenX - panX) / (zoom == 0f ? 1f : zoom);
    }

    public float toLocalY(float screenY) {
        return (screenY - panY) / (zoom == 0f ? 1f : zoom);
    }

    public void reset() {
        panX = 0f;
        panY = 0f;
        zoom = 1f;
        minZoom = 0.5f;
        maxZoom = 2.5f;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("panX", Float.valueOf(panX));
        m.put("panY", Float.valueOf(panY));
        m.put("zoom", Float.valueOf(zoom));
        m.put("minZoom", Float.valueOf(minZoom));
        m.put("maxZoom", Float.valueOf(maxZoom));
        return m;
    }
}
