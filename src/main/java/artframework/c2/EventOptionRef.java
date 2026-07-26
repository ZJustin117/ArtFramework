package artframework.c2;

/** Index into an event dialog option list (no STS event types). */
public final class EventOptionRef {

    public final int index;
    public final String label;

    public EventOptionRef(int index, String label) {
        this.index = index;
        this.label = label != null ? label : "";
    }

    @Override
    public String toString() {
        return "EventOptionRef{" + index + "," + label + "}";
    }
}
