package artframework.context;

/** Derived card interaction state. */
public final class CardInteractionComponent {
    public final boolean playable;
    public final boolean selected;
    public final boolean hovered;
    public final boolean dragging;

    public CardInteractionComponent(boolean playable, boolean selected, boolean hovered, boolean dragging) {
        this.playable = playable;
        this.selected = selected;
        this.hovered = hovered;
        this.dragging = dragging;
    }
}
