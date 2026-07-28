package artframework.sts1.lab;

import artframework.api.UiOpResult;

import java.util.ArrayList;
import java.util.List;

/**
 * STS1 lab host: whitelist menu / save / char-select / abandon actions.
 * Soft-fails; schedules hitbox clicks on the GL thread when possible.
 */
public final class StsLabHost implements LabHost {

    public static final StsLabHost INSTANCE = new StsLabHost();

    private StsLabHost() {}

    @Override
    public LabStateSnapshot dump() {
        return StsLabState.dump();
    }

    @Override
    public UiOpResult clearSaves() {
        try {
            Class<?> playerClass = Class.forName("com.megacrit.cardcrawl.characters.AbstractPlayer$PlayerClass");
            Object[] values = (Object[]) playerClass.getMethod("values").invoke(null);
            Class<?> save = Class.forName("com.megacrit.cardcrawl.saveAndContinue.SaveAndContinue");
            int deleted = 0;
            for (Object pc : values) {
                String path = String.valueOf(save.getMethod("getPlayerSavePath", playerClass).invoke(null, pc));
                if (deleteLocal(path)) {
                    deleted++;
                }
                if (deleteLocal(path + ".backUp")) {
                    deleted++;
                }
            }
            return UiOpResult.ok("cleared " + deleted);
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    @Override
    public UiOpResult stripResumeButtons() {
        try {
            Object menu = mainMenu();
            if (menu == null) {
                return UiOpResult.unavailable("no main menu");
            }
            @SuppressWarnings("unchecked")
            List<Object> buttons = (List<Object>) StsLabState.field(menu.getClass(), menu, "buttons");
            if (buttons == null) {
                return UiOpResult.unavailable("no buttons");
            }
            boolean hasPlay = false;
            int removed = 0;
            for (int i = buttons.size() - 1; i >= 0; i--) {
                Object button = buttons.get(i);
                if (button == null) {
                    continue;
                }
                String result = clickResultName(button);
                if ("PLAY".equals(result)) {
                    hasPlay = true;
                    continue;
                }
                if ("ABANDON_RUN".equals(result) || "RESUME_GAME".equals(result)) {
                    buttons.remove(i);
                    removed++;
                }
            }
            if (removed > 0 && !hasPlay) {
                Class<?> menuButton = Class.forName("com.megacrit.cardcrawl.screens.mainMenu.MenuButton");
                Class<?> clickResult =
                        Class.forName("com.megacrit.cardcrawl.screens.mainMenu.MenuButton$ClickResult");
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object play = Enum.valueOf((Class) clickResult, "PLAY");
                Object nb =
                        menuButton
                                .getConstructor(clickResult, int.class)
                                .newInstance(play, Integer.valueOf(buttons.size()));
                buttons.add(nb);
            }
            return UiOpResult.ok("stripped " + removed);
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    @Override
    public UiOpResult openCharSelect() {
        try {
            Object menu = mainMenu();
            if (menu == null) {
                return UiOpResult.unavailable("no main menu");
            }
            final Object charSelect = StsLabState.field(menu.getClass(), menu, "charSelectScreen");
            if (charSelect == null) {
                return UiOpResult.unavailable("no char select");
            }
            post(new Runnable() {
                @Override
                public void run() {
                    try {
                        charSelect.getClass().getMethod("open", boolean.class).invoke(charSelect, Boolean.FALSE);
                    } catch (Throwable ignored) {
                    }
                }
            });
            return UiOpResult.ok("char select open scheduled");
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    @Override
    public UiOpResult selectCharacter(String characterId) {
        if (characterId == null || characterId.isEmpty()) {
            return UiOpResult.unavailable("character id required");
        }
        try {
            Object menu = mainMenu();
            if (menu == null) {
                return UiOpResult.unavailable("no main menu");
            }
            final Object screen = StsLabState.field(menu.getClass(), menu, "charSelectScreen");
            if (screen == null) {
                return UiOpResult.unavailable("no char select");
            }
            final Object option = findCharacterOption(screen, characterId);
            if (option == null) {
                return UiOpResult.unavailable("character not found: " + characterId);
            }
            Object locked = StsLabState.field(option.getClass(), option, "locked");
            if (Boolean.TRUE.equals(locked)) {
                return UiOpResult.unavailable("character locked: " + characterId);
            }
            final Object hb = StsLabState.field(option.getClass(), option, "hb");
            if (hb == null) {
                return UiOpResult.unavailable("no character hitbox");
            }
            post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Class<?> input =
                                Class.forName("com.megacrit.cardcrawl.helpers.input.InputHelper");
                        Object cx = StsLabState.field(hb.getClass(), hb, "cX");
                        Object cy = StsLabState.field(hb.getClass(), hb, "cY");
                        if (cx instanceof Number) {
                            input.getField("mX").setInt(null, Math.round(((Number) cx).floatValue()));
                        }
                        if (cy instanceof Number) {
                            input.getField("mY").setInt(null, Math.round(((Number) cy).floatValue()));
                        }
                        setBoolean(hb, "clicked", true);
                    } catch (Throwable ignored) {
                    }
                }
            });
            return UiOpResult.ok("character click scheduled " + characterId);
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    @Override
    public UiOpResult embark() {
        try {
            Object menu = mainMenu();
            if (menu == null) {
                return UiOpResult.unavailable("no main menu");
            }
            Object screen = StsLabState.field(menu.getClass(), menu, "charSelectScreen");
            if (screen == null) {
                return UiOpResult.unavailable("no char select");
            }
            final Object confirm = StsLabState.field(screen.getClass(), screen, "confirmButton");
            if (confirm == null) {
                return UiOpResult.unavailable("no embark button");
            }
            Object disabled = StsLabState.field(confirm.getClass(), confirm, "isDisabled");
            if (Boolean.TRUE.equals(disabled)) {
                return UiOpResult.unavailable("embark disabled");
            }
            final Object hb = StsLabState.field(confirm.getClass(), confirm, "hb");
            if (hb == null) {
                return UiOpResult.unavailable("no embark hitbox");
            }
            post(new Runnable() {
                @Override
                public void run() {
                    try {
                        setBoolean(hb, "clicked", true);
                    } catch (Throwable ignored) {
                    }
                }
            });
            return UiOpResult.ok("embark scheduled");
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    @Override
    public UiOpResult setSeed(String seedText) {
        if (seedText == null || seedText.isEmpty()) {
            return UiOpResult.ok("seed skipped");
        }
        try {
            Class<?> seedHelper = Class.forName("com.megacrit.cardcrawl.helpers.SeedHelper");
            try {
                seedHelper.getMethod("setSeed", String.class).invoke(null, seedText);
                return UiOpResult.ok("seed set " + seedText);
            } catch (NoSuchMethodException e) {
                // fall through to panel
            }
            Object menu = mainMenu();
            if (menu == null) {
                return UiOpResult.unavailable("no main menu for seed");
            }
            Object panel = StsLabState.field(menu.getClass(), menu, "seedPanel");
            if (panel == null) {
                return UiOpResult.unavailable("no seed panel");
            }
            try {
                java.lang.reflect.Field tf = findField(panel.getClass(), "textField");
                if (tf != null) {
                    tf.setAccessible(true);
                    tf.set(panel, seedText);
                }
            } catch (Throwable ignored) {
            }
            return UiOpResult.ok("seed text applied " + seedText);
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    @Override
    public UiOpResult menuClick(String clickResult) {
        if (clickResult == null || clickResult.isEmpty()) {
            return UiOpResult.unavailable("ClickResult required");
        }
        try {
            Object menu = mainMenu();
            if (menu == null) {
                return UiOpResult.unavailable("no main menu");
            }
            @SuppressWarnings("unchecked")
            List<Object> buttons = (List<Object>) StsLabState.field(menu.getClass(), menu, "buttons");
            if (buttons == null) {
                return UiOpResult.unavailable("no buttons");
            }
            String want = clickResult.toUpperCase();
            for (Object button : buttons) {
                if (button == null) {
                    continue;
                }
                if (!want.equals(clickResultName(button))) {
                    continue;
                }
                final Object hb = StsLabState.field(button.getClass(), button, "hb");
                if (hb == null) {
                    return UiOpResult.unavailable("no hitbox for " + want);
                }
                post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setBoolean(hb, "clicked", true);
                        } catch (Throwable ignored) {
                        }
                    }
                });
                return UiOpResult.ok("menu-click scheduled " + want);
            }
            return UiOpResult.unavailable("button not found: " + want);
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    @Override
    public UiOpResult abandon() {
        try {
            LabStateSnapshot s = dump();
            if (s.abandonConfirmOpen) {
                return abandonConfirm();
            }
            if (s.onMainMenu() && s.hasAbandon) {
                return menuClick("ABANDON_RUN");
            }
            if (s.endScreen) {
                return returnToMenu();
            }
            if (!s.inGame) {
                if (s.onMainMenu()) {
                    return UiOpResult.ok("already menu");
                }
                return UiOpResult.unavailable("not in run");
            }
            // Best-effort: open settings via TopPanel then rely on follow-up ticks / menu abandon.
            try {
                Class<?> dungeon = Class.forName("com.megacrit.cardcrawl.dungeons.AbstractDungeon");
                Object top = StsLabState.field(dungeon, null, "topPanel");
                if (top != null) {
                    Object settingsHb = StsLabState.field(top.getClass(), top, "settingsHb");
                    if (settingsHb == null) {
                        Object btn = StsLabState.field(top.getClass(), top, "settingsButton");
                        if (btn != null) {
                            settingsHb = StsLabState.field(btn.getClass(), btn, "hb");
                        }
                    }
                    if (settingsHb != null) {
                        final Object hb = settingsHb;
                        post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    setBoolean(hb, "clicked", true);
                                } catch (Throwable ignored) {
                                }
                            }
                        });
                        return UiOpResult.ok("settings click scheduled (abandon follow-up)");
                    }
                }
            } catch (Throwable ignored) {
            }
            // Fallback: delete saves and hope next menu load is clean (does not exit run alone).
            return UiOpResult.unavailable("abandon path unavailable; use ensure-menu after death or settings");
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    @Override
    public UiOpResult abandonConfirm() {
        try {
            Object menu = mainMenu();
            if (menu != null) {
                Object popup = StsLabState.field(menu.getClass(), menu, "abandonPopup");
                if (popup == null) {
                    popup = StsLabState.field(menu.getClass(), menu, "confirmPopup");
                }
                if (popup != null) {
                    Object yes = StsLabState.field(popup.getClass(), popup, "yesHb");
                    if (yes == null) {
                        yes = StsLabState.field(popup.getClass(), popup, "confirmHb");
                    }
                    if (yes == null) {
                        Object yesBtn = StsLabState.field(popup.getClass(), popup, "yes");
                        if (yesBtn != null) {
                            yes = StsLabState.field(yesBtn.getClass(), yesBtn, "hb");
                        }
                    }
                    if (yes != null) {
                        final Object hb = yes;
                        post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    setBoolean(hb, "clicked", true);
                                } catch (Throwable ignored) {
                                }
                            }
                        });
                        return UiOpResult.ok("abandon confirm scheduled");
                    }
                    try {
                        popup.getClass().getMethod("yes").invoke(popup);
                        return UiOpResult.ok("abandon confirm yes()");
                    } catch (Throwable ignored) {
                    }
                }
            }
            return menuClick("ABANDON_RUN");
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    @Override
    public UiOpResult returnToMenu() {
        try {
            Class<?> dungeon = Class.forName("com.megacrit.cardcrawl.dungeons.AbstractDungeon");
            Object death = StsLabState.field(dungeon, null, "deathScreen");
            if (death != null) {
                Object btn = StsLabState.field(death.getClass(), death, "returnButton");
                if (btn == null) {
                    btn = StsLabState.field(death.getClass(), death, "button");
                }
                if (btn != null) {
                    final Object hb = StsLabState.field(btn.getClass(), btn, "hb");
                    if (hb != null) {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    setBoolean(hb, "clicked", true);
                                } catch (Throwable ignored) {
                                }
                            }
                        });
                        return UiOpResult.ok("return-menu scheduled");
                    }
                }
            }
            return UiOpResult.unavailable("no return-to-menu control");
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    @Override
    public UiOpResult proceed() {
        try {
            Class<?> dungeon = Class.forName("com.megacrit.cardcrawl.dungeons.AbstractDungeon");
            Object overlay = StsLabState.field(dungeon, null, "overlayMenu");
            if (overlay == null) {
                return UiOpResult.unavailable("no overlayMenu");
            }
            Object proceed = StsLabState.field(overlay.getClass(), overlay, "proceedButton");
            if (proceed == null) {
                return UiOpResult.unavailable("no proceed button");
            }
            final Object hb = StsLabState.field(proceed.getClass(), proceed, "hb");
            if (hb == null) {
                return UiOpResult.unavailable("no proceed hitbox");
            }
            post(new Runnable() {
                @Override
                public void run() {
                    try {
                        setBoolean(hb, "clicked", true);
                    } catch (Throwable ignored) {
                    }
                }
            });
            return UiOpResult.ok("proceed scheduled");
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    private static Object mainMenu() throws Exception {
        Class<?> game = Class.forName("com.megacrit.cardcrawl.core.CardCrawlGame");
        return StsLabState.field(game, null, "mainMenuScreen");
    }

    private static String clickResultName(Object button) throws Exception {
        Object result = StsLabState.field(button.getClass(), button, "result");
        if (result == null) {
            return "";
        }
        String name = String.valueOf(result);
        if (name.contains(".")) {
            name = name.substring(name.lastIndexOf('.') + 1);
        }
        return name;
    }

    private static Object findCharacterOption(Object screen, String characterId) throws Exception {
        List<Object> options = new ArrayList<Object>();
        Object visible = StsLabState.field(screen.getClass(), screen, "options");
        Object all = null;
        try {
            all = StsLabState.field(screen.getClass(), screen, "allOptions");
        } catch (Throwable ignored) {
        }
        if (all instanceof List) {
            for (Object o : (List<?>) all) {
                options.add(o);
            }
        } else if (visible instanceof List) {
            for (Object o : (List<?>) visible) {
                options.add(o);
            }
        }
        String want = characterId.trim();
        for (Object opt : options) {
            if (opt == null) {
                continue;
            }
            String desc = StsLabState.describeCharacter(opt);
            if (desc.equalsIgnoreCase(want) || desc.toUpperCase().contains(want.toUpperCase())) {
                // page if needed
                pageToOption(screen, options, opt);
                return opt;
            }
            Object name = StsLabState.field(opt.getClass(), opt, "name");
            if (name != null && String.valueOf(name).equalsIgnoreCase(want)) {
                pageToOption(screen, options, opt);
                return opt;
            }
        }
        return null;
    }

    private static void pageToOption(Object screen, List<Object> allOptions, Object target) {
        try {
            int index = allOptions.indexOf(target);
            if (index < 0) {
                return;
            }
            int per = 4;
            try {
                Object v = StsLabState.field(screen.getClass(), screen, "optionsPerIndex");
                if (v instanceof Number) {
                    per = ((Number) v).intValue();
                }
            } catch (Throwable ignored) {
            }
            if (per <= 0) {
                return;
            }
            int targetPage = index / per;
            int current = 0;
            try {
                Object v = StsLabState.field(screen.getClass(), screen, "selectIndex");
                if (v instanceof Number) {
                    current = ((Number) v).intValue();
                }
            } catch (Throwable ignored) {
            }
            java.lang.reflect.Method setCurrent =
                    screen.getClass().getDeclaredMethod("setCurrentOptions", boolean.class);
            setCurrent.setAccessible(true);
            while (current < targetPage) {
                setCurrent.invoke(screen, Boolean.TRUE);
                current++;
            }
            while (current > targetPage) {
                setCurrent.invoke(screen, Boolean.FALSE);
                current--;
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean deleteLocal(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        try {
            Class<?> gdx = Class.forName("com.badlogic.gdx.Gdx");
            Object files = gdx.getField("files").get(null);
            Object handle = files.getClass().getMethod("local", String.class).invoke(files, path);
            Boolean exists = (Boolean) handle.getClass().getMethod("exists").invoke(handle);
            if (exists != null && exists.booleanValue()) {
                Boolean deleted = (Boolean) handle.getClass().getMethod("delete").invoke(handle);
                return deleted != null && deleted.booleanValue();
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static void setBoolean(Object instance, String name, boolean value) throws Exception {
        Class<?> c = instance.getClass();
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
    }

    private static java.lang.reflect.Field findField(Class<?> type, String name) {
        Class<?> c = type;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private static void post(Runnable r) {
        try {
            Class<?> gdx = Class.forName("com.badlogic.gdx.Gdx");
            Object app = gdx.getField("app").get(null);
            if (app != null) {
                app.getClass().getMethod("postRunnable", Runnable.class).invoke(app, r);
                return;
            }
        } catch (Throwable ignored) {
        }
        r.run();
    }

    @Override
    public void yieldFrame() {
        // Console/recipe runs on a non-render thread; postRunnable work needs real frames.
        try {
            Thread.sleep(350L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static UiOpResult unavailable(Throwable t) {
        return UiOpResult.unavailable(
                t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
    }
}
