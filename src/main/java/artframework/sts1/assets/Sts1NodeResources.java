package artframework.sts1.assets;

import artframework.assets.ResourceIds;
import artframework.component.ArtNodeTypes;
import artframework.component.UiNode;

/**
 * Pure mapping from public semantic STS node props to stable HostAssets keys.
 * Layout authors never refer to STS image paths.
 */
public final class Sts1NodeResources {

    private Sts1NodeResources() {}

    public static String primaryResource(UiNode node) {
        if (node == null) {
            return "";
        }
        String explicit = node.propString("resource_id", "");
        if (!explicit.isEmpty()) {
            return explicit;
        }
        if (ArtNodeTypes.STS_BUTTON.equals(node.type)) {
            return "end_turn".equals(node.propString("variant", ""))
                    ? ResourceIds.UI_BUTTON_END_TURN : ResourceIds.UI_BUTTON_DEFAULT;
        }
        if (ArtNodeTypes.STS_PANEL.equals(node.type)) {
            return ResourceIds.UI_PANEL_DEFAULT;
        }
        if (ArtNodeTypes.STS_CARD.equals(node.type)) {
            return ResourceIds.UI_REWARD_CARD;
        }
        if (ArtNodeTypes.STS_INTENT.equals(node.type)) {
            return node.propString("resource_id", ResourceIds.UI_COMBAT_INTENT_UNKNOWN);
        }
        if (ArtNodeTypes.STS_ENERGY_ORB.equals(node.type)) {
            return ResourceIds.energyOrb(node.propString("color", "red"));
        }
        if (ArtNodeTypes.STS_TOP_PANEL.equals(node.type)) {
            return ResourceIds.UI_TOP_PANEL_BAR;
        }
        if (ArtNodeTypes.STS_MAP.equals(node.type)) {
            return ResourceIds.MAP_BG_PREFIX + node.propString("act", "act1");
        }
        if (ArtNodeTypes.STS_MAP_NODE.equals(node.type)) {
            return ResourceIds.mapNode(node.propString("kind", "monster"));
        }
        if (ArtNodeTypes.STS_EVENT_OPTION.equals(node.type)) {
            return node.propBool("enabled", true)
                    ? ResourceIds.UI_EVENT_BUTTON_ENABLED : ResourceIds.UI_EVENT_BUTTON_DISABLED;
        }
        if (ArtNodeTypes.STS_REWARD_ITEM.equals(node.type)) {
            return ResourceIds.UI_REWARD_CARD;
        }
        if (ArtNodeTypes.STS_ROOM_ACTION.equals(node.type)) {
            String room = node.propString("room", "");
            if ("event".equals(room)) {
                return ResourceIds.UI_EVENT_PANEL;
            }
            if ("reward".equals(room)) {
                return ResourceIds.UI_REWARD_PANEL;
            }
            if ("rest".equals(room)) {
                return ResourceIds.UI_CAMPFIRE_OUTLINE;
            }
            return ResourceIds.UI_PANEL_DEFAULT;
        }
        return "";
    }

    public static String hoverResource(UiNode node) {
        if (node != null && ArtNodeTypes.STS_BUTTON.equals(node.type)
                && "end_turn".equals(node.propString("variant", ""))) {
            return ResourceIds.UI_BUTTON_END_TURN_HOVER;
        }
        return "";
    }
}
