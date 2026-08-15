package artframework.context;

import artframework.api.UiOpResult;
import artframework.assets.AssetResolveResult;
import artframework.assets.HostAssetsHolder;
import artframework.core.ComponentKind;
import artframework.core.SignalHandler;
import artframework.core.SignalDispatchResult;
import artframework.core.SignalHub;
import artframework.core.SignalNames;
import artframework.core.SignalSubscription;
import artframework.core.UiComponent;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import artframework.presentation.HostBindingComponent;
import artframework.presentation.NodePropertiesComponent;
import artframework.presentation.PresentPolicyComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;
import artframework.presentation.SignalPortsComponent;
import artframework.ecs.EcsPipeline;
import artframework.ecs.EcsTick;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Full-present C2 surfaces driven by the shared signal projection (milestone 15).
 */
public final class PresentSurfaces {

    private static final Map<String, UiComponent> BY_ID = new LinkedHashMap<String, UiComponent>();
    private static final SignalHub HUB = new SignalHub();
    private static final SurfaceLifecycleSystem LIFECYCLE = new SurfaceLifecycleSystem();

    static {
        register(new HandSurface());
        register(new CardSlotsSurface());
        register(new ControlsSurface());
        register(new MapSurface());
        register(new SkeletonSurface());
        register(new CombatRootSurface());
        register(new EventSurface());
        register(new SelectSurface(SurfaceIds.SELECT_GRID));
        register(new SelectSurface(SurfaceIds.SELECT_HAND));
        register(new ProceedSurface());
        register(new EnergySurface());
        register(new RewardSurface(SurfaceIds.REWARD_COMBAT));
        register(new RewardSurface(SurfaceIds.REWARD_CARD));
        register(new RewardSurface(SurfaceIds.REWARD_BOSS_RELIC));
        register(new RestSurface());
        register(new TreasureSurface());
        register(new ShopSurface());
        register(new TopPanelSurface());
        register(new IntentsSurface());
    }

    private PresentSurfaces() {}

    private static void register(UiComponent c) {
        BY_ID.put(c.id(), c);
    }

    public static UiComponent get(String id) {
        if (id == null) {
            return null;
        }
        String canon = SurfaceIds.canonicalize(id);
        UiComponent c = BY_ID.get(canon);
        if (c != null) {
            return c;
        }
        return BY_ID.get(id);
    }

    public static List<String> ids() {
        return Collections.unmodifiableList(new ArrayList<String>(BY_ID.keySet()));
    }

    /** Whether any native/full-present surface currently participates in the frame. */
    public static boolean anyMounted() {
        for (UiComponent c : BY_ID.values()) {
            if (c.isMounted()) {
                return true;
            }
        }
        return false;
    }

    public static List<Map<String, Object>> probeAll() {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (UiComponent c : BY_ID.values()) {
            out.add(c.probeSlice());
        }
        return out;
    }

    public static void resetForTests() {
        HUB.clear();
        PresentationContext context = context();
        PresentationWorld world = context.world();
        for (EntityId entity : new ArrayList<EntityId>(context.entities())) {
            if (world.get(entity, SurfaceIdentityComponent.class) != null) {
                context.destroy(entity);
            }
        }
    }

    public static void emit(String id, String signal, Object... args) {
        UiComponent c = get(id);
        if (c != null) {
            c.emit(signal, args);
        }
    }

    /** Internal ECS world for durable surface state; surface APIs remain the compatibility facade. */
    public static PresentationWorld world() {
        return context().world();
    }

    private static PresentationContext context() {
        return PresentationRegistry.context("c2-surfaces");
    }

    abstract static class BaseSurface implements UiComponent {
        private final String id;
        private final Set<String> signals;

