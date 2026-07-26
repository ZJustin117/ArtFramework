package spireui.c2;

import spireui.api.WindowClass;
import spireui.api.WindowDef;

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

    private NativeTemplateRuntime() {}

    /** Logic path is available (bind/unbind + template hooks + entity present). */
    public static boolean isAvailable() {
        return true;
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
        slot.activate();
    }

    public static void unbind(WindowDef def) {
        if (def == null) {
            return;
        }
        TemplateSlot slot = slotFor(resolveKey(def));
        if (slot != null) {
            slot.deactivate();
        }
    }

    public static boolean isMapBound() {
        return MAP_TEMPLATE.isActive();
    }

    public static boolean isEventBound() {
        return EVENT_TEMPLATE.isActive();
    }

    public static boolean isSelectGridBound() {
        return GRID_SELECT.isActive();
    }

    public static boolean isSelectHandBound() {
        return HAND_SELECT.isActive();
    }

    public static boolean isEndTurnBound() {
        return END_TURN_TEMPLATE.isActive();
    }

    public static boolean isBound(String resourceOrId) {
        TemplateSlot slot = slotFor(resourceOrId);
        return slot != null && slot.isActive();
    }

    public static void resetForTests() {
        MAP_TEMPLATE.resetForTests();
        EVENT_TEMPLATE.resetForTests();
        GRID_SELECT.resetForTests();
        HAND_SELECT.resetForTests();
        END_TURN_TEMPLATE.resetForTests();
        ENTITY_PRESENT.resetForTests();
    }

    private static String resolveKey(WindowDef def) {
        if (def.resource != null && !def.resource.isEmpty()) {
            return def.resource;
        }
        return def.id;
    }

    private static TemplateSlot slotFor(String key) {
        if (key == null) {
            return null;
        }
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
