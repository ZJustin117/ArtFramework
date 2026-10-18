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
    public final String title;
    public final String cost;
    public final String type;
    public final String description;
    /** Native card color enum constant name (e.g. {@code RED}/{@code COLORLESS}); default {@code ""}. */
    public final String color;
    /** Native card rarity enum constant name (e.g. {@code BASIC}/{@code RARE}); default {@code ""}. */
    public final String rarity;
    /**
     * True when the native card cost permits the ENERGY ORB draw ({@code AbstractCard.cost > -2});
     * fail-open {@code true} when cost is unknown/absent.
     */
    public final boolean orbDrawable;
    /**
     * Native {@code AbstractCard.isCostModified || isCostModifiedForTurn || freeToPlay()}: the cost
     * number is drawn GREEN. Fail-open {@code false}.
     */
    public final boolean costModified;
    /**
     * Native {@code player.hand.contains(card) && !card.hasEnoughEnergy()}: the cost number is drawn
     * RED, and this branch takes precedence over {@link #costModified}. Fail-open {@code false}.
     */
    public final boolean costRestricted;
    /**
     * U06: native {@code AbstractCard.upgraded} (public field). Native {@code renderTitle} draws the
     * title GREEN when the card is upgraded ({@code card.upgraded} -&gt;
     * {@code Settings.GREEN_TEXT_COLOR}); otherwise it uses the card's own {@code renderColor}.
     * Fail-open {@code false}.
     */
    public final boolean upgraded;
    /**
     * U06: native {@code AbstractCard.useSmallTitleFont} (PRIVATE field, read by the backend through
     * guarded reflection). Native {@code renderTitle} multiplies the title font scale by
     * {@code 0.85f} when set (the non-default-language small-title path). Fail-open {@code false}.
     */
    public final boolean smallTitleFont;
    /**
     * U06: native {@code AbstractCard.renderColor.a} (PRIVATE {@code Color} field, read by the
     * backend through guarded reflection). Native {@code renderTitle} draws the title with that
     * alpha. Fail-open {@code 1f}.
     */
    public final float titleAlpha;

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
        this(ref, zone, slotIndex, pose, playable, selected, hovered, dragging,
                artResourceId, frameResourceId, ref.cardId, "", "", "", "", "", true, false, false);
    }

    public CardView(
            CardRef ref, CardZone zone, int slotIndex, CardPose pose, boolean playable,
            boolean selected, boolean hovered, boolean dragging, String artResourceId,
            String frameResourceId, String title, String cost, String type, String description) {
        this(ref, zone, slotIndex, pose, playable, selected, hovered, dragging, artResourceId,
                frameResourceId, title, cost, type, description, "", "", true, false, false);
    }

    public CardView(
            CardRef ref, CardZone zone, int slotIndex, CardPose pose, boolean playable,
            boolean selected, boolean hovered, boolean dragging, String artResourceId,
            String frameResourceId, String title, String cost, String type, String description,
            String color) {
        this(ref, zone, slotIndex, pose, playable, selected, hovered, dragging, artResourceId,
                frameResourceId, title, cost, type, description, color, "", true, false, false);
    }

    public CardView(
            CardRef ref, CardZone zone, int slotIndex, CardPose pose, boolean playable,
            boolean selected, boolean hovered, boolean dragging, String artResourceId,
            String frameResourceId, String title, String cost, String type, String description,
            String color, String rarity) {
        this(ref, zone, slotIndex, pose, playable, selected, hovered, dragging, artResourceId,
                frameResourceId, title, cost, type, description, color, rarity, true, false, false);
    }

    public CardView(
            CardRef ref, CardZone zone, int slotIndex, CardPose pose, boolean playable,
            boolean selected, boolean hovered, boolean dragging, String artResourceId,
            String frameResourceId, String title, String cost, String type, String description,
            String color, String rarity, boolean orbDrawable) {
        this(ref, zone, slotIndex, pose, playable, selected, hovered, dragging, artResourceId,
                frameResourceId, title, cost, type, description, color, rarity, orbDrawable,
                false, false);
    }

    public CardView(
            CardRef ref, CardZone zone, int slotIndex, CardPose pose, boolean playable,
            boolean selected, boolean hovered, boolean dragging, String artResourceId,
            String frameResourceId, String title, String cost, String type, String description,
            String color, String rarity, boolean orbDrawable, boolean costModified,
            boolean costRestricted) {
        this(ref, zone, slotIndex, pose, playable, selected, hovered, dragging, artResourceId,
                frameResourceId, title, cost, type, description, color, rarity, orbDrawable,
                costModified, costRestricted, false, false, 1f);
    }

    /**
     * U06 full carrier. The three title fields are added at the END so every pre-existing argument
     * order is untouched; the shorter constructors delegate here with the fail-open defaults
     * {@code false, false, 1f}.
     */
    public CardView(
            CardRef ref, CardZone zone, int slotIndex, CardPose pose, boolean playable,
            boolean selected, boolean hovered, boolean dragging, String artResourceId,
            String frameResourceId, String title, String cost, String type, String description,
            String color, String rarity, boolean orbDrawable, boolean costModified,
            boolean costRestricted, boolean upgraded, boolean smallTitleFont, float titleAlpha) {
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
        this.title = title != null ? title : ref.cardId;
        this.cost = cost != null ? cost : "";
        this.type = type != null ? type : "";
        this.description = description != null ? description : "";
        this.color = color != null ? color : "";
        this.rarity = rarity != null ? rarity : "";
        this.orbDrawable = orbDrawable;
        this.costModified = costModified;
        this.costRestricted = costRestricted;
        this.upgraded = upgraded;
        this.smallTitleFont = smallTitleFont;
        this.titleAlpha = titleAlpha;
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
        private String title;
        private String cost = "";
        private String type = "";
        private String description = "";
        private String color = "";
        private String rarity = "";
        private boolean orbDrawable = true;
        private boolean costModified = false;
        private boolean costRestricted = false;
        private boolean upgraded = false;
        private boolean smallTitleFont = false;
        private float titleAlpha = 1f;

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

        public Builder title(String value) { this.title = value; return this; }
        public Builder cost(String value) { this.cost = value; return this; }
        public Builder type(String value) { this.type = value; return this; }
        public Builder description(String value) { this.description = value; return this; }
        public Builder color(String value) { this.color = value; return this; }
        public Builder rarity(String value) { this.rarity = value; return this; }
        public Builder orbDrawable(boolean value) { this.orbDrawable = value; return this; }
        public Builder costModified(boolean value) { this.costModified = value; return this; }
        public Builder costRestricted(boolean value) { this.costRestricted = value; return this; }
        public Builder upgraded(boolean value) { this.upgraded = value; return this; }
        public Builder smallTitleFont(boolean value) { this.smallTitleFont = value; return this; }
        public Builder titleAlpha(float value) { this.titleAlpha = value; return this; }

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
                    artResourceId, frameResourceId, title != null ? title : ref.cardId,
                    cost, type, description, color, rarity, orbDrawable, costModified,
                    costRestricted, upgraded, smallTitleFont, titleAlpha);
        }
    }
}