        BaseSurface(String id, String... signalNames) {
            this.id = id;
            this.signals = new HashSet<String>(Arrays.asList(signalNames));
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public ComponentKind kind() {
            return ComponentKind.NATIVE_HOST;
        }

        @Override
        public boolean isMounted() {
            EntityId entity = surfaceEntity();
            if (entity == null) return false;
            SurfaceLifecycleComponent lifecycle = world().get(entity, SurfaceLifecycleComponent.class);
            return lifecycle != null && lifecycle.mounted;
        }

        @Override
        public void mount() {
            syncComponents(true);
        }

        @Override
        public void unmount() {
            EntityId entity = surfaceEntity();
            if (entity != null) {
                world().put(entity, SurfaceLifecycleRequestComponent.class,
                        new SurfaceLifecycleRequestComponent(false));
                EcsPipeline.run(world(), new EcsTick(0f, 0L),
                        Collections.<artframework.ecs.EcsSystem>singletonList(LIFECYCLE));
                context().destroy(entity);
            }
            HUB.clearInstance(id);
            if (SurfaceIds.COMBAT_HAND.equals(id)) {
                PresentProjections.get().clearDrag();
            }
        }

        @Override
        public SignalSubscription connect(String signal, SignalHandler handler) {
            requireSignal(signal);
            return HUB.connect(id, signal, handler);
        }

        @Override
        public void disconnect(String signal, SignalHandler handler) {
            requireSignal(signal);
            HUB.disconnect(id, signal, handler);
        }

        @Override
        public SignalDispatchResult emit(String signal, Object... args) {
            requireSignal(signal);
            return HUB.dispatch(id, signal, args);
        }

        private void requireSignal(String signal) {
            if (signal == null || !signals.contains(signal)) {
                throw new IllegalArgumentException(
                        "undeclared signal \"" + signal + "\" on surface \"" + id + "\"");
            }
        }

        Map<String, Object> baseProbe(List<String> actions) {
            syncComponents();
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("kind", kind().name());
            m.put("mounted", Boolean.valueOf(isMounted()));
            artframework.sts1.PresentLevel level = artframework.sts1.FullPresentMode.levelOf(id);
            m.put("presentLevel", level.name());
            m.put("fullPresent", Boolean.valueOf(level.allowsFullPresent()));
            m.put("observe", Boolean.valueOf(level.allowsObserve()));
            m.put("maySuppressNative", Boolean.valueOf(artframework.sts1.FullPresentMode.maySuppressNative(id)));
            m.put("actions", actions);
            m.put("signals", new ArrayList<String>(signals));
            m.putAll(PresentProjections.get().probeSlice());
            return m;
        }

        private void syncComponents() {
            syncComponents(isMounted());
        }

        private void syncComponents(boolean mounted) {
            EntityId entity = surfaceEntity();
            if (entity == null) {
                PresentationKey key = new PresentationKey("sts1.surface", id);
                entity = context().create(key, id, "surface", "c2");
                world().put(entity, SurfaceIdentityComponent.class,
                        new SurfaceIdentityComponent(id, kind()));
            }
            artframework.sts1.PresentLevel level = artframework.sts1.FullPresentMode.levelOf(id);
            world().put(entity, SurfaceLifecycleRequestComponent.class,
                    new SurfaceLifecycleRequestComponent(mounted));
            EcsPipeline.run(world(), new EcsTick(0f, 0L),
                    Collections.<artframework.ecs.EcsSystem>singletonList(LIFECYCLE));
            world().put(entity, SurfacePolicyComponent.class,
                    new SurfacePolicyComponent(level, level.allowsFullPresent(),
                            level.allowsObserve(), artframework.sts1.FullPresentMode.maySuppressNative(id)));
            world().put(entity, HostBindingComponent.class, new HostBindingComponent("STS1_C2", id));
            world().put(entity, SignalPortsComponent.class,
                    new SignalPortsComponent(new ArrayList<String>(signals)));
            world().put(entity, NodePropertiesComponent.class,
                    new NodePropertiesComponent(Collections.<String, Object>singletonMap("surfaceId", id)));
            world().put(entity, PresentPolicyComponent.class,
                    new PresentPolicyComponent(level.name(), level.allowsFullPresent(),
                            level.allowsFullPresent()));
        }

        private EntityId surfaceEntity() {
            return context().entity(new PresentationKey("sts1.surface", id));
        }

        protected IntentResult submit(String intentName, Object... args) {
            EntityId entity = surfaceEntity();
            if (entity != null) {
                world().put(entity, SurfaceActionComponent.class,
                        new SurfaceActionComponent(intentName));
                world().put(entity, SurfaceIntentComponent.class,
                        new SurfaceIntentComponent(intentName, id));
                world().put(entity, SurfaceIntentExecutionComponent.class,
                        new SurfaceIntentExecutionComponent(intentName, id, args));
                world().put(entity, BusinessConfirmationComponent.class,
                        new BusinessConfirmationComponent(intentName,
                                BusinessConfirmationComponent.domain(id, intentName),
                                BusinessConfirmationComponent.State.PENDING,
                                -1L, -1L, -1L, -1L, "awaiting authority snapshot"));
            }
            artframework.api.ArtFramework.executeSurfaceIntents();
            SurfaceResultComponent result = entity != null
                    ? world().get(entity, SurfaceResultComponent.class) : null;
            if (result == null) return IntentResult.rejected("no result");
            return IntentResult.of(result.status, result.message);
        }

        private IntentResult recordResult(EntityId entity, IntentResult result) {
            if (entity != null) {
                world().put(entity, SurfaceResultComponent.class,
                        new SurfaceResultComponent(result.status, result.message));
            }
            return result;
        }

        protected static UiOpResult toOp(IntentResult r) {
            if (r == null) {
                return UiOpResult.unavailable("no result");
            }
            if (r.status == IntentResult.Status.ACCEPTED) {
                return UiOpResult.ok(r.message);
            }
            if (r.status == IntentResult.Status.QUEUED) {
                return UiOpResult.ok("queued:" + r.message);
            }
            if (r.message != null && r.message.startsWith("blocked:")) {
                return UiOpResult.blocked(r.message);
            }
            return UiOpResult.unavailable(r.message);
        }
    }

