package artframework.c2;

/**
 * Decorative pin on a map node (party marker, vote, etc.). Presentation only — no authority.
 */
public final class MapPin {

    public final String pinId;
    public final MapNodeRef node;
    /** Consumer-defined label or style key; empty allowed. */
    public final String label;

    public MapPin(String pinId, MapNodeRef node, String label) {
        if (pinId == null || pinId.isEmpty()) {
            throw new IllegalArgumentException("pinId required");
        }
        if (node == null) {
            throw new IllegalArgumentException("node required");
        }
        this.pinId = pinId;
        this.node = node;
        this.label = label != null ? label : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MapPin)) {
            return false;
        }
        return pinId.equals(((MapPin) o).pinId);
    }

    @Override
    public int hashCode() {
        return pinId.hashCode();
    }
}
