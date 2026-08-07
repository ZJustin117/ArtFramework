package artframework.component;

/**
 * Built-in ART presentation node type names (beyond classic UI controls).
 */
public final class ArtNodeTypes {

    public static final String ANIMATION_PLAYER = "art.animation_player";
    public static final String SHADER_EFFECT = "art.shader_effect";
    public static final String SKELETON = "art.skeleton";
    /** Present scope node: props profile/id + mode attach|override. */
    public static final String PRESENT_PROFILE = "art.present_profile";
    /** STS1-hosted vanilla controls. Their props use ResourceIds, never host file paths. */
    public static final String STS_BUTTON = "art.sts.button";
    public static final String STS_PANEL = "art.sts.panel";
    public static final String STS_CARD = "art.sts.card";
    public static final String STS_ENERGY_ORB = "art.sts.energy_orb";
    public static final String STS_INTENT = "art.sts.intent";
    public static final String STS_TOP_PANEL = "art.sts.top_panel";
    public static final String STS_MAP = "art.sts.map";
    public static final String STS_MAP_NODE = "art.sts.map_node";
    public static final String STS_EVENT_OPTION = "art.sts.event_option";
    public static final String STS_REWARD_ITEM = "art.sts.reward_item";
    public static final String STS_ROOM_ACTION = "art.sts.room_action";

    private ArtNodeTypes() {}
}
