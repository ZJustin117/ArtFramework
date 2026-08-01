package artframework.context;

/** Host-neutral resource identifiers for a card projection. */
public final class CardAssetsComponent {
    public final String artResourceId;
    public final String frameResourceId;

    public CardAssetsComponent(String artResourceId, String frameResourceId) {
        this.artResourceId = artResourceId != null ? artResourceId : "";
        this.frameResourceId = frameResourceId != null ? frameResourceId : "";
    }
}
