package artframework.context;

/** Mutable ART projection row for one card instance (derived from frames). */
public final class CardEntity {

    public final String instanceId;
    public String cardId;
    public CardZone zone;
    public int slotIndex;
    public CardPose pose;
    public boolean playable;
    public boolean selected;
    public boolean hovered;
    public boolean dragging;
    public String artResourceId;
    public String frameResourceId;

    public CardEntity(String instanceId) {
        this.instanceId = instanceId;
    }

    void apply(CardView view) {
        this.cardId = view.ref.cardId;
        this.zone = view.zone;
        this.slotIndex = view.slotIndex;
        this.pose = view.pose;
        this.playable = view.playable;
        this.selected = view.selected;
        this.hovered = view.hovered;
        this.dragging = view.dragging;
        this.artResourceId = view.artResourceId;
        this.frameResourceId = view.frameResourceId;
    }
}
