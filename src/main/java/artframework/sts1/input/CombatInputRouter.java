package artframework.sts1.input;

import artframework.api.ArtFramework;
import artframework.context.CardEntity;
import artframework.context.CardRef;
import artframework.context.CardZone;
import artframework.context.IntentNames;
import artframework.context.IntentResult;
import artframework.context.PresentProjection;
import artframework.context.SurfaceIds;
import artframework.context.UiIntent;
import artframework.sts1.FullPresentMode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Routes combat hand input to signals + intents when full-present owns input (16.5). Does not
 * execute game rules; delegates to {@link IntentExecutor}.
 */
public final class CombatInputRouter {

    private static IntentExecutor executor = new RejectingExecutor();
    private static boolean suppressNativeInput;

    private CombatInputRouter() {}

    public static void setExecutor(IntentExecutor exec) {
        executor = exec != null ? exec : new RejectingExecutor();
    }

    public static IntentExecutor executor() {
        return executor;
    }

    public static void setSuppressNativeInput(boolean suppress) {
        suppressNativeInput = suppress;
    }

    public static boolean shouldSuppressNativeInput() {
        return suppressNativeInput
                && FullPresentMode.mayOwnInput(SurfaceIds.COMBAT_HAND)
                && ArtFramework.component(SurfaceIds.COMBAT_HAND) != null
                && ArtFramework.component(SurfaceIds.COMBAT_HAND).isMounted()
                && "combat".equals(ArtFramework.projection().scene());
    }

    public static IntentResult beginDrag(String instanceId) {
        return route(IntentNames.BEGIN_DRAG, instanceId);
    }

    public static IntentResult moveDrag(String instanceId, float x, float y) {
        return route(IntentNames.MOVE_DRAG, instanceId, Float.valueOf(x), Float.valueOf(y));
    }

    public static IntentResult dropCard(String instanceId, String targetId) {
        return route(IntentNames.DROP_CARD, instanceId, targetId);
    }

    public static IntentResult cancelDrag(String instanceId) {
        return route(IntentNames.CANCEL_DRAG, instanceId);
    }

    public static IntentResult playCard(CardRef ref, String targetId) {
        if (ref == null) {
            return IntentResult.rejected("CardRef required");
        }
        return route(IntentNames.PLAY_CARD, ref, targetId);
    }

    public static IntentResult pressEndTurn() {
        if (!FullPresentMode.mayOwnInput(SurfaceIds.COMBAT_CONTROLS)) {
            return IntentResult.rejected("controls full-present input not enabled");
        }
        UiIntent intent = UiIntent.of(IntentNames.PRESS_END_TURN, SurfaceIds.COMBAT_CONTROLS);
        return ArtFramework.submitIntent(intent);
    }

    public static IntentResult route(String intentName, Object... args) {
        if (!FullPresentMode.mayOwnInput(SurfaceIds.COMBAT_HAND)) {
            return IntentResult.rejected("hand full-present input not enabled");
        }
        if (ArtFramework.component(SurfaceIds.COMBAT_HAND) == null
                || !ArtFramework.component(SurfaceIds.COMBAT_HAND).isMounted()) {
            return IntentResult.rejected("hand surface not mounted");
        }
        PresentProjection proj = ArtFramework.projection();
        if (IntentNames.BEGIN_DRAG.equals(intentName)
                || IntentNames.PLAY_CARD.equals(intentName)
                || IntentNames.DROP_CARD.equals(intentName)
                || IntentNames.CANCEL_DRAG.equals(intentName)
                || IntentNames.MOVE_DRAG.equals(intentName)) {
            String instanceId = instanceFromArgs(args);
            if (instanceId != null && !instanceId.isEmpty()) {
                CardEntity e = proj.get(instanceId);
                if (e == null) {
                    // allow type-id play via CardRef in args[0]
                    if (!(args != null && args.length > 0 && args[0] instanceof CardRef)) {
                        return IntentResult.rejected("unknown card instance: " + instanceId);
                    }
                } else if (e.zone != CardZone.HAND
                        && !IntentNames.CANCEL_DRAG.equals(intentName)) {
                    return IntentResult.rejected("card not in hand: " + instanceId);
                }
            }
        }
        UiIntent intent = UiIntent.of(intentName, SurfaceIds.COMBAT_HAND, args);
        return ArtFramework.submitIntent(intent);
    }

    /**
     * Backend hook: when mayOwnInput, execute via installed executor; otherwise reject.
     */
    public static IntentResult executeIfOwned(UiIntent intent) {
        if (intent == null) {
            return IntentResult.rejected("intent required");
        }
        if (!FullPresentMode.mayOwnInput(intent.surfaceId)) {
            return IntentResult.rejected("sts1 full-present input is not enabled for " + intent.surfaceId);
        }
        IntentResult r = executor.execute(intent);
        if (r == null) {
            return IntentResult.rejected("executor declined: " + intent.name);
        }
        return r;
    }

    public static Map<String, Object> probeSlice() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("suppressNativeInput", Boolean.valueOf(shouldSuppressNativeInput()));
        m.put("handOwnsInput", Boolean.valueOf(FullPresentMode.mayOwnInput(SurfaceIds.COMBAT_HAND)));
        m.put("controlsOwnInput", Boolean.valueOf(FullPresentMode.mayOwnInput(SurfaceIds.COMBAT_CONTROLS)));
        m.put(
                "dragInstanceId",
                ArtFramework.projection().dragInstanceId() != null
                        ? ArtFramework.projection().dragInstanceId()
                        : "");
        return m;
    }

    public static void resetForTests() {
        executor = new RejectingExecutor();
        suppressNativeInput = false;
    }

    private static String instanceFromArgs(Object[] args) {
        if (args == null || args.length == 0 || args[0] == null) {
            return "";
        }
        if (args[0] instanceof CardRef) {
            return ((CardRef) args[0]).instanceId;
        }
        return String.valueOf(args[0]);
    }

    private static final class RejectingExecutor implements IntentExecutor {
        @Override
        public IntentResult execute(UiIntent intent) {
            return IntentResult.rejected(
                    "sts1 intent executor not installed: " + (intent != null ? intent.name : ""));
        }
    }
}
