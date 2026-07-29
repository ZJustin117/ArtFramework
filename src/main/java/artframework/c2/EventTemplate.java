package artframework.c2;

/** C2 event lifecycle state; option policy lives on the shared SignalBus. */
public final class EventTemplate {
    public static final String RESOURCE = NativeTemplateIds.EVENT;
    private boolean active;
    private String eventId = "";
    public void activate() { active = true; }
    public void deactivate() { active = false; eventId = ""; }
    public boolean isActive() { return active; }
    public void setEventId(String value) { eventId = value != null ? value : ""; }
    public String getEventId() { return eventId; }
    void resetForTests() { active = false; eventId = ""; }
}
