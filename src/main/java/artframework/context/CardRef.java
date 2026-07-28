package artframework.context;

/**
 * Stable card identity for present/intents. Prefer {@link #instanceId} over type id alone.
 */
public final class CardRef {

    public final String instanceId;
    public final String cardId;

    public CardRef(String instanceId, String cardId) {
        if (instanceId == null || instanceId.isEmpty()) {
            throw new IllegalArgumentException("instanceId required");
        }
        this.instanceId = instanceId;
        this.cardId = cardId != null ? cardId : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CardRef)) {
            return false;
        }
        CardRef other = (CardRef) o;
        return instanceId.equals(other.instanceId);
    }

    @Override
    public int hashCode() {
        return instanceId.hashCode();
    }

    @Override
    public String toString() {
        return "CardRef{" + instanceId + "," + cardId + "}";
    }
}
