package artframework.sts1.lab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable lab navigation snapshot (no STS types). */
public final class LabStateSnapshot {

    public final String mode;
    public final String menuScreen;
    public final boolean inGame;
    public final boolean inCombat;
    public final boolean fading;
    public final boolean hasResume;
    public final boolean hasAbandon;
    public final boolean hasPlay;
    public final boolean charSelectOpen;
    public final boolean characterSelected;
    public final boolean embarkEnabled;
    public final boolean seedPanelOpen;
    public final boolean endScreen;
    public final boolean abandonConfirmOpen;
    public final String roomPhase;
    public final String selectedCharacter;
    public final List<String> buttons;
    public final List<String> characters;
    public final String message;

    public LabStateSnapshot(
            String mode,
            String menuScreen,
            boolean inGame,
            boolean inCombat,
            boolean fading,
            boolean hasResume,
            boolean hasAbandon,
            boolean hasPlay,
            boolean charSelectOpen,
            boolean characterSelected,
            boolean embarkEnabled,
            boolean seedPanelOpen,
            boolean endScreen,
            boolean abandonConfirmOpen,
            String roomPhase,
            String selectedCharacter,
            List<String> buttons,
            List<String> characters,
            String message) {
        this.mode = mode != null ? mode : "";
        this.menuScreen = menuScreen != null ? menuScreen : "";
        this.inGame = inGame;
        this.inCombat = inCombat;
        this.fading = fading;
        this.hasResume = hasResume;
        this.hasAbandon = hasAbandon;
        this.hasPlay = hasPlay;
        this.charSelectOpen = charSelectOpen;
        this.characterSelected = characterSelected;
        this.embarkEnabled = embarkEnabled;
        this.seedPanelOpen = seedPanelOpen;
        this.endScreen = endScreen;
        this.abandonConfirmOpen = abandonConfirmOpen;
        this.roomPhase = roomPhase != null ? roomPhase : "";
        this.selectedCharacter = selectedCharacter != null ? selectedCharacter : "";
        this.buttons =
                buttons == null
                        ? Collections.<String>emptyList()
                        : Collections.unmodifiableList(new ArrayList<String>(buttons));
        this.characters =
                characters == null
                        ? Collections.<String>emptyList()
                        : Collections.unmodifiableList(new ArrayList<String>(characters));
        this.message = message != null ? message : "";
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean onMainMenu() {
        return !inGame && "MAIN_MENU".equals(menuScreen) && !fading;
    }

    public boolean onCharSelect() {
        return !inGame && ("CHAR_SELECT".equals(menuScreen) || charSelectOpen) && !fading;
    }

    /** True once STS has initialized a current dungeon room. */
    public boolean isRunReady() {
        // MainMenuScreen retains CHAR_SELECT/fade fields after embark. The current-room phase is
        // the authoritative boundary: it is absent until AbstractDungeon has a usable room.
        return inGame && !roomPhase.isEmpty();
    }

    /** True while STS exposes a player but has not initialized a current dungeon room. */
    public boolean isEmbarkTransition() {
        return inGame
                && !isRunReady()
                && (fading || charSelectOpen || "CHAR_SELECT".equals(menuScreen));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("mode", mode);
        m.put("menuScreen", menuScreen);
        m.put("inGame", Boolean.valueOf(inGame));
        m.put("runReady", Boolean.valueOf(isRunReady()));
        m.put("inCombat", Boolean.valueOf(inCombat));
        m.put("fading", Boolean.valueOf(fading));
        m.put("hasResume", Boolean.valueOf(hasResume));
        m.put("hasAbandon", Boolean.valueOf(hasAbandon));
        m.put("hasPlay", Boolean.valueOf(hasPlay));
        m.put("charSelectOpen", Boolean.valueOf(charSelectOpen));
        m.put("characterSelected", Boolean.valueOf(characterSelected));
        m.put("embarkEnabled", Boolean.valueOf(embarkEnabled));
        m.put("seedPanelOpen", Boolean.valueOf(seedPanelOpen));
        m.put("endScreen", Boolean.valueOf(endScreen));
        m.put("abandonConfirmOpen", Boolean.valueOf(abandonConfirmOpen));
        m.put("roomPhase", roomPhase);
        m.put("selectedCharacter", selectedCharacter);
        m.put("buttons", new ArrayList<String>(buttons));
        m.put("characters", new ArrayList<String>(characters));
        if (!message.isEmpty()) {
            m.put("message", message);
        }
        return m;
    }

    public static final class Builder {
        private String mode = "";
        private String menuScreen = "";
        private boolean inGame;
        private boolean inCombat;
        private boolean fading;
        private boolean hasResume;
        private boolean hasAbandon;
        private boolean hasPlay;
        private boolean charSelectOpen;
        private boolean characterSelected;
        private boolean embarkEnabled;
        private boolean seedPanelOpen;
        private boolean endScreen;
        private boolean abandonConfirmOpen;
        private String roomPhase = "";
        private String selectedCharacter = "";
        private List<String> buttons = new ArrayList<String>();
        private List<String> characters = new ArrayList<String>();
        private String message = "";

        public Builder mode(String v) {
            mode = v;
            return this;
        }

        public Builder menuScreen(String v) {
            menuScreen = v;
            return this;
        }

        public Builder inGame(boolean v) {
            inGame = v;
            return this;
        }

        public Builder inCombat(boolean v) {
            inCombat = v;
            return this;
        }

        public Builder fading(boolean v) {
            fading = v;
            return this;
        }

        public Builder hasResume(boolean v) {
            hasResume = v;
            return this;
        }

        public Builder hasAbandon(boolean v) {
            hasAbandon = v;
            return this;
        }

        public Builder hasPlay(boolean v) {
            hasPlay = v;
            return this;
        }

        public Builder charSelectOpen(boolean v) {
            charSelectOpen = v;
            return this;
        }

        public Builder characterSelected(boolean v) {
            characterSelected = v;
            return this;
        }

        public Builder embarkEnabled(boolean v) {
            embarkEnabled = v;
            return this;
        }

        public Builder seedPanelOpen(boolean v) {
            seedPanelOpen = v;
            return this;
        }

        public Builder endScreen(boolean v) {
            endScreen = v;
            return this;
        }

        public Builder abandonConfirmOpen(boolean v) {
            abandonConfirmOpen = v;
            return this;
        }

        public Builder roomPhase(String v) {
            roomPhase = v;
            return this;
        }

        public Builder selectedCharacter(String v) {
            selectedCharacter = v;
            return this;
        }

        public Builder buttons(List<String> v) {
            buttons = v != null ? new ArrayList<String>(v) : new ArrayList<String>();
            return this;
        }

        public Builder characters(List<String> v) {
            characters = v != null ? new ArrayList<String>(v) : new ArrayList<String>();
            return this;
        }

        public Builder message(String v) {
            message = v;
            return this;
        }

        public LabStateSnapshot build() {
            return new LabStateSnapshot(
                    mode,
                    menuScreen,
                    inGame,
                    inCombat,
                    fading,
                    hasResume,
                    hasAbandon,
                    hasPlay,
                    charSelectOpen,
                    characterSelected,
                    embarkEnabled,
                    seedPanelOpen,
                    endScreen,
                    abandonConfirmOpen,
                    roomPhase,
                    selectedCharacter,
                    buttons,
                    characters,
                    message);
        }
    }
}
