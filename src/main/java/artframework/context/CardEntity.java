package artframework.context;

/** Mutable ART projection row for one card instance (derived from frames). */
public final class CardEntity {

    static final int IDENTITY_CHANGED = 1;
    static final int PLACEMENT_CHANGED = 1 << 1;
    static final int INTERACTION_CHANGED = 1 << 2;
    static final int ASSETS_CHANGED = 1 << 3;
    static final int CHROME_CHANGED = 1 << 4;

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
    public String title;
    public String cost;
    public String type;
    public String description;

    public CardEntity(String instanceId) {
        this.instanceId = instanceId;
    }

    int apply(CardView view) {
        int changes = 0;
        if (!same(cardId, view.ref.cardId)) {
            changes |= IDENTITY_CHANGED;
        }
        if (zone != view.zone || slotIndex != view.slotIndex || !same(pose, view.pose)) {
            changes |= PLACEMENT_CHANGED;
        }
        if (playable != view.playable
                || selected != view.selected
                || hovered != view.hovered
                || dragging != view.dragging) {
            changes |= INTERACTION_CHANGED;
        }
        if (!same(artResourceId, view.artResourceId)
                || !same(frameResourceId, view.frameResourceId)) {
            changes |= ASSETS_CHANGED;
        }
        if (!same(title, view.title) || !same(cost, view.cost) || !same(type, view.type)
                || !same(description, view.description)) changes |= CHROME_CHANGED;
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
        this.title = view.title;
        this.cost = view.cost;
        this.type = view.type;
        this.description = view.description;
        return changes;
    }

    private static boolean same(Object left, Object right) {
        return left == right || (left != null && left.equals(right));
    }
}
