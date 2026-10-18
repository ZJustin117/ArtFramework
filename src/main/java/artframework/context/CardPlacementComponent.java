package artframework.context;

/** Card zone, ordering, and pose projected from a backend frame. */
public final class CardPlacementComponent {
    public final CardZone zone;
    public final int slotIndex;
    /**
     * Layout pose supplied by the frame, or {@code null} when it supplied none. See
     * {@link CardView#pose} for why absence is preserved.
     */
    public final CardPose pose;

    public CardPlacementComponent(CardZone zone, int slotIndex, CardPose pose) {
        this.zone = zone != null ? zone : CardZone.OTHER;
        this.slotIndex = slotIndex;
        this.pose = pose;
    }
}