    static final class HandSurface extends BaseSurface {
        HandSurface() {
            super(
                    SurfaceIds.COMBAT_HAND,
                    SignalNames.PRESSED,
                    SignalNames.CARD_PRESSED,
                    SignalNames.DRAG_STARTED,
                    SignalNames.DRAG_MOVED,
                    SignalNames.DRAG_ENDED,
                    SignalNames.PLAY_REQUESTED);
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if (!isMounted()) {
                return UiOpResult.notBound("hand surface not mounted");
            }
            PresentProjection proj = PresentProjections.get();
            if ("begin_drag".equals(name) || IntentNames.BEGIN_DRAG.equals(name)) {
                String instanceId = argString(args, 0);
                CardEntity e = proj.get(instanceId);
                if (e == null) {
                    return UiOpResult.unavailable("unknown card instance: " + instanceId);
                }
                IntentResult r = submit(IntentNames.BEGIN_DRAG, instanceId);
                if (r.isAccepted()) {
                    proj.setDragInstanceId(instanceId);
                    emit(SignalNames.DRAG_STARTED, instanceId);
                }
                return toOp(r);
            }
            if ("move_drag".equals(name) || IntentNames.MOVE_DRAG.equals(name)) {
                String instanceId = argString(args, 0);
                float x = argFloat(args, 1);
                float y = argFloat(args, 2);
                IntentResult r = submit(IntentNames.MOVE_DRAG, instanceId, Float.valueOf(x), Float.valueOf(y));
                if (r.isAccepted()) {
                    emit(SignalNames.DRAG_MOVED, instanceId, Float.valueOf(x), Float.valueOf(y));
                }
                return toOp(r);
            }
            if ("drop_card".equals(name) || IntentNames.DROP_CARD.equals(name)) {
                String instanceId = argString(args, 0);
                String target = argString(args, 1);
                IntentResult r = submit(IntentNames.DROP_CARD, instanceId, target);
                if (r.isAccepted()) {
                    proj.clearDrag();
                    emit(SignalNames.DRAG_ENDED, instanceId, target);
                }
                return toOp(r);
            }
            if ("cancel_drag".equals(name) || IntentNames.CANCEL_DRAG.equals(name)) {
                String instanceId = argString(args, 0);
                IntentResult r = submit(IntentNames.CANCEL_DRAG, instanceId);
                proj.clearDrag();
                if (r.isAccepted()) {
                    emit(SignalNames.DRAG_ENDED, instanceId, "cancel");
                }
                return toOp(r);
            }
            if ("play_card".equals(name) || IntentNames.PLAY_CARD.equals(name)) {
                Object refOrId = args != null && args.length > 0 ? args[0] : null;
                String target = argString(args, 1);
                CardRef ref;
                if (refOrId instanceof CardRef) {
                    ref = (CardRef) refOrId;
                } else {
                    String instanceId = refOrId != null ? String.valueOf(refOrId) : "";
                    if (instanceId.isEmpty()) {
                        return UiOpResult.unavailable("CardRef or instanceId required");
                    }
                    CardEntity e = proj.get(instanceId);
                    String cardId = e != null ? e.cardId : "";
                    if (e == null) {
                        // treat as type id lookup in hand
                        for (CardEntity h : proj.listZone(CardZone.HAND)) {
                            if (instanceId.equals(h.cardId)) {
                                e = h;
                                break;
                            }
                        }
                        if (e != null) {
                            ref = new CardRef(e.instanceId, e.cardId);
                        } else {
                            ref = new CardRef(instanceId, cardId);
                        }
                    } else {
                        ref = new CardRef(e.instanceId, cardId);
                    }
                }
                IntentResult r = submit(IntentNames.PLAY_CARD, ref, target);
                if (r.isAccepted()) {
                    emit(SignalNames.PLAY_REQUESTED, ref.instanceId, target);
                }
                return toOp(r);
            }
            if ("sync_frame".equals(name)) {
                return UiOpResult.unavailable("frames are published by the authority endpoint");
            }
            return UiOpResult.unavailable("unknown action: " + name);
        }

