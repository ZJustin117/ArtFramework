package artframework.sts1.lab;

import artframework.api.UiOpResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Mutable in-memory lab host for pure recipe tests. */
public final class FakeLabHost implements LabHost {

    public final List<String> actions = new ArrayList<String>();
    private LabStateSnapshot state;
    private int clearCount;
    private boolean failChar;
    private boolean failAbandon;

    public FakeLabHost(LabStateSnapshot initial) {
        this.state = initial != null ? initial : LabStateSnapshot.builder().mode("CHAR_SELECT").menuScreen("MAIN_MENU").hasPlay(true).build();
    }

    public FakeLabHost() {
        this(null);
    }

    public LabStateSnapshot state() {
        return state;
    }

    public void setState(LabStateSnapshot s) {
        state = s;
    }

    public FakeLabHost failCharacterSelect(boolean v) {
        failChar = v;
        return this;
    }

    public FakeLabHost failAbandon(boolean v) {
        failAbandon = v;
        return this;
    }

    public int clearCount() {
        return clearCount;
    }

    @Override
    public LabStateSnapshot dump() {
        return state;
    }

    @Override
    public UiOpResult clearSaves() {
        actions.add("clear-saves");
        clearCount++;
        state =
                LabStateSnapshot.builder()
                        .mode(state.mode)
                        .menuScreen(state.menuScreen.isEmpty() ? "MAIN_MENU" : state.menuScreen)
                        .inGame(false)
                        .hasPlay(true)
                        .hasResume(false)
                        .hasAbandon(false)
                        .buttons(Arrays.asList("PLAY", "OPTIONS", "QUIT"))
                        .build();
        return UiOpResult.ok("cleared");
    }

    @Override
    public UiOpResult stripResumeButtons() {
        actions.add("strip-resume");
        List<String> buttons = new ArrayList<String>();
        buttons.add("PLAY");
        for (String b : state.buttons) {
            if (!"RESUME_GAME".equals(b) && !"ABANDON_RUN".equals(b) && !"PLAY".equals(b)) {
                buttons.add(b);
            }
        }
        state =
                LabStateSnapshot.builder()
                        .mode(state.mode)
                        .menuScreen("MAIN_MENU")
                        .inGame(false)
                        .hasPlay(true)
                        .hasResume(false)
                        .hasAbandon(false)
                        .buttons(buttons)
                        .build();
        return UiOpResult.ok("stripped");
    }

    @Override
    public UiOpResult openCharSelect() {
        actions.add("open-char-select");
        if (state.inGame) {
            return UiOpResult.unavailable("in game");
        }
        state =
                LabStateSnapshot.builder()
                        .mode("CHAR_SELECT")
                        .menuScreen("CHAR_SELECT")
                        .charSelectOpen(true)
                        .characters(Arrays.asList("IRONCLAD", "THE_SILENT", "DEFECT", "WATCHER"))
                        .hasPlay(true)
                        .build();
        return UiOpResult.ok("char select open");
    }

    @Override
    public UiOpResult selectCharacter(String characterId) {
        actions.add("char:" + characterId);
        if (failChar) {
            return UiOpResult.unavailable("character not found");
        }
        if (!state.charSelectOpen && !state.onCharSelect()) {
            return UiOpResult.unavailable("not on char select");
        }
        String id = characterId != null ? characterId : "";
        boolean found = false;
        for (String c : state.characters) {
            if (c.equalsIgnoreCase(id)) {
                found = true;
                id = c;
                break;
            }
        }
        if (!found && !state.characters.isEmpty()) {
            return UiOpResult.unavailable("character not found: " + characterId);
        }
        if (state.characters.isEmpty()) {
            id = characterId != null ? characterId.toUpperCase() : "IRONCLAD";
        }
        state =
                LabStateSnapshot.builder()
                        .mode(state.mode)
                        .menuScreen("CHAR_SELECT")
                        .charSelectOpen(true)
                        .characterSelected(true)
                        .embarkEnabled(true)
                        .selectedCharacter(id)
                        .characters(state.characters)
                        .build();
        return UiOpResult.ok("selected " + id);
    }

    @Override
    public UiOpResult embark() {
        actions.add("embark");
        if (!state.characterSelected) {
            return UiOpResult.unavailable("no character selected");
        }
        if (!state.embarkEnabled) {
            return UiOpResult.unavailable("embark disabled");
        }
        state =
                LabStateSnapshot.builder()
                        .mode("GAMEPLAY")
                        .menuScreen("")
                        .inGame(true)
                        .inCombat(false)
                        .roomPhase("INCOMPLETE")
                        .selectedCharacter(state.selectedCharacter)
                        .build();
        return UiOpResult.ok("embarked");
    }

