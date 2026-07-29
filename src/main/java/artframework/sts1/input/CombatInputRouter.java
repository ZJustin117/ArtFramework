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
import artframework.context.ContextSignals;
import artframework.sts1.FullPresentMode;
import artframework.sts1.FullPresentCapability;

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

    public static boolean isExecutorReady(String surfaceId) {
        return executor.isReady(surfaceId);
    }

    public static FullPresentCapability capability(String surfaceId) {
        String id = SurfaceIds.canonicalize(surfaceId);
        boolean mounted = ArtFramework.component(id) != null && ArtFramework.component(id).isMounted();
        String scene = ArtFramework.projection().scene();
        boolean sceneReady = sceneReadyFor(id, scene, mounted);
        return FullPresentCapability.resolve(
                FullPresentMode.levelOf(id),
                mounted,
                sceneReady,
                isExecutorReady(id),
                false,
                artframework.sts1.PresentSafety.isPanic());
    }

    /**
     * Scene match for FULL_READY. Room surfaces require matching backend scene (not mount-only).
     * Select/event stay mount-driven (host screens may not rewrite projection scene).
     */
    static boolean sceneReadyFor(String id, String scene, boolean mounted) {
        if (SurfaceIds.MAP.equals(id)) {
            return "map".equals(scene);
        }
        if (SurfaceIds.REWARD_COMBAT.equals(id)
                || SurfaceIds.REWARD_CARD.equals(id)
                || SurfaceIds.REWARD_BOSS_RELIC.equals(id)) {
            return "reward".equals(scene);
        }
        if (SurfaceIds.REST.equals(id)) {
            return "rest".equals(scene);
        }
        if (SurfaceIds.TREASURE.equals(id)) {
            return "treasure".equals(scene);
        }
        if (SurfaceIds.SHOP.equals(id)) {
            return "shop".equals(scene);
        }
        if (SurfaceIds.COMBAT_ENERGY.equals(id) || SurfaceIds.COMBAT_INTENTS.equals(id)) {
            return "combat".equals(scene);
        }
        if (SurfaceIds.COMBAT_PROCEED.equals(id)) {
            return "combat".equals(scene) || "reward".equals(scene);
        }
        if (SurfaceIds.TOP_PANEL.equals(id)) {
            return "combat".equals(scene)
                    || "map".equals(scene)
                    || "reward".equals(scene)
                    || "rest".equals(scene)
                    || "treasure".equals(scene)
                    || "shop".equals(scene)
                    || "event".equals(scene);
        }
        if (SurfaceIds.EVENT.equals(id)
                || SurfaceIds.SELECT_GRID.equals(id)
                || SurfaceIds.SELECT_HAND.equals(id)
                || SurfaceIds.SKELETON.equals(id)) {
            return mounted;
        }
        return "combat".equals(scene);
    }

    public static void setSuppressNativeInput(boolean suppress) {
        suppressNativeInput = suppress;
    }

    public static boolean shouldSuppressNativeInput() {
        return suppressNativeInput && capability(SurfaceIds.COMBAT_HAND).ownsInput();
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
        if (!capability(SurfaceIds.COMBAT_CONTROLS).ownsInput()) {
            return IntentResult.rejected("controls full-present input not enabled");
        }
        UiIntent intent = UiIntent.of(IntentNames.PRESS_END_TURN, SurfaceIds.COMBAT_CONTROLS);
        return toIntentResult(artframework.core.SignalBuses.get().emit(
                new artframework.core.UiSignal(ContextSignals.action(SurfaceIds.COMBAT_CONTROLS, intent.name),
                        SurfaceIds.COMBAT_CONTROLS, intent)));
    }

    public static IntentResult route(String intentName, Object... args) {
        if (!capability(SurfaceIds.COMBAT_HAND).ownsInput()) {
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
        return toIntentResult(artframework.core.SignalBuses.get().emit(
                new artframework.core.UiSignal(ContextSignals.action(SurfaceIds.COMBAT_HAND, intent.name),
                        SurfaceIds.COMBAT_HAND, intent)));
    }

    /**
     * Backend hook: when mayOwnInput, execute via installed executor; otherwise reject.
     */
    public static IntentResult executeIfOwned(UiIntent intent) {
        if (intent == null) {
            return IntentResult.rejected("intent required");
        }
        if (!capability(intent.surfaceId).ownsInput()) {
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
        FullPresentCapability hand = capability(SurfaceIds.COMBAT_HAND);
        FullPresentCapability controls = capability(SurfaceIds.COMBAT_CONTROLS);
        m.put("executorReady", Boolean.valueOf(isExecutorReady(SurfaceIds.COMBAT_HAND)));
        m.put("handOwnsInput", Boolean.valueOf(hand.ownsInput()));
        m.put("controlsOwnInput", Boolean.valueOf(controls.ownsInput()));
        m.put("handState", hand.state.name());
        m.put("handReason", hand.reason);
        m.put("controlsState", controls.state.name());
        m.put("controlsReason", controls.reason);
        FullPresentCapability event = capability(SurfaceIds.EVENT);
        FullPresentCapability select = capability(SurfaceIds.SELECT_GRID);
        m.put("eventState", event.state.name());
        m.put("eventReason", event.reason);
        m.put("selectState", select.state.name());
        m.put("selectReason", select.reason);
        m.put("eventOwnsInput", Boolean.valueOf(event.ownsInput()));
        m.put("selectOwnsInput", Boolean.valueOf(select.ownsInput()));
        FullPresentCapability reward = capability(SurfaceIds.REWARD_COMBAT);
        FullPresentCapability rest = capability(SurfaceIds.REST);
        FullPresentCapability shop = capability(SurfaceIds.SHOP);
        FullPresentCapability treasure = capability(SurfaceIds.TREASURE);
        FullPresentCapability proceed = capability(SurfaceIds.COMBAT_PROCEED);
        FullPresentCapability energy = capability(SurfaceIds.COMBAT_ENERGY);
        FullPresentCapability intents = capability(SurfaceIds.COMBAT_INTENTS);
        m.put("rewardState", reward.state.name());
        m.put("rewardReason", reward.reason);
        m.put("rewardOwnsInput", Boolean.valueOf(reward.ownsInput()));
        m.put("restState", rest.state.name());
        m.put("restReason", rest.reason);
        m.put("shopState", shop.state.name());
        m.put("shopReason", shop.reason);
        m.put("treasureState", treasure.state.name());
        m.put("treasureReason", treasure.reason);
        m.put("proceedState", proceed.state.name());
        m.put("proceedReason", proceed.reason);
        m.put("energyState", energy.state.name());
        m.put("energyReason", energy.reason);
        m.put("intentsState", intents.state.name());
        m.put("intentsReason", intents.reason);
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

    private static IntentResult toIntentResult(artframework.core.SignalDispatchResult result) {
        if (result == null) {
            return IntentResult.rejected("no result");
        }
        if (result.isRejected()) {
            return IntentResult.rejected(result.message);
        }
        return IntentResult.accepted(result.message);
    }

    private static final class RejectingExecutor implements IntentExecutor {
        @Override
        public IntentResult execute(UiIntent intent) {
            return IntentResult.rejected(
                    "sts1 intent executor not installed: " + (intent != null ? intent.name : ""));
        }

        @Override
        public boolean isReady(String surfaceId) {
            return false;
        }
    }
}