        @Override
        public Map<String, Object> probeSlice() {
            Map<String, Object> m =
                    baseProbe(
                            Arrays.asList(
                                    "begin_drag",
                                    "move_drag",
                                    "drop_card",
                                    "cancel_drag",
                                    "play_card",
                                    "sync_frame"));
            List<Map<String, Object>> hand = new ArrayList<Map<String, Object>>();
            for (CardEntity e : PresentProjections.get().listZone(CardZone.HAND)) {
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                row.put("instanceId", e.instanceId);
                row.put("cardId", e.cardId);
                row.put("slot", Integer.valueOf(e.slotIndex));
                row.put("x", Float.valueOf(e.pose.x));
                row.put("y", Float.valueOf(e.pose.y));
                row.put("art", e.artResourceId);
                AssetResolveResult art =
                        HostAssetsHolder.get().resolve(e.artResourceId);
                row.put("artPack", art.packId);
                row.put("artFound", Boolean.valueOf(art.found));
                hand.add(row);
            }
            m.put("hand", hand);
            return m;
        }
    }

    static final class CardSlotsSurface extends BaseSurface {
        CardSlotsSurface() {
            super(SurfaceIds.COMBAT_CARD_SLOTS, SignalNames.CARD_SELECTED, SignalNames.SLOT_CHANGED);
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if (!isMounted()) {
                return UiOpResult.notBound("card_slots not mounted");
            }
            if ("select_card".equals(name) || IntentNames.SELECT_CARD.equals(name)) {
                String instanceId = argString(args, 0);
                IntentResult r = submit(IntentNames.SELECT_CARD, instanceId);
                if (r.isAccepted()) {
                    emit(SignalNames.CARD_SELECTED, instanceId);
                }
                return toOp(r);
            }
            if ("inspect_slot".equals(name)) {
                String instanceId = argString(args, 0);
                CardEntity e = PresentProjections.get().get(instanceId);
                return e != null
                        ? UiOpResult.ok(e.zone + ":" + e.slotIndex)
                        : UiOpResult.unavailable("no slot for " + instanceId);
            }
            return UiOpResult.unavailable("unknown action: " + name);
        }

        @Override
        public Map<String, Object> probeSlice() {
            Map<String, Object> m = baseProbe(Arrays.asList("select_card", "inspect_slot"));
            m.put("zones", zoneCounts());
            return m;
        }

        private static Map<String, Object> zoneCounts() {
            Map<String, Object> z = new LinkedHashMap<String, Object>();
            PresentProjection p = PresentProjections.get();
            for (CardZone zone : CardZone.values()) {
                z.put(zone.name(), Integer.valueOf(p.listZone(zone).size()));
            }
            return z;
        }
    }

    static final class ControlsSurface extends BaseSurface {
        ControlsSurface() {
            super(SurfaceIds.COMBAT_CONTROLS, SignalNames.PRESSED, SignalNames.ENABLED_CHANGED);
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if (!isMounted()) {
                return UiOpResult.notBound("controls not mounted");
            }
            if ("press".equals(name)
                    || IntentNames.PRESS.equals(name)
                    || "press_end_turn".equals(name)
                    || IntentNames.PRESS_END_TURN.equals(name)
                    || "pressEndTurn".equals(name)) {
                IntentResult r = submit(IntentNames.PRESS_END_TURN);
                if (r.isAccepted()) {
                    emit(SignalNames.PRESSED);
                }
                return toOp(r);
            }
            return UiOpResult.unavailable("unknown action: " + name);
        }

