package artframework.c2;

import artframework.component.MapNodeRef;

/** Data-only map pin state projected from the C2 map template. */
public final class MapPinComponent {
    public final String pinId;
    public final MapNodeRef node;
    public final String label;

    public MapPinComponent(String pinId, MapNodeRef node, String label) {
        if (pinId == null || pinId.isEmpty()) throw new IllegalArgumentException("pinId required");
        if (node == null) throw new IllegalArgumentException("node required");
        this.pinId = pinId;
        this.node = node;
        this.label = label != null ? label : "";
    }

    public MapPin toPin() { return new MapPin(pinId, node, label); }
}
