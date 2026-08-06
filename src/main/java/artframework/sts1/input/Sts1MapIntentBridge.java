package artframework.sts1.input;

import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import artframework.c2.MapNodeRef;
import artframework.context.IntentNames;
import artframework.context.IntentResult;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Schedules a native map-node gesture so STS retains path and transition authority.
 *
 * <p>InputHelper.updateFirst overwrites mX/mY/justClicked each frame. Gestures are therefore held
 * as sticky state and re-applied from a Postfix on InputHelper.updateFirst so MapRoomNode.update
 * observes the injected edge.
 */
public final class Sts1MapIntentBridge {

    private static volatile MapNodeRef pending;
    private static volatile int pendingFrames;
    private static volatile MapNodeRef lastTarget;
    private static volatile String lastStatus = "idle";
    private static volatile String lastEligibility = "";
    private static volatile int attempts;

    private Sts1MapIntentBridge() {}

    /** Lab helper: first presentable floor-0 node (valid hitbox, not offscreen sentinel). */
    public static IntentResult clickFirstPresentable() {
        return clickFirstPresentable("");
    }

    /** Lab helper: first presentable floor-0 node matching a symbol or room class hint. */
    public static IntentResult clickFirstPresentable(String roomKind) {
        try {
            if (AbstractDungeon.map == null) {
                lastStatus = "no_map";
                return IntentResult.rejected("no map");
            }
            MapNodeRef best = null;
            float bestX = Float.MAX_VALUE;
            for (List<MapRoomNode> row : AbstractDungeon.map) {
                if (row == null) {
                    continue;
                }
                for (MapRoomNode n : row) {
                    if (!"".equals(eligibilityOf(n)) || !matchesRoomKind(n, roomKind)) {
                        continue;
                    }
                    if (n.hb.cX < bestX) {
                        bestX = n.hb.cX;
                        best = new MapNodeRef(n.y, n.x, "");
                    }
                }
            }
            if (best == null) {
                lastStatus = "no_presentable_node";
                return IntentResult.rejected("no native-selectable map node");
            }
            return click(best);
        } catch (Throwable t) {
            return IntentResult.rejected(
                    "map first failed: "
                            + (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName()));
        }
    }