        @Override
        public Map<String, Object> probeSlice() {
            Map<String, Object> m = baseProbe(Arrays.asList("press", "press_end_turn"));
            ControlsView cv = PresentProjections.get().controls();
            m.put("endTurnEnabled", Boolean.valueOf(cv.endTurnEnabled));
            m.put("endTurnVisible", Boolean.valueOf(cv.endTurnVisible));
            m.put("energy", Integer.valueOf(cv.energy));
            m.put("proceedEnabled", Boolean.valueOf(cv.proceedEnabled));
            m.put("proceedVisible", Boolean.valueOf(cv.proceedVisible));
            m.put("controls", cv.toMap().get("controls"));
            return m;
        }
    }

    static final class ProceedSurface extends BaseSurface {
        ProceedSurface() {
            super(SurfaceIds.COMBAT_PROCEED, SignalNames.PRESSED, SignalNames.SURFACE_OPENED);
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if ("mount_proceed".equals(name)) {
                mount();
                emit(SignalNames.SURFACE_OPENED);
                return UiOpResult.ok("proceed mounted");
            }
            if (!isMounted()) {
                return UiOpResult.notBound("proceed not mounted");
            }
            if ("press_proceed".equals(name) || IntentNames.PRESS_PROCEED.equals(name)) {
                IntentResult r = submit(IntentNames.PRESS_PROCEED);
                if (r != null && r.status != IntentResult.Status.REJECTED) {
                    emit(SignalNames.PRESSED, ControlsView.PROCEED_ID);
                }
                return toOp(r);
            }
            if ("press_cancel".equals(name) || IntentNames.PRESS_CANCEL.equals(name)) {
                IntentResult r = submit(IntentNames.PRESS_CANCEL);
                if (r != null && r.status != IntentResult.Status.REJECTED) {
                    emit(SignalNames.PRESSED, ControlsView.CANCEL_ID);
                }
                return toOp(r);
            }
            return UiOpResult.unavailable("unknown action: " + name);
        }

        @Override
        public Map<String, Object> probeSlice() {
            Map<String, Object> m =
                    baseProbe(Arrays.asList("press_proceed", "press_cancel", "mount_proceed"));
            ControlsView cv = PresentProjections.get().controls();
            m.put("proceedEnabled", Boolean.valueOf(cv.proceedEnabled));
            m.put("cancelEnabled", Boolean.valueOf(cv.cancelEnabled));
            return m;
        }
    }

    static final class EnergySurface extends BaseSurface {
        EnergySurface() {
            super(SurfaceIds.COMBAT_ENERGY, SignalNames.SURFACE_OPENED);
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if ("mount_energy".equals(name)) {
                mount();
                emit(SignalNames.SURFACE_OPENED);
                return UiOpResult.ok("energy mounted");
            }
            if (!isMounted()) {
                return UiOpResult.notBound("energy not mounted");
            }
            return UiOpResult.unavailable("unknown action: " + name);
        }

        @Override
        public Map<String, Object> probeSlice() {
            Map<String, Object> m = baseProbe(Arrays.asList("mount_energy"));
            m.put("energy", Integer.valueOf(PresentProjections.get().controls().energy));
            return m;
        }
    }

    static final class RewardSurface extends BaseSurface {
        RewardSurface(String id) {
            super(id, SignalNames.PRESSED, SignalNames.SURFACE_OPENED, SignalNames.SURFACE_CLOSED);
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if ("mount_reward".equals(name)) {
                mount();
                emit(SignalNames.SURFACE_OPENED);
                return UiOpResult.ok(id() + " mounted");
            }
            if (!isMounted()) {
                return UiOpResult.notBound(id() + " not mounted");
            }
            if ("claim".equals(name) || IntentNames.CLAIM_REWARD.equals(name)) {
                IntentResult r = submit(IntentNames.CLAIM_REWARD, args);
                if (r != null && r.status != IntentResult.Status.REJECTED) {
                    emit(SignalNames.PRESSED, args);
                }
                return toOp(r);
            }
            if ("skip".equals(name) || IntentNames.SKIP_REWARD.equals(name)) {
                IntentResult r = submit(IntentNames.SKIP_REWARD, args);
                return toOp(r);
            }
            return UiOpResult.unavailable("unknown action: " + name);
        }

