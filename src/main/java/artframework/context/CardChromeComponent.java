package artframework.context;

/** Immutable host-neutral card text/chrome data. */
public final class CardChromeComponent {
    public final String title;
    public final String cost;
    public final String type;
    public final String description;

    public CardChromeComponent(String title, String cost, String type, String description) {
        this.title = value(title);
        this.cost = value(cost);
        this.type = value(type);
        this.description = value(description);
    }

    private static String value(String value) { return value != null ? value : ""; }
}
