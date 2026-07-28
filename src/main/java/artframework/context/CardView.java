package artframework.context;

/**
 * Immutable per-card row in a {@link ContextFrame}. No STS engine types.
 */
public final class CardView {

    public final CardRef ref;
    public final CardZone zone;
    public final int slotIndex;
    public final CardPose pose;
    public final boolean playable;
    public final boolean selected;
    public final boolean hovered;
    public final boolean dragging;
    public final String artResourceId;
    public final String frameResourceId;

    public CardView(
            CardRef ref,
            CardZone zone,
            int slotIndex,
            CardPose pose,
            boolean playable,
            boolean selected,
            boolean hovered,
            boolean dragging,
            String artResourceId,
            String frameResourceId) {
        if (ref == null) {
            throw new IllegalArgumentException("ref required");
        }
        this.ref = ref;
        this.zone = zone != null ? zone : CardZone.OTHER;
        this.slotIndex = slotIndex;
        this.pose = pose != null ? pose : CardPose.at(0f, 0f);
        this.playable = playable;
        this.selected = selected;
        this.hovered = hovered;
        this.dragging = dragging;
        this.artResourceId = artResourceId != null ? artResourceId : "";
        this.frameResourceId = frameResourceId != null ? frameResourceId : "";
    }

    public static Builder builder(CardRef ref) {
        return new Builder(ref);
    }

    public static final class Builder {
        private final CardRef ref;
        private CardZone zone = CardZone.HAND;
        private int slotIndex;
        private CardPose pose = CardPose.at(0f, 0f);
        private boolean playable = true;
        private boolean selected;
        private boolean hovered;
        private boolean dragging;
        private String artResourceId = "";
        private String frameResourceId = "";

        Builder(CardRef ref) {
            this.ref = ref;
        }

        public Builder zone(CardZone z) {
            this.zone = z;
            return this;
        }

        public Builder slot(int index) {
            this.slotIndex = index;
            return this;
        }

        public Builder pose(CardPose p) {
            this.pose = p;
            return this;
        }

        public Builder playable(boolean v) {
            this.playable = v;
            return this;
        }

        public Builder selected(boolean v) {
            this.selected = v;
            return this;
        }

        public Builder hovered(boolean v) {
            this.hovered = v;
            return this;
        }

        public Builder dragging(boolean v) {
            this.dragging = v;
            return this;
        }

        public Builder art(String resourceId) {
            this.artResourceId = resourceId;
            return this;
        }

        public Builder frame(String resourceId) {
            this.frameResourceId = resourceId;
            return this;
        }

        public CardView build() {
            return new CardView(
                    ref,
                    zone,
                    slotIndex,
                    pose,
                    playable,
                    selected,
                    hovered,
                    dragging,
                    artResourceId,
                    frameResourceId);
        }
    }
}