        @Override
        public Map<String, Object> probeSlice() {
            Map<String, Object> m =
                    baseProbe(Arrays.asList("claim", "skip", "mount_reward", IntentNames.CLAIM_REWARD));
            m.put("reward", PresentProjections.get().reward().toMap());
            return m;
        }
    }

    static final class RestSurface extends BaseSurface {
        RestSurface() {
            super(SurfaceIds.REST, SignalNames.PRESSED, SignalNames.SURFACE_OPENED);
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if ("mount_rest".equals(name)) {
                mount();
                emit(SignalNames.SURFACE_OPENED);
                return UiOpResult.ok("rest mounted");
            }
            if (!isMounted()) {
                return UiOpResult.notBound("rest not mounted");
            }
            if ("choose".equals(name) || IntentNames.CHOOSE_REST_OPTION.equals(name)) {
                IntentResult r = submit(IntentNames.CHOOSE_REST_OPTION, args);
                if (r != null && r.status != IntentResult.Status.REJECTED) {
                    emit(SignalNames.PRESSED, args);
                }
                return toOp(r);
            }
            return UiOpResult.unavailable("unknown action: " + name);
        }

        @Override
        public Map<String, Object> probeSlice() {
            Map<String, Object> m = baseProbe(Arrays.asList("choose", "mount_rest"));
            m.put("rest", PresentProjections.get().rest().toMap());
            return m;
        }
    }

    static final class TreasureSurface extends BaseSurface {
        TreasureSurface() {
            super(SurfaceIds.TREASURE, SignalNames.PRESSED, SignalNames.SURFACE_OPENED);
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if ("mount_treasure".equals(name)) {
                mount();
                emit(SignalNames.SURFACE_OPENED);
                return UiOpResult.ok("treasure mounted");
            }
            if (!isMounted()) {
                return UiOpResult.notBound("treasure not mounted");
            }
            if ("open".equals(name) || IntentNames.OPEN_CHEST.equals(name)) {
                IntentResult r = submit(IntentNames.OPEN_CHEST, args);
                if (r != null && r.status != IntentResult.Status.REJECTED) {
                    emit(SignalNames.PRESSED);
                }
                return toOp(r);
            }
            return UiOpResult.unavailable("unknown action: " + name);
        }

        @Override
        public Map<String, Object> probeSlice() {
            Map<String, Object> m = baseProbe(Arrays.asList("open", "mount_treasure"));
            m.put("treasure", PresentProjections.get().treasure().toMap());
            return m;
        }
    }

    static final class ShopSurface extends BaseSurface {
        ShopSurface() {
            super(SurfaceIds.SHOP, SignalNames.PRESSED, SignalNames.SURFACE_OPENED);
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if ("mount_shop".equals(name)) {
                mount();
                emit(SignalNames.SURFACE_OPENED);
                return UiOpResult.ok("shop mounted");
            }
            if (!isMounted()) {
                return UiOpResult.notBound("shop not mounted");
            }
            if ("buy".equals(name) || IntentNames.BUY_SHOP_ENTRY.equals(name)) {
                IntentResult r = submit(IntentNames.BUY_SHOP_ENTRY, args);
                if (r != null && r.status != IntentResult.Status.REJECTED) {
                    emit(SignalNames.PRESSED, args);
                }
                return toOp(r);
            }
            if ("purge".equals(name) || IntentNames.PURGE_CARD.equals(name)) {
                return toOp(submit(IntentNames.PURGE_CARD, args));
            }
            return UiOpResult.unavailable("unknown action: " + name);
        }

        @Override
        public Map<String, Object> probeSlice() {
            Map<String, Object> m = baseProbe(Arrays.asList("buy", "purge", "mount_shop"));
            m.put("shop", PresentProjections.get().shop().toMap());
            return m;
        }
    }

    static final class TopPanelSurface extends BaseSurface {
        TopPanelSurface() {
            super(SurfaceIds.TOP_PANEL, SignalNames.SURFACE_OPENED);
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if ("mount_top_panel".equals(name)) {
                mount();
                emit(SignalNames.SURFACE_OPENED);
                return UiOpResult.ok("top panel mounted");
            }
            if (!isMounted()) {
                return UiOpResult.notBound("top panel not mounted");
            }
            return UiOpResult.unavailable("unknown action: " + name);
        }

