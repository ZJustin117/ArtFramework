package artframework.sts1.input;

import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import artframework.c2.MapNodeRef;
import artframework.context.IntentNames;
import artframework.context.IntentResult;

import java.util.List;

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

    private Sts1MapIntentBridge() {}

    /** Lab helper: first presentable floor-0 node (valid hitbox, not offscreen sentinel). */
    public static IntentResult clickFirstPresentable() {
        try {
            if (AbstractDungeon.map == null) {
                return IntentResult.rejected("no map");
            }
            MapNodeRef best = null;
            float bestX = Float.MAX_VALUE;
            for (List<MapRoomNode> row : AbstractDungeon.map) {
                if (row == null) {
                    continue;
                }
                for (MapRoomNode n : row) {
                    if (n == null || n.y != 0 || n.hb == null) {
                        continue;
                    }
                    if (n.hb.cX < -500f || n.hb.cY < -500f) {
                        continue;
                    }
                    if (n.hb.cX < bestX) {
                        bestX = n.hb.cX;
                        best = new MapNodeRef(n.y, n.x, "");
                    }
                }
            }
            if (best == null) {
                return IntentResult.rejected("no presentable floor-0 map node");
            }
            return click(best);
        } catch (Throwable t) {
            return IntentResult.rejected(
                    "map first failed: "
                            + (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName()));
        }
    }

    public static IntentResult click(final MapNodeRef ref) {
        if (ref == null) {
            return IntentResult.rejected("MapNodeRef required");
        }
        if (findNode(ref) == null) {
            return IntentResult.rejected("map node unavailable: " + ref.row + "," + ref.col);
        }
        pending = ref;
        // Several frames: first-pick path waits on animWaitTimer before nextRoom.
        pendingFrames = 12;
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
                pendingFrames--;
                if (pendingFrames <= 0) {
                    pending = null;
                }
                return;
            }
            if (target.hb != null) {
                InputHelper.mX = (int) target.hb.cX;
                InputHelper.mY = (int) target.hb.cY;
                InputHelper.justClickedLeft = true;
            }
            AbstractDungeon.dungeonMapScreen.clicked = true;
            AbstractDungeon.dungeonMapScreen.clickTimer = 0f;
            // Keep sticky for a couple of frames so MapRoomNode.update can observe it.
            pendingFrames--;
            if (pendingFrames <= 0 || AbstractDungeon.nextRoom != null) {
                pending = null;
                pendingFrames = 0;
            }
        } catch (Throwable ignored) {
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
}
