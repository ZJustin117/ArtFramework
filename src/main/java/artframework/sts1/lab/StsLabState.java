package artframework.sts1.lab;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads STS main-menu / run state into {@link LabStateSnapshot}. Soft-fails without STS.
 */
public final class StsLabState {

    private StsLabState() {}

    public static LabStateSnapshot dump() {
        try {
            Class.forName("com.megacrit.cardcrawl.core.CardCrawlGame");
        } catch (Throwable t) {
            return LabStateSnapshot.builder()
                    .message(t.getMessage() != null ? t.getMessage() : "sts unavailable")
                    .build();
        }
        try {
            return dumpLive();
        } catch (Throwable t) {
            return LabStateSnapshot.builder()
                    .message(t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName())
                    .build();
        }
    }

    public static Map<String, Object> dumpMap() {
        return dump().toMap();
    }

    private static LabStateSnapshot dumpLive() throws Exception {
        Class<?> game = Class.forName("com.megacrit.cardcrawl.core.CardCrawlGame");
        Object mode = field(game, null, "mode");
        String modeName = mode != null ? String.valueOf(mode) : "";
        boolean gameplay = modeName.contains("GAMEPLAY");

        Object menu = field(game, null, "mainMenuScreen");
        String menuScreen = "";
        boolean fading = false;
        boolean hasResume = false;
        boolean hasAbandon = false;
        boolean hasPlay = false;
        List<String> buttons = new ArrayList<String>();
        boolean charSelectOpen = false;
        boolean characterSelected = false;
        boolean embarkEnabled = false;
        String selectedCharacter = "";
        List<String> characters = new ArrayList<String>();
        boolean seedPanelOpen = false;
        boolean abandonConfirmOpen = false;

        if (menu != null) {
            Object screen = field(menu.getClass(), menu, "screen");
            menuScreen = screen != null ? String.valueOf(screen) : "";
            if (menuScreen.contains(".")) {
                menuScreen = menuScreen.substring(menuScreen.lastIndexOf('.') + 1);
            }
            Object isFading = field(menu.getClass(), menu, "isFadingOut");
            Object faded = field(menu.getClass(), menu, "fadedOut");
            fading =
                    Boolean.TRUE.equals(isFading)
                            || Boolean.TRUE.equals(faded);
            Object buttonList = field(menu.getClass(), menu, "buttons");
            if (buttonList instanceof List) {
                for (Object b : (List<?>) buttonList) {
                    if (b == null) {
                        continue;
                    }
                    Object result = field(b.getClass(), b, "result");
                    String name = result != null ? String.valueOf(result) : "";
                    if (name.contains(".")) {
                        name = name.substring(name.lastIndexOf('.') + 1);
                    }
                    buttons.add(name);
                    if ("RESUME_GAME".equals(name)) {
                        hasResume = true;
                    }
                    if ("ABANDON_RUN".equals(name)) {
                        hasAbandon = true;
                    }
                    if ("PLAY".equals(name)) {
                        hasPlay = true;
                    }
                }
            }
            Object charSelect = field(menu.getClass(), menu, "charSelectScreen");
            if (charSelect != null && "CHAR_SELECT".equals(menuScreen)) {
                charSelectOpen = true;
                boolean[] sel = new boolean[1];
                boolean[] emb = new boolean[1];
                String[] selName = new String[] {""};
                readCharSelect(charSelect, characters, sel, emb, selName);
                characterSelected = sel[0];
                embarkEnabled = emb[0];
                selectedCharacter = selName[0];
            }
            Object seedPanel = field(menu.getClass(), menu, "seedPanel");
            if (seedPanel != null) {
                Object shown = field(seedPanel.getClass(), seedPanel, "shown");
                seedPanelOpen = Boolean.TRUE.equals(shown);
            }
        }

        boolean inGame = gameplay;
        boolean inCombat = false;
        String roomPhase = "";
        boolean endScreen = false;
        try {
            Class<?> dungeon = Class.forName("com.megacrit.cardcrawl.dungeons.AbstractDungeon");
            Object player = field(dungeon, null, "player");
            if (player != null) {
                inGame = true;
            }
            Object screen = field(dungeon, null, "screen");
            String sn = screen != null ? String.valueOf(screen) : "";
            if (sn.contains("DEATH") || sn.contains("VICTORY") || sn.contains("UNLOCK")) {
                endScreen = true;
            }
            Object room = null;
            try {
                Object node = field(dungeon, null, "currMapNode");
                if (node != null) {
                    room = node.getClass().getMethod("getRoom").invoke(node);
                }
            } catch (Throwable ignored) {
            }
            if (room != null) {
                Object phase = field(room.getClass(), room, "phase");
                roomPhase = phase != null ? String.valueOf(phase) : "";
                if (roomPhase.contains(".")) {
                    roomPhase = roomPhase.substring(roomPhase.lastIndexOf('.') + 1);
                }
                Object battleOver = field(room.getClass(), room, "isBattleOver");
                inCombat =
                        "COMBAT".equals(roomPhase)
                                && !Boolean.TRUE.equals(battleOver);
            }
        } catch (Throwable ignored) {
        }

        // Confirm popup heuristic
        try {
            if (menu != null) {
                Object conf = field(menu.getClass(), menu, "abandonPopup");
                if (conf == null) {
                    conf = field(menu.getClass(), menu, "confirmPopup");
                }
                if (conf != null) {
                    Object shown = field(conf.getClass(), conf, "shown");
                    if (shown == null) {
                        shown = field(conf.getClass(), conf, "isVisible");
                    }
                    abandonConfirmOpen = Boolean.TRUE.equals(shown);
                }
            }
        } catch (Throwable ignored) {
        }

        return LabStateSnapshot.builder()
                .mode(modeName.contains(".") ? modeName.substring(modeName.lastIndexOf('.') + 1) : modeName)
                .menuScreen(menuScreen)
                .inGame(inGame)
                .inCombat(inCombat)
                .fading(fading)
                .hasResume(hasResume)
                .hasAbandon(hasAbandon)
                .hasPlay(hasPlay)
                .charSelectOpen(charSelectOpen)
                .characterSelected(characterSelected)
                .embarkEnabled(embarkEnabled)
                .seedPanelOpen(seedPanelOpen)
                .endScreen(endScreen)
                .abandonConfirmOpen(abandonConfirmOpen)
                .roomPhase(roomPhase)
                .selectedCharacter(selectedCharacter)
                .buttons(buttons)
                .characters(characters)
                .build();
    }

