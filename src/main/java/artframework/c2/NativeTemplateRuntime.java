package artframework.c2;

import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;
import artframework.render.RenderHosts;

/**
 * C2: native STS UI templates. Logic registry for bind/unbind; hooks live on per-template
 * objects. Engine patches (later) call into active templates — none required for unit gate.
 */
public final class NativeTemplateRuntime {

    public static final String MAP = NativeTemplateIds.MAP;
    public static final String EVENT = NativeTemplateIds.EVENT;
    public static final String SELECT_GRID = NativeTemplateIds.SELECT_GRID;
    public static final String SELECT_HAND = NativeTemplateIds.SELECT_HAND;
    public static final String END_TURN = NativeTemplateIds.END_TURN;

    private static final MapTemplate MAP_TEMPLATE = new MapTemplate();
    private static final EventTemplate EVENT_TEMPLATE = new EventTemplate();
    private static final SelectTemplate GRID_SELECT =
            new SelectTemplate(SelectKind.GRID, NativeTemplateIds.SELECT_GRID);
    private static final SelectTemplate HAND_SELECT =
            new SelectTemplate(SelectKind.HAND, NativeTemplateIds.SELECT_HAND);
    private static final EndTurnTemplate END_TURN_TEMPLATE = new EndTurnTemplate();
    private static final DefaultEntityPresent ENTITY_PRESENT = new DefaultEntityPresent();
    private static final PresentationContext TEMPLATE_CONTEXT =
            PresentationRegistry.context("c2-templates");
    private static final PresentationWorld TEMPLATE_WORLD = TEMPLATE_CONTEXT.world();
    private static boolean renderBridgeInstalled;

    private NativeTemplateRuntime() {}

    /** Logic path is available (bind/unbind + template hooks + entity present). */
    public static boolean isAvailable() {
        return true;
    }

    private static void ensureRenderBridge() {
        if (renderBridgeInstalled) {
            return;
        }
        renderBridgeInstalled = true;
        ENTITY_PRESENT.addListener(new EntityPresentListener() {
            @Override
            public void onAttached(EntitySlot slot) {
                RenderHosts.get().syncEntityPresent();
            }

            @Override
            public void onSynced(EntitySlot slot) {
                RenderHosts.get().syncEntityPresent();
            }

            @Override
            public void onLaidOut(EntitySlot slot) {
                RenderHosts.get().syncEntityPresent();
            }

            @Override
            public void onDetached(String slotId) {
                RenderHosts.get().syncEntityPresent();
            }
        });
    }

    public static MapTemplate map() {
        return MAP_TEMPLATE;
    }

    public static EventTemplate event() {
        return EVENT_TEMPLATE;
    }

    public static SelectTemplate selectGrid() {
        return GRID_SELECT;
    }

    public static SelectTemplate selectHand() {
        return HAND_SELECT;
    }

    public static EndTurnTemplate endTurn() {
        return END_TURN_TEMPLATE;
    }

    /** Shared entity presenter registry (players/cards/relics/monsters). */
    public static DefaultEntityPresent entities() {
        ensureRenderBridge();
        return ENTITY_PRESENT;
    }

    /**
     * Activate a registered native template by {@link WindowDef#resource} (or id fallback).
     */
    public static void bind(WindowDef def) {
        if (def == null) {
            throw new IllegalArgumentException("def required");
        }
        if (def.windowClass != WindowClass.NATIVE_TEMPLATE) {
            throw new IllegalArgumentException("expected NATIVE_TEMPLATE: " + def.id);
        }
        String key = resolveKey(def);
        TemplateSlot slot = slotFor(key);
        if (slot == null) {
            throw new IllegalArgumentException("unknown native template: " + key);
        }
        putBound(key, true);
        slot.activate();
        NativeComponents.syncMountFromRuntime();
    }

    public static void unbind(WindowDef def) {
        if (def == null) {
            return;
        }
        TemplateSlot slot = slotFor(resolveKey(def));
        if (slot != null) {
            putBound(resolveKey(def), false);
            slot.deactivate();
            NativeComponents.clearSignals(resolveKey(def));
        }
        NativeComponents.syncMountFromRuntime();
    }

    public static boolean isMapBound() {
        return isBound(NativeTemplateIds.MAP);
    }

    public static boolean isEventBound() {
        return isBound(NativeTemplateIds.EVENT);
    }

    public static boolean isSelectGridBound() {
        return isBound(NativeTemplateIds.SELECT_GRID);
    }

    public static boolean isSelectHandBound() {
        return isBound(NativeTemplateIds.SELECT_HAND);
    }

    public static boolean isEndTurnBound() {
        return isBound(NativeTemplateIds.END_TURN);
    }

    public static boolean isBound(String resourceOrId) {
        String key = NativeTemplateIds.canonicalize(resourceOrId);
        if (slotFor(key) == null) return false;
        EntityId entity = TEMPLATE_CONTEXT.entity(new PresentationKey("sts1.template", key));
        if (entity == null) return false;
        NativeTemplateStateComponent state = TEMPLATE_WORLD.get(entity, NativeTemplateStateComponent.class);
        return state != null && state.bound;
    }

