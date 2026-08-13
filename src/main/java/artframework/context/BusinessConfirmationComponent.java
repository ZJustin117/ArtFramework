package artframework.context;

/** Data-only business confirmation for a native intent after an authority snapshot. */
public final class BusinessConfirmationComponent {
    public enum State { PENDING, CONFIRMED, FAILED }
    public enum Domain { CARD, MAP, EVENT, REWARD, SELECT, ROOM, GENERIC }

    public final String intent;
    public final Domain domain;
    public final State state;
    public final long requestedFrame;
    public final long requestedEpoch;
    public final long observedFrame;
    public final long observedEpoch;
    public final String evidence;

    public BusinessConfirmationComponent(String intent, Domain domain, State state,
            long requestedFrame, long requestedEpoch, long observedFrame, long observedEpoch,
            String evidence) {
        this.intent = intent != null ? intent : "";
        this.domain = domain != null ? domain : Domain.GENERIC;
        this.state = state != null ? state : State.PENDING;
        this.requestedFrame = requestedFrame;
        this.requestedEpoch = requestedEpoch;
        this.observedFrame = observedFrame;
        this.observedEpoch = observedEpoch;
        this.evidence = evidence != null ? evidence : "";
    }

    public BusinessConfirmationComponent observe(State next, long frame, long epoch, String proof) {
        return new BusinessConfirmationComponent(intent, domain, next, requestedFrame,
                requestedEpoch, frame, epoch, proof);
    }

    public static Domain domain(String surfaceId, String intent) {
        String value = (surfaceId != null ? surfaceId : "") + ":" + (intent != null ? intent : "");
        if (value.contains("card") || value.contains("drag") || value.contains("play")) return Domain.CARD;
        if (value.contains("map") || value.contains("room")) return Domain.MAP;
        if (value.contains("event")) return Domain.EVENT;
        if (value.contains("reward")) return Domain.REWARD;
        if (value.contains("select")) return Domain.SELECT;
        return Domain.GENERIC;
    }
}