    private static void readCharSelect(
            Object charSelect,
            List<String> characters,
            boolean[] characterSelected,
            boolean[] embarkEnabled,
            String[] selectedCharacter)
            throws Exception {
        characters.clear();
        Object options = field(charSelect.getClass(), charSelect, "options");
        try {
            Object all = field(charSelect.getClass(), charSelect, "allOptions");
            if (all instanceof List && !((List<?>) all).isEmpty()) {
                options = all;
            }
        } catch (Throwable ignored) {
        }
        if (options instanceof List) {
            for (Object opt : (List<?>) options) {
                if (opt == null) {
                    continue;
                }
                String desc = describeCharacter(opt);
                if (!desc.isEmpty()) {
                    characters.add(desc);
                }
                Object selected = field(opt.getClass(), opt, "selected");
                if (Boolean.TRUE.equals(selected)) {
                    characterSelected[0] = true;
                    selectedCharacter[0] = desc;
                }
            }
        }
        Object confirm = field(charSelect.getClass(), charSelect, "confirmButton");
        if (confirm != null) {
            Object disabled = field(confirm.getClass(), confirm, "isDisabled");
            embarkEnabled[0] = !Boolean.TRUE.equals(disabled);
            if (characterSelected[0] && disabled == null) {
                embarkEnabled[0] = true;
            }
        }
    }

    static String describeCharacter(Object option) {
        try {
            Object c = field(option.getClass(), option, "c");
            if (c != null) {
                Object chosen = field(c.getClass(), c, "chosenClass");
                if (chosen != null) {
                    return String.valueOf(chosen);
                }
            }
            Object name = field(option.getClass(), option, "name");
            if (name != null) {
                return String.valueOf(name);
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    static void setField(Class<?> type, Object target, String name, Object value) throws Exception {
        Class<?> c = type;
        while (c != null) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    static Object field(Class<?> type, Object instance, String name) throws Exception {
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
}
