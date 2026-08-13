package artframework.c2;

/** Data-only event template state observed from the native host adapter. */
public final class EventTemplateDataComponent {
    public final String eventId;

    public EventTemplateDataComponent(String eventId) {
        this.eventId = eventId != null ? eventId : "";
    }
}
