package artframework.component;

/** Semantic STS node classification shared by probes and generic synthetic UiOps. */
public final class StsNodeTypes {

    private StsNodeTypes() {}

    public static boolean isPressable(String type) {
        return ArtNodeTypes.STS_BUTTON.equals(type)
                || ArtNodeTypes.STS_CARD.equals(type)
                || ArtNodeTypes.STS_MAP_NODE.equals(type)
                || ArtNodeTypes.STS_EVENT_OPTION.equals(type)
                || ArtNodeTypes.STS_REWARD_ITEM.equals(type)
                || ArtNodeTypes.STS_ROOM_ACTION.equals(type);
    }
}
