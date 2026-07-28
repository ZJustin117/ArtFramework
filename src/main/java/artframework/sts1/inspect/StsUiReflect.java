package artframework.sts1.inspect;

import artframework.api.UiOpResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Whitelist STS native UI dump/click for lab. Avoids arbitrary reflection; failures are soft.
 */
public final class StsUiReflect {

    private StsUiReflect() {}

    public static Map<String, Object> dump() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        try {
            Class.forName("com.megacrit.cardcrawl.dungeons.AbstractDungeon");
        } catch (Throwable t) {
            out.put("error", t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
            out.put("available", Boolean.FALSE);
            return out;
        }
        out.put("available", Boolean.TRUE);
        try {
            Class<?> dungeon = Class.forName("com.megacrit.cardcrawl.dungeons.AbstractDungeon");
            Object screen = softField(dungeon, null, "screen");
            out.put("screen", screen != null ? String.valueOf(screen) : "null");
            Object player = softField(dungeon, null, "player");
            out.put("inGame", Boolean.valueOf(player != null));
            Object room = softInvokeStatic(dungeon, "getCurrRoom");
            if (room != null) {
                out.put("room", room.getClass().getSimpleName());
                Object event = softField(room.getClass(), room, "event");
                if (event != null) {
                    out.put("event", event.getClass().getSimpleName());
                }
            }
            Object overlay = softField(dungeon, null, "overlayMenu");
            if (overlay != null) {
                Object endTurn = softField(overlay.getClass(), overlay, "endTurnButton");
                out.put("endTurnPresent", Boolean.valueOf(endTurn != null));
            } else {
                out.put("endTurnPresent", Boolean.FALSE);
            }
            Object grid = softField(dungeon, null, "gridSelectScreen");
            out.put("gridSelect", Boolean.valueOf(grid != null && isVisible(grid)));
            Object hand = softField(dungeon, null, "handCardSelectScreen");
            out.put("handSelect", Boolean.valueOf(hand != null && isVisible(hand)));
        } catch (Throwable t) {
            out.put(
                    "partialError",
                    t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
        }
        return out;
    }

    /**
     * Whitelist paths: {@code endturn}, {@code grid.confirm}, {@code event} + index via
     * {@link #click(String, String)}.
     */
    public static UiOpResult click(String path) {
        return click(path, null);
    }

    public static UiOpResult click(String path, String extra) {
        if (path == null || path.isEmpty()) {
            return UiOpResult.unavailable("path required");
        }
        String p = path.toLowerCase();
        try {
            if ("endturn".equals(p) || "end-turn".equals(p)) {
                return clickEndTurn();
            }
            if ("grid.confirm".equals(p) || "grid_confirm".equals(p) || "confirm".equals(p)) {
                return clickGridConfirm();
            }
            if ("event".equals(p)) {
                int index = 0;
                if (extra != null && !extra.isEmpty()) {
                    try {
                        index = Integer.parseInt(extra);
                    } catch (NumberFormatException e) {
                        return UiOpResult.unavailable("event index required");
                    }
                }
                return clickEventOption(index);
            }
            return UiOpResult.unavailable(
                    "unknown path: " + path + " (endturn|grid.confirm|event <i>)");
        } catch (Throwable t) {
            return UiOpResult.unavailable(
                    t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
        }
    }

    private static UiOpResult clickEndTurn() throws Exception {
        Class<?> dungeon = Class.forName("com.megacrit.cardcrawl.dungeons.AbstractDungeon");
        Object overlay = field(dungeon, null, "overlayMenu");
        if (overlay == null) {
            return UiOpResult.unavailable("no overlayMenu");
        }
        Object endTurn = field(overlay.getClass(), overlay, "endTurnButton");
        if (endTurn == null) {
            return UiOpResult.unavailable("no end turn button");
        }
        postRunnable(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            endTurn
                                    .getClass()
                                    .getMethod("disable", boolean.class)
                                    .invoke(endTurn, Boolean.TRUE);
                        } catch (Throwable t) {
                            try {
                                Object hb = field(endTurn.getClass(), endTurn, "hb");
                                if (hb != null) {
                                    setBoolean(hb.getClass(), hb, "clicked", true);
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                });
        return UiOpResult.ok("endturn click scheduled");
    }

    private static UiOpResult clickGridConfirm() throws Exception {
        Class<?> dungeon = Class.forName("com.megacrit.cardcrawl.dungeons.AbstractDungeon");
        Object gcs = field(dungeon, null, "gridSelectScreen");
        if (gcs == null) {
            return UiOpResult.unavailable("no grid select");
        }
        Object confirm = field(gcs.getClass(), gcs, "confirmButton");
        if (confirm == null) {
            return UiOpResult.unavailable("no confirm button");
        }
        Object hb = field(confirm.getClass(), confirm, "hb");
        if (hb == null) {
            return UiOpResult.unavailable("no confirm hitbox");
        }
        setBoolean(hb.getClass(), hb, "clicked", true);
        return UiOpResult.ok("grid.confirm clicked");
    }

    private static UiOpResult clickEventOption(final int index) throws Exception {
        Class<?> dungeon = Class.forName("com.megacrit.cardcrawl.dungeons.AbstractDungeon");
        Object room = invokeStatic(dungeon, "getCurrRoom");
        if (room == null) {
            return UiOpResult.unavailable("no room");
        }
        final Object event = field(room.getClass(), room, "event");
        if (event == null) {
            return UiOpResult.unavailable("no event");
        }
        postRunnable(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            java.lang.reflect.Method m =
                                    Class.forName("com.megacrit.cardcrawl.events.AbstractEvent")
                                            .getDeclaredMethod("buttonEffect", int.class);
                            m.setAccessible(true);
                            m.invoke(event, Integer.valueOf(index));
                        } catch (Throwable ignored) {
                        }
                    }
                });
        return UiOpResult.ok("event option scheduled " + index);
    }

    private static boolean isVisible(Object screen) {
        try {
            Object v = field(screen.getClass(), screen, "isOpen");
            if (v instanceof Boolean) {
                return ((Boolean) v).booleanValue();
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    private static void postRunnable(Runnable r) throws Exception {
        try {
            Class<?> gdx = Class.forName("com.badlogic.gdx.Gdx");
            Object app = field(gdx, null, "app");
            if (app != null) {
                app.getClass().getMethod("postRunnable", Runnable.class).invoke(app, r);
                return;
            }
        } catch (Throwable ignored) {
        }
        r.run();
    }

    private static Object softField(Class<?> type, Object instance, String name) {
        try {
            return field(type, instance, name);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object softInvokeStatic(Class<?> type, String method) {
        try {
            return invokeStatic(type, method);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object field(Class<?> type, Object instance, String name) throws Exception {
        Class<?> c = type;
        while (c != null) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(instance);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private static void setBoolean(Class<?> type, Object instance, String name, boolean value)
            throws Exception {
        Class<?> c = type;
        while (c != null) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.setBoolean(instance, value);
                return;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Object invokeStatic(Class<?> type, String method) throws Exception {
        try {
            return type.getMethod(method).invoke(null);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (java.lang.reflect.InvocationTargetException e) {
            return null;
        } catch (Throwable t) {
            return null;
        }
    }
}
