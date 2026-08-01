package artframework.context;

/** Card zone, ordering, and pose projected from a backend frame. */
public final class CardPlacementComponent {
    public final CardZone zone;
    public final int slotIndex;
    public final CardPose pose;

    public CardPlacementComponent(CardZone zone, int slotIndex, CardPose pose) {
        this.zone = zone != null ? zone : CardZone.OTHER;
        this.slotIndex = slotIndex;
        this.pose = pose != null ? pose : CardPose.at(0f, 0f);
    }
}
