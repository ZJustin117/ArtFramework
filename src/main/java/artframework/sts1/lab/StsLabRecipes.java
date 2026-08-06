package artframework.sts1.lab;

import artframework.api.UiOpResult;

/**
 * L2 lab recipes: branch on {@link LabStateSnapshot} and call {@link LabHost} steps.
 * Pure relative to host; production host is STS-backed.
 */
public final class StsLabRecipes {

    /** Ticks per synchronous recipe; STS host yields ~350ms/tick so keep moderate. */
    public static final int DEFAULT_BUDGET = 40;

    /** Ticks per async recipe; advanced once per frame on device. */
    public static final int ASYNC_DEFAULT_BUDGET = 3600;

    public static final String LOG_PREFIX = "ART_LAB ";

    private final LabHost host;
    private final int budget;

    public StsLabRecipes(LabHost host) {
        this(host, DEFAULT_BUDGET);
    }

    public StsLabRecipes(LabHost host, int budget) {
        if (host == null) {
            throw new IllegalArgumentException("host required");
        }
        this.host = host;
        this.budget = budget > 0 ? budget : DEFAULT_BUDGET;
    }

    public LabHost host() {
        return host;
    }

    public UiOpResult ensureMenu() {
        for (int i = 0; i < budget; i++) {
            LabStateSnapshot s = host.dump();
            if (s.onMainMenu()) {
                return UiOpResult.ok("on main menu");
            }
            if (s.fading) {
                host.yieldFrame();
                continue;
            }
            if (s.isEmbarkTransition()) {
                // A player can be initialized while character select is still fading out.
                host.yieldFrame();
                continue;
            }
            if (s.abandonConfirmOpen) {
                UiOpResult r = host.abandonConfirm();
                if (!r.isOk()) {
                    return r;
                }
                host.yieldFrame();
                continue;
            }
            if (s.endScreen) {
                UiOpResult r = host.returnToMenu();
                if (!r.isOk()) {
                    return r;
                }
                host.yieldFrame();
                continue;
            }
            if (s.inGame || s.hasAbandon) {
                UiOpResult r = host.abandon();
                if (!r.isOk()) {
                    return r;
                }
                host.yieldFrame();
                continue;
            }
            if (s.onCharSelect()) {
                // Not main-menu root; start-run / ensure-fresh handle this.
                return UiOpResult.ok("on char select");
            }
            host.yieldFrame();
        }
        LabStateSnapshot last = host.dump();
        if (last.onMainMenu() || last.onCharSelect()) {
            return UiOpResult.ok(last.onMainMenu() ? "on main menu" : "on char select");
        }
        return UiOpResult.unavailable(
                "ensure-menu timeout menuScreen=" + last.menuScreen + " inGame=" + last.inGame);
    }

    public UiOpResult ensureFreshMenu() {
        UiOpResult menu = ensureMenu();
        if (!menu.isOk()) {
            return menu;
        }
        LabStateSnapshot s = host.dump();
        UiOpResult clear = host.clearSaves();
        if (!clear.isOk()) {
            return clear;
        }
        if (s.onMainMenu() || (!s.buttons.isEmpty() && s.hasPlay)) {
            UiOpResult strip = host.stripResumeButtons();
            if (!strip.isOk()) {
                return strip;
            }
            LabStateSnapshot after = host.dump();
            if (after.hasResume) {
                return UiOpResult.unavailable("resume still present after strip");
            }
        }
        return UiOpResult.ok("fresh menu");
    }

    public UiOpResult startRun(String characterId) {
        return startRun(characterId, null);
    }

    public UiOpResult startRun(String characterId, String seed) {
        String charId =
                characterId != null && !characterId.isEmpty() ? characterId : "IRONCLAD";
        UiOpResult fresh = ensureFreshMenu();
        if (!fresh.isOk()) {
            return fresh;
        }
        for (int i = 0; i < budget; i++) {
            LabStateSnapshot s = host.dump();
            if (s.isRunReady()) {
                return UiOpResult.ok("in game char=" + s.selectedCharacter);
            }
            if (s.inGame) {
                // AbstractDungeon.player can be assigned before the character-select fade ends.
                // Do not treat that transient state as a usable dungeon run.
                host.yieldFrame();
                continue;
            }
            if (s.fading) {
                host.yieldFrame();
                continue;
            }
            if (!s.charSelectOpen && !s.onCharSelect()) {
                UiOpResult open = host.openCharSelect();
                if (!open.isOk()) {
                    return open;
                }
                host.yieldFrame();
                continue;
            }
            if (!s.characterSelected
                    || (charId != null
                            && !charId.isEmpty()
                            && s.selectedCharacter != null
                            && !s.selectedCharacter.isEmpty()
                            && !s.selectedCharacter.equalsIgnoreCase(charId))) {
                UiOpResult sel = host.selectCharacter(charId);
                if (!sel.isOk()) {
                    return sel;
                }
                host.yieldFrame();
                continue;
            }
            if (seed != null && !seed.isEmpty()) {
                UiOpResult seedResult = host.setSeed(seed);
                if (!seedResult.isOk()) {
                    return seedResult;
                }
                seed = null;
                host.yieldFrame();
                continue;
            }
            if (!s.embarkEnabled && s.characterSelected) {
                host.yieldFrame();
                continue;
            }
            UiOpResult emb = host.embark();
            if (!emb.isOk()) {
                return emb;
            }
            host.yieldFrame();
        }
        LabStateSnapshot last = host.dump();
        if (last.isRunReady()) {
            return UiOpResult.ok("in game");
        }
        return UiOpResult.unavailable(
                "start-run timeout inGame=" + last.inGame + " selected=" + last.selectedCharacter);
    }

    public UiOpResult reset() {
        return ensureFreshMenu();
    }
}