    private static boolean matchesRoomKind(MapRoomNode node, String roomKind) {
        if (roomKind == null || roomKind.trim().isEmpty()) {
            return true;
        }
        String want = roomKind.trim().toLowerCase();
        try {
            String symbol = node.getRoomSymbol(Boolean.FALSE);
            if (want.equals("event") || want.equals("unknown")) {
                // "*" is STS's hidden symbol for every unrevealed room kind, not just events.
                return node.room instanceof com.megacrit.cardcrawl.rooms.EventRoom;
            }
            if (want.equals("monster") || want.equals("combat")) {
                return "M".equals(symbol);
            }
            if (want.equals("elite")) {
                return "E".equals(symbol);
            }
            if (want.equals("rest")) {
                return "R".equals(symbol);
            }
            if (want.equals("shop")) {
                return "$".equals(symbol);
            }
            if (want.equals("treasure")) {
                return "T".equals(symbol);
            }
            return node.room != null && node.room.getClass().getSimpleName().toLowerCase().contains(want);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static IntentResult click(final MapNodeRef ref) {
        if (ref == null) {
            lastStatus = "missing_ref";
            return IntentResult.rejected("MapNodeRef required");
        }
        if (findNode(ref) == null) {
            lastStatus = "node_unavailable";
            return IntentResult.rejected("map node unavailable: " + ref.row + "," + ref.col);
        }
        MapRoomNode target = findNode(ref);
        String reason = eligibilityOf(target);
        if (!"".equals(reason)) {
            lastEligibility = reason;
            lastStatus = "native_target_ineligible";
            return IntentResult.rejected("map node is not selectable: " + reason);
        }
        lastEligibility = "selectable";
        pending = ref;
        lastTarget = ref;
        lastStatus = "queued";
        attempts = 0;
        // Several frames: first-pick path waits on animWaitTimer before nextRoom.
        pendingFrames = 60;
        // Also queue once on GL thread in case Postfix ordering differs.
        Runnable nudge =
                new Runnable() {
                    @Override
                    public void run() {
                        applyPendingGesture();
                    }
                };
        if (Gdx.app != null) {
            Gdx.app.postRunnable(nudge);
        } else {
            nudge.run();
        }
        return IntentResult.queued(IntentNames.CLICK_MAP_NODE + " " + ref.row + "," + ref.col);
    }

    /** Called after InputHelper.updateFirst so injected coords survive the frame. */
    public static void onAfterInputUpdate() {
        applyPendingGesture();
    }

    public static Map<String, Object> probeSlice() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        MapNodeRef ref = lastTarget;
        out.put("status", lastStatus);
        out.put("pending", Boolean.valueOf(pending != null));
        out.put("attempts", Integer.valueOf(attempts));
        out.put("pendingFrames", Integer.valueOf(pendingFrames));
        if (!lastEligibility.isEmpty()) {
            out.put("eligibility", lastEligibility);
        }
        if (ref != null) {
            out.put("row", Integer.valueOf(ref.row));
            out.put("col", Integer.valueOf(ref.col));
        }
        try {
            out.put("firstRoomChosen", Boolean.valueOf(AbstractDungeon.firstRoomChosen));
            out.put("screen", AbstractDungeon.screen != null ? AbstractDungeon.screen.name() : "");
            if (AbstractDungeon.currMapNode != null && AbstractDungeon.currMapNode.room != null) {
                out.put("currRoomPhase", String.valueOf(AbstractDungeon.currMapNode.room.phase));
            }
            if (AbstractDungeon.currMapNode != null) {
                out.put("currRow", Integer.valueOf(AbstractDungeon.currMapNode.y));
                out.put("currCol", Integer.valueOf(AbstractDungeon.currMapNode.x));
            }
            MapRoomNode target = ref != null ? findNodeIgnoringTransition(ref) : null;
            if (target != null && target.hb != null) {
                out.put("targetHovered", Boolean.valueOf(target.hb.hovered));
                out.put("targetClicked", Boolean.valueOf(target.hb.clicked));
                out.put("targetTaken", Boolean.valueOf(target.taken));
                out.put("targetHasEdges", Boolean.valueOf(target.hasEdges()));
                out.put("targetEligible", Boolean.valueOf("".equals(eligibilityOf(target))));
            }
            if (AbstractDungeon.dungeonMapScreen != null) {
                out.put("mapClicked", Boolean.valueOf(AbstractDungeon.dungeonMapScreen.clicked));
                out.put("mapDismissable", Boolean.valueOf(AbstractDungeon.dungeonMapScreen.dismissable));
            }
            out.put("nextRoom", Boolean.valueOf(AbstractDungeon.nextRoom != null));
            if (AbstractDungeon.nextRoom != null) {
                out.put("nextRow", Integer.valueOf(AbstractDungeon.nextRoom.y));
                out.put("nextCol", Integer.valueOf(AbstractDungeon.nextRoom.x));
            }
        } catch (Throwable ignored) {
            out.put("nextRoom", Boolean.FALSE);
        }
        return out;
    }

    public static void resetForTests() {
        pending = null;
        pendingFrames = 0;
        lastTarget = null;
        lastStatus = "idle";
        lastEligibility = "";
        attempts = 0;
    }

    private static void applyPendingGesture() {
        MapNodeRef ref = pending;
        if (ref == null || pendingFrames <= 0) {
            pending = null;
            pendingFrames = 0;
            return;
        }
        try {
            MapRoomNode target = findNode(ref);
            if (target == null
                    || AbstractDungeon.dungeonMapScreen == null
                    || AbstractDungeon.nextRoom != null) {
                if (AbstractDungeon.nextRoom != null) {
                    lastStatus = "next_room_selected";
                } else {
                    lastStatus = "target_unavailable";
                }
                pendingFrames--;
                if (pendingFrames <= 0) {
                    pending = null;
                }
                return;
            }
            String reason = eligibilityOf(target);
            if (!"".equals(reason)) {
                lastEligibility = reason;
                lastStatus = "native_target_ineligible";
                pending = null;
                pendingFrames = 0;
                return;
            }
            if (target.hb != null) {
                InputHelper.mX = (int) target.hb.cX;
                InputHelper.mY = (int) target.hb.cY;
                InputHelper.justClickedLeft = true;
                target.hb.clicked = true;
            }
            AbstractDungeon.dungeonMapScreen.clicked = true;
            AbstractDungeon.dungeonMapScreen.clickTimer = 0f;
            attempts++;
            lastStatus = "gesture_injected";
            // Keep sticky for a couple of frames so MapRoomNode.update can observe it.
            pendingFrames--;
            if (pendingFrames <= 0 || AbstractDungeon.nextRoom != null) {
                pending = null;
                pendingFrames = 0;
            }
        } catch (Throwable ignored) {
            lastStatus = "gesture_error";
            pending = null;
            pendingFrames = 0;
        }
    }

    /**
     * Resolve by STS node coordinates ({@code y}=floor/row, {@code x}=col). List indices are not
     * equal to {@code x} because empty slots are omitted from some map rows.
     */
    private static MapRoomNode findNode(MapNodeRef ref) {
        try {
            if (AbstractDungeon.dungeonMapScreen == null
                    || AbstractDungeon.map == null
                    || AbstractDungeon.nextRoom != null) {
                return null;
            }
            return findNodeIgnoringTransition(ref);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static MapRoomNode findNodeIgnoringTransition(MapNodeRef ref) {
        try {
            if (AbstractDungeon.map == null) {
                return null;
            }
            for (List<MapRoomNode> row : AbstractDungeon.map) {
                if (row == null) {
                    continue;
                }
                for (MapRoomNode n : row) {
                    if (n != null && n.y == ref.row && n.x == ref.col) {
                        if (n.hb != null && (n.hb.cX < -500f || n.hb.cY < -500f)) {
                            return null;
                        }
                        return n;
                    }
                }
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Mirrors DungeonMapScreen's native map-node update eligibility. Returns {@code ""} when the
     * node is selectable, otherwise the rejecting reason. Pure: never mutates diagnostics.
     */
    private static String eligibilityOf(MapRoomNode node) {
        if (node == null || node.hb == null) {
            return "missing_node";
        }
        if (node.hb.cX < -500f || node.hb.cY < -500f) {
            return "offscreen_node";
        }
        if (node.taken) {
            return "node_taken";
        }
        // DungeonMapScreen only updates nodes it kept in visibleMapNodes.
        if (!node.hasEdges()) {
            return "not_visible";
        }
        if (!AbstractDungeon.firstRoomChosen) {
            if (node.y != 0) {
                return "not_first_floor";
            }
            if (AbstractDungeon.currMapNode == null || AbstractDungeon.currMapNode.room == null) {
                return "missing_current_room";
            }
            if (AbstractDungeon.currMapNode.room.phase
                    != com.megacrit.cardcrawl.rooms.AbstractRoom.RoomPhase.COMPLETE) {
                return "current_room_not_complete";
            }
            return "";
        }
        if (AbstractDungeon.currMapNode == null) {
            return "missing_current_node";
        }
        if (!AbstractDungeon.currMapNode.isConnectedTo(node)) {
            return "not_connected";
        }
        return "";
    }
}