        @Override
        public Map<String, Object> probeSlice() {
            Map<String, Object> m = baseProbe(Arrays.asList("mount_top_panel"));
            m.put("topPanel", PresentProjections.get().topPanel().toMap());
            return m;
        }
    }

    static final class IntentsSurface extends BaseSurface {
        IntentsSurface() {
            super(SurfaceIds.COMBAT_INTENTS, SignalNames.SURFACE_OPENED);
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if ("mount_intents".equals(name)) {
                mount();
                emit(SignalNames.SURFACE_OPENED);
                return UiOpResult.ok("intents mounted");
            }
            if (!isMounted()) {
                return UiOpResult.notBound("intents not mounted");
            }
            return UiOpResult.unavailable("unknown action: " + name);
        }

        @Override
        public Map<String, Object> probeSlice() {
            Map<String, Object> m = baseProbe(Arrays.asList("mount_intents"));
            m.put("intents", PresentProjections.get().intents().toMap());
            return m;
        }
    }

    static final class MapSurface extends BaseSurface {
        MapSurface() {
            super(SurfaceIds.MAP, SignalNames.NODE_CLICKED, SignalNames.NODE_HOVERED);
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if (!isMounted()) {
                return UiOpResult.notBound("map surface not mounted");
            }
            if ("click_node".equals(name)
                    || "clickMapNode".equals(name)
                    || IntentNames.CLICK_MAP_NODE.equals(name)) {
                Object node = args != null && args.length > 0 ? args[0] : null;
                IntentResult r = submit(IntentNames.CLICK_MAP_NODE, node);
                if (r != null && r.status != IntentResult.Status.REJECTED) {
                    emit(SignalNames.NODE_CLICKED, node);
                }
                return toOp(r);
            }
            if ("set_pins".equals(name)) {
                return UiOpResult.ok("pins decorative only");
            }
            return UiOpResult.unavailable("unknown action: " + name);
        }

        @Override
        public Map<String, Object> probeSlice() {
            Map<String, Object> m = baseProbe(Arrays.asList("click_node", "set_pins"));
            MapView mv = PresentProjections.get().map();
            m.put("map", mv.toMap());
            m.put("nodeCount", Integer.valueOf(mv.nodeCount()));
            return m;
        }
    }

    static final class SkeletonSurface extends BaseSurface {
        SkeletonSurface() {
            super(SurfaceIds.SKELETON, SignalNames.FINISHED, SignalNames.SKELETON_EVENT);
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if (!isMounted()) {
                return UiOpResult.notBound("skeleton surface not mounted");
            }
            if ("play".equals(name)
                    || "stop".equals(name)
                    || "set_transform".equals(name)
                    || "set_animation".equals(name)
                    || "add_animation".equals(name)
                    || "set_mix".equals(name)) {
                IntentResult r = submit(name, args);
                if (r.isAccepted() && "play".equals(name)) {
                    // finished is host-driven later; acknowledge play only
                }
                return toOp(r);
            }
            return UiOpResult.unavailable("unknown action: " + name);
        }

        @Override
        public Map<String, Object> probeSlice() {
            return baseProbe(Arrays.asList("play", "stop", "set_transform", "set_animation", "add_animation", "set_mix"));
        }
    }

    /** Event full-present surface (19.5 contract; 22 host draw/executor). */
    static final class EventSurface extends BaseSurface {
        EventSurface() {
            super(SurfaceIds.EVENT, SignalNames.OPTION_CHOSEN, SignalNames.SURFACE_OPENED, SignalNames.SURFACE_CLOSED);
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if ("mount_event".equals(name)) {
                mount();
                emit(SignalNames.SURFACE_OPENED);
                return UiOpResult.ok("event surface mounted");
            }
            if (!isMounted()) {
                return UiOpResult.notBound("event surface not mounted");
            }
            if ("choose_option".equals(name)
                    || IntentNames.CHOOSE_EVENT_OPTION.equals(name)
                    || "choose_event_option".equals(name)) {
                Object index = args != null && args.length > 0 ? args[0] : Integer.valueOf(0);
                Object label = args != null && args.length > 1 ? args[1] : "";
                IntentResult r = submit(IntentNames.CHOOSE_EVENT_OPTION, index, label);
                if (r != null && r.status != IntentResult.Status.REJECTED) {
                    emit(SignalNames.OPTION_CHOSEN, index, label);
                }
                return toOp(r);
            }
            return UiOpResult.unavailable("unknown action: " + name);
        }