    @Override
    public UiOpResult setSeed(String seedText) {
        actions.add("seed:" + (seedText != null ? seedText : ""));
        if (seedText == null || seedText.isEmpty()) {
            return UiOpResult.ok("seed skipped");
        }
        return UiOpResult.ok("seed set " + seedText);
    }

    @Override
    public UiOpResult menuClick(String clickResult) {
        actions.add("menu-click:" + clickResult);
        if ("ABANDON_RUN".equals(clickResult)) {
            state =
                    LabStateSnapshot.builder()
                            .mode(state.mode)
                            .menuScreen("MAIN_MENU")
                            .abandonConfirmOpen(true)
                            .hasAbandon(true)
                            .hasPlay(state.hasPlay)
                            .buttons(state.buttons)
                            .build();
            return UiOpResult.ok("abandon clicked");
        }
        if ("PLAY".equals(clickResult)) {
            return openCharSelect();
        }
        if ("RESUME_GAME".equals(clickResult)) {
            state =
                    LabStateSnapshot.builder()
                            .mode("GAMEPLAY")
                            .inGame(true)
                            .build();
            return UiOpResult.ok("resumed");
        }
        return UiOpResult.ok("menu-click " + clickResult);
    }

    @Override
    public UiOpResult abandon() {
        actions.add("abandon");
        if (failAbandon) {
            return UiOpResult.unavailable("abandon failed");
        }
        if (!state.inGame && !state.hasAbandon && !state.abandonConfirmOpen) {
            if (state.onMainMenu()) {
                return UiOpResult.ok("already menu");
            }
            return UiOpResult.unavailable("cannot abandon");
        }
        if (state.abandonConfirmOpen) {
            return abandonConfirm();
        }
        if (state.inGame) {
            state =
                    LabStateSnapshot.builder()
                            .mode("CHAR_SELECT")
                            .menuScreen("MAIN_MENU")
                            .inGame(false)
                            .hasAbandon(true)
                            .abandonConfirmOpen(true)
                            .hasPlay(true)
                            .buttons(Arrays.asList("ABANDON_RUN", "PLAY"))
                            .build();
            return UiOpResult.ok("abandon scheduled");
        }
        state =
                LabStateSnapshot.builder()
                        .mode(state.mode)
                        .menuScreen("MAIN_MENU")
                        .abandonConfirmOpen(true)
                        .hasAbandon(true)
                        .hasPlay(true)
                        .buttons(state.buttons)
                        .build();
        return UiOpResult.ok("abandon scheduled");
    }

    @Override
    public UiOpResult abandonConfirm() {
        actions.add("abandon-confirm");
        state =
                LabStateSnapshot.builder()
                        .mode("CHAR_SELECT")
                        .menuScreen("MAIN_MENU")
                        .inGame(false)
                        .hasPlay(true)
                        .hasResume(false)
                        .hasAbandon(false)
                        .abandonConfirmOpen(false)
                        .buttons(Arrays.asList("PLAY", "OPTIONS", "QUIT"))
                        .build();
        return UiOpResult.ok("abandoned");
    }

    @Override
    public UiOpResult returnToMenu() {
        actions.add("return-menu");
        if (!state.endScreen && state.inGame) {
            return UiOpResult.unavailable("not on end screen");
        }
        state =
                LabStateSnapshot.builder()
                        .mode("CHAR_SELECT")
                        .menuScreen("MAIN_MENU")
                        .inGame(false)
                        .hasPlay(true)
                        .buttons(Arrays.asList("PLAY"))
                        .build();
        return UiOpResult.ok("returned");
    }

    @Override
    public UiOpResult proceed() {
        actions.add("proceed");
        if (!state.inGame) {
            return UiOpResult.unavailable("not in game");
        }
        state =
                LabStateSnapshot.builder()
                        .mode("GAMEPLAY")
                        .inGame(true)
                        .inCombat(true)
                        .roomPhase("COMBAT")
                        .selectedCharacter(state.selectedCharacter)
                        .build();
        return UiOpResult.ok("proceed");
    }

    @Override
    public void yieldFrame() {
        // Instant state transitions in tests — no wait.
    }
}