    public static void resetForTests() {
        MAP_TEMPLATE.resetForTests();
        EVENT_TEMPLATE.resetForTests();
        GRID_SELECT.resetForTests();
        HAND_SELECT.resetForTests();
        END_TURN_TEMPLATE.resetForTests();
        ENTITY_PRESENT.resetForTests();
        for (EntityId entity : new java.util.ArrayList<EntityId>(TEMPLATE_CONTEXT.entities())) {
            if (TEMPLATE_WORLD.get(entity, NativeTemplateStateComponent.class) != null) {
                TEMPLATE_CONTEXT.destroy(entity);
            }
        }
        renderBridgeInstalled = false;
        NativeComponents.resetForTests();
    }

    static void setEventId(String eventId) {
        TEMPLATE_WORLD.put(templateEntity(NativeTemplateIds.EVENT), EventTemplateDataComponent.class,
                new EventTemplateDataComponent(eventId));
    }

    static String eventId() {
        EventTemplateDataComponent state = TEMPLATE_WORLD.get(
                templateEntity(NativeTemplateIds.EVENT), EventTemplateDataComponent.class);
        return state != null ? state.eventId : "";
    }

    static void setEndTurnEnabled(boolean enabled) {
        TEMPLATE_WORLD.put(templateEntity(NativeTemplateIds.END_TURN), EndTurnTemplateDataComponent.class,
                new EndTurnTemplateDataComponent(enabled));
    }

    static boolean endTurnEnabled() {
        EndTurnTemplateDataComponent state = TEMPLATE_WORLD.get(
                templateEntity(NativeTemplateIds.END_TURN), EndTurnTemplateDataComponent.class);
        return state == null || state.buttonEnabled;
    }

    /** ECS-derived map pin compatibility view. */
    public static java.util.List<MapPin> mapPins() {
        java.util.List<MapPin> pins = new java.util.ArrayList<MapPin>();
        for (EntityId entity : TEMPLATE_CONTEXT.entities()) {
            MapPinComponent pin = TEMPLATE_WORLD.get(entity, MapPinComponent.class);
            if (pin != null) pins.add(pin.toPin());
        }
        return java.util.Collections.unmodifiableList(pins);
    }

    /** ECS-derived end-turn enabled compatibility query. */
    public static boolean isEndTurnEnabled() {
        return endTurnEnabled();
    }

    private static String resolveKey(WindowDef def) {
        String key;
        if (def.resource != null && !def.resource.isEmpty()) {
            key = def.resource;
        } else {
            key = def.id;
        }
        return NativeTemplateIds.canonicalize(key);
    }

    private static void putBound(String resourceOrId, boolean bound) {
        String key = NativeTemplateIds.canonicalize(resourceOrId);
        EntityId entity = templateEntity(key);
        TEMPLATE_WORLD.put(entity, NativeTemplateStateComponent.class,
                new NativeTemplateStateComponent(key, bound));
    }

    private static EntityId templateEntity(String resourceOrId) {
        String key = NativeTemplateIds.canonicalize(resourceOrId);
        PresentationKey presentationKey = new PresentationKey("sts1.template", key);
        EntityId entity = TEMPLATE_CONTEXT.entity(presentationKey);
        return entity != null ? entity : TEMPLATE_CONTEXT.create(presentationKey, key,
                "native-template", "c2");
    }

    private static TemplateSlot slotFor(String key) {
        if (key == null) {
            return null;
        }
        key = NativeTemplateIds.canonicalize(key);
        if (NativeTemplateIds.MAP.equals(key)) {
            return new TemplateSlot() {
                @Override
                public void activate() {
                    MAP_TEMPLATE.activate();
                }

                @Override
                public void deactivate() {
                    MAP_TEMPLATE.deactivate();
                }

                @Override
                public boolean isActive() {
                    return MAP_TEMPLATE.isActive();
                }
            };
        }
        if (NativeTemplateIds.EVENT.equals(key)) {
            return new TemplateSlot() {
                @Override
                public void activate() {
                    EVENT_TEMPLATE.activate();
                }

                @Override
                public void deactivate() {
                    EVENT_TEMPLATE.deactivate();
                }

                @Override
                public boolean isActive() {
                    return EVENT_TEMPLATE.isActive();
                }
            };
        }
        if (NativeTemplateIds.SELECT_GRID.equals(key)) {
            return new TemplateSlot() {
                @Override
                public void activate() {
                    GRID_SELECT.activate();
                }

                @Override
                public void deactivate() {
                    GRID_SELECT.deactivate();
                }

                @Override
                public boolean isActive() {
                    return GRID_SELECT.isActive();
                }
            };
        }
        if (NativeTemplateIds.SELECT_HAND.equals(key)) {
            return new TemplateSlot() {
                @Override
                public void activate() {
                    HAND_SELECT.activate();
                }

                @Override
                public void deactivate() {
                    HAND_SELECT.deactivate();
                }

                @Override
                public boolean isActive() {
                    return HAND_SELECT.isActive();
                }
            };
        }
        if (NativeTemplateIds.END_TURN.equals(key)) {
            return new TemplateSlot() {
                @Override
                public void activate() {
                    END_TURN_TEMPLATE.activate();
                }

                @Override
                public void deactivate() {
                    END_TURN_TEMPLATE.deactivate();
                }

                @Override
                public boolean isActive() {
                    return END_TURN_TEMPLATE.isActive();
                }
            };
        }
        return null;
    }

    private interface TemplateSlot {
        void activate();

        void deactivate();

        boolean isActive();
    }
}
