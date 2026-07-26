package artframework.c2;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * C2 event template: option-click intercept while {@code sts.event} is bound.
 * Patches to {@code AbstractEvent} come later.
 */
public final class EventTemplate {

    public static final String RESOURCE = NativeTemplateIds.EVENT;

    private final CopyOnWriteArrayList<EventOptionInterceptor> interceptors =
            new CopyOnWriteArrayList<EventOptionInterceptor>();
    private boolean active;
    private String eventId = "";

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
        eventId = "";
    }

    public boolean isActive() {
        return active;
    }

    /** Optional consumer hint for which event is showing. */
    public void setEventId(String eventId) {
        this.eventId = eventId != null ? eventId : "";
    }

    public String getEventId() {
        return eventId;
    }

    public void addInterceptor(EventOptionInterceptor interceptor) {
        if (interceptor == null) {
            throw new IllegalArgumentException("interceptor required");
        }
        interceptors.add(interceptor);
    }

    public void removeInterceptor(EventOptionInterceptor interceptor) {
        interceptors.remove(interceptor);
    }

    public GateResult dispatchOption(EventOptionRef option) {
        if (option == null) {
            throw new IllegalArgumentException("option required");
        }
        if (!active) {
            return GateResult.ALLOW;
        }
        for (EventOptionInterceptor interceptor : interceptors) {
            GateResult r = interceptor.intercept(option);
            if (r == GateResult.BLOCK) {
                return GateResult.BLOCK;
            }
        }
        return GateResult.ALLOW;
    }

    public int interceptorCount() {
        return interceptors.size();
    }

    void resetForTests() {
        interceptors.clear();
        active = false;
        eventId = "";
    }
}
