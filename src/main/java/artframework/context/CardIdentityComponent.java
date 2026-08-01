package artframework.context;

/** Stable card identity projected from a backend frame. */
public final class CardIdentityComponent {
    public final String instanceId;
    public final String cardId;

    public CardIdentityComponent(String instanceId, String cardId) {
        this.instanceId = instanceId;
        this.cardId = cardId;
    }
}
