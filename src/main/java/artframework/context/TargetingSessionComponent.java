package artframework.context;

/** Data-only targeting session state for the C2 context projection. */
public final class TargetingSessionComponent {

    public static final TargetingSessionComponent EMPTY =
            new TargetingSessionComponent(false, null, null, Phase.ARMED);

    public final boolean active;
    public final String cardInstanceId;
    public final String targetKey;
    public final Phase phase;

    public TargetingSessionComponent(boolean active, String cardInstanceId, String targetKey, Phase phase) {
        this.active = active;
        this.cardInstanceId = cardInstanceId != null ? cardInstanceId : "";
        this.targetKey = targetKey != null ? targetKey : "";
        this.phase = phase != null ? phase : Phase.ARMED;
    }

    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<String, Object>();
        m.put("active", Boolean.valueOf(active));
        m.put("cardInstanceId", cardInstanceId);
        m.put("targetKey", targetKey);
        m.put("phase", phase.name());
        return m;
    }

    public enum Phase {
        ARMED,
        VALID,
        COMMITTED
    }
}