        @Override
        public Map<String, Object> probeSlice() {
            return baseProbe(Arrays.asList("choose_option", "mount_event", IntentNames.CHOOSE_EVENT_OPTION));
        }
    }

    /** Grid/hand select full-present surface (19.5). */
    static final class SelectSurface extends BaseSurface {
        SelectSurface(String id) {
            super(id, SignalNames.CARD_SELECTED, SignalNames.CONFIRMED, SignalNames.SURFACE_OPENED);
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if ("mount_select".equals(name)) {
                mount();
                emit(SignalNames.SURFACE_OPENED);
                return UiOpResult.ok(id() + " mounted");
            }
            if (!isMounted()) {
                return UiOpResult.notBound(id() + " not mounted");
            }
            if ("select_card".equals(name) || IntentNames.SELECT_CARD.equals(name)) {
                IntentResult r = submit(IntentNames.SELECT_CARD, args);
                if (r != null && r.status != IntentResult.Status.REJECTED) {
                    emit(SignalNames.CARD_SELECTED, args);
                }
                return toOp(r);
            }
            if ("confirm".equals(name) || IntentNames.CONFIRM_SELECT.equals(name)) {
                IntentResult r = submit(IntentNames.CONFIRM_SELECT, args);
                if (r != null && r.status != IntentResult.Status.REJECTED) {
                    emit(SignalNames.CONFIRMED, args);
                }
                return toOp(r);
            }
            return UiOpResult.unavailable("unknown action: " + name);
        }

        @Override
        public Map<String, Object> probeSlice() {
            Map<String, Object> m =
                    baseProbe(
                            Arrays.asList(
                                    "select_card",
                                    "confirm",
                                    "mount_select",
                                    IntentNames.SELECT_CARD,
                                    IntentNames.CONFIRM_SELECT));
            m.put(
                    "selectKind",
                    SurfaceIds.SELECT_HAND.equals(id()) ? "HAND" : "GRID");
            return m;
        }
    }

    static final class CombatRootSurface extends BaseSurface {
        CombatRootSurface() {
            super(SurfaceIds.COMBAT_SURFACE, SignalNames.SURFACE_OPENED, SignalNames.SURFACE_CLOSED);
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if ("attach_overlay".equals(name)) {
                if (!isMounted()) {
                    mount();
                    emit(SignalNames.SURFACE_OPENED);
                }
                return UiOpResult.ok("overlay attached");
            }
            if ("detach_overlay".equals(name)) {
                if (isMounted()) {
                    unmount();
                    emit(SignalNames.SURFACE_CLOSED);
                }
                return UiOpResult.ok("overlay detached");
            }
            if ("mount_combat".equals(name)) {
                PresentSurfaces.get(SurfaceIds.COMBAT_HAND).mount();
                PresentSurfaces.get(SurfaceIds.COMBAT_CARD_SLOTS).mount();
                PresentSurfaces.get(SurfaceIds.COMBAT_CONTROLS).mount();
                PresentSurfaces.get(SurfaceIds.COMBAT_ENERGY).mount();
                mount();
                emit(SignalNames.SURFACE_OPENED);
                return UiOpResult.ok("combat surfaces mounted");
            }
            return UiOpResult.unavailable("unknown action: " + name);
        }

        @Override
        public Map<String, Object> probeSlice() {
            return baseProbe(Arrays.asList("attach_overlay", "detach_overlay", "mount_combat"));
        }
    }

    private static String argString(Object[] args, int i) {
        if (args == null || i >= args.length || args[i] == null) {
            return "";
        }
        return String.valueOf(args[i]);
    }

    private static float argFloat(Object[] args, int i) {
        if (args == null || i >= args.length || args[i] == null) {
            return 0f;
        }
        if (args[i] instanceof Number) {
            return ((Number) args[i]).floatValue();
        }
        try {
            return Float.parseFloat(String.valueOf(args[i]));
        } catch (NumberFormatException e) {
            return 0f;
        }
    }
}
