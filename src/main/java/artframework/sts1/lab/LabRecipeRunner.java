package artframework.sts1.lab;

import artframework.api.UiOpResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Advances L2 lab recipes one step per host tick (postUpdate). Console arms a job and returns
 * immediately so the GL/game thread is not blocked with sleep.
 */
public final class LabRecipeRunner {

    public enum Kind {
        IDLE,
        ENSURE_MENU,
        ENSURE_FRESH,
        START_RUN
    }

    private static Kind kind = Kind.IDLE;
    private static String characterId = "IRONCLAD";
    private static String seed;
    private static boolean seedDone;
    private static int ticksLeft;
    private static String lastStatus = "idle";
    private static String lastMessage = "";
    private static boolean clearsDone;
    private static boolean stripDone;
    private static boolean embarkDone;
    private static boolean abandonDone;

    private LabRecipeRunner() {}

    public static synchronized void resetForTests() {
        kind = Kind.IDLE;
        characterId = "IRONCLAD";
        seed = null;
        seedDone = false;
        ticksLeft = 0;
        lastStatus = "idle";
        lastMessage = "";
        clearsDone = false;
        stripDone = false;
        embarkDone = false;
        abandonDone = false;
    }

    public static synchronized boolean isBusy() {
        return kind != Kind.IDLE;
    }

    public static synchronized Map<String, Object> statusMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("kind", kind.name());
        m.put("busy", Boolean.valueOf(kind != Kind.IDLE));
        m.put("ticksLeft", Integer.valueOf(ticksLeft));
        m.put("status", lastStatus);
        m.put("message", lastMessage);
        m.put("characterId", characterId);
        if (seed != null) {
            m.put("seed", seed);
        }
        return m;
    }

    public static synchronized UiOpResult armEnsureMenu(int budget) {
        kind = Kind.ENSURE_MENU;
        ticksLeft = budget > 0 ? budget : StsLabRecipes.DEFAULT_BUDGET;
        lastStatus = "running";
        lastMessage = "ensure-menu armed";
        return UiOpResult.ok("ensure-menu armed ticks=" + ticksLeft);
    }

    public static synchronized UiOpResult armEnsureFresh(int budget) {
        kind = Kind.ENSURE_FRESH;
        ticksLeft = budget > 0 ? budget : StsLabRecipes.DEFAULT_BUDGET;
        clearsDone = false;
        stripDone = false;
        embarkDone = false;
        abandonDone = false;
        lastStatus = "running";
        lastMessage = "ensure-fresh-menu armed";
        return UiOpResult.ok("ensure-fresh-menu armed ticks=" + ticksLeft);
    }

    public static synchronized UiOpResult armStartRun(String charId, String seedText, int budget) {
        kind = Kind.START_RUN;
        characterId = charId != null && !charId.isEmpty() ? charId : "IRONCLAD";
        seed = seedText != null && !seedText.isEmpty() ? seedText : StsLabRecipes.DEFAULT_SEED;
        seedDone = false;
        ticksLeft = budget > 0 ? budget : StsLabRecipes.DEFAULT_BUDGET;
        clearsDone = false;
        stripDone = false;
        embarkDone = false;
        abandonDone = false;
        lastStatus = "running";
        lastMessage = "start-run armed char=" + characterId;
        return UiOpResult.ok("start-run armed char=" + characterId + " ticks=" + ticksLeft);
    }

    /** One game-frame step. Safe no-op when idle. */
    public static void tick() {
        Kind k;
        synchronized (LabRecipeRunner.class) {
            k = kind;
            if (k == Kind.IDLE) {
                return;
            }
            if (ticksLeft <= 0) {
                fail("timeout");
                return;
            }
            ticksLeft--;
        }
        try {
            switch (k) {
                case ENSURE_MENU:
                    stepEnsureMenu(false);
                    break;
                case ENSURE_FRESH:
                    stepEnsureMenu(true);
                    break;
                case START_RUN:
                    stepStartRun();
                    break;
                default:
                    break;
            }
        } catch (Throwable t) {
            fail(t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
        }
    }

    private static void stepEnsureMenu(boolean fresh) {
        LabHost host = StsLabNav.host();
        LabStateSnapshot s = host.dump();
        if ("MAIN_MENU".equals(s.menuScreen) && !s.fading) {
            if (!fresh) {
                succeed("on main menu");
                return;
            }
            finishFresh(host, s);
            return;
        }
        // MainMenuScreen can retain CHAR_SELECT/fade fields after a usable dungeon begins.
        // A fresh-menu request must abandon that run instead of waiting on stale menu state.
        if (s.inGame || s.hasAbandon) {
            synchronized (LabRecipeRunner.class) {
                if (abandonDone) {
                    return;
                }
                abandonDone = true;
            }
            host.abandon();
            return;
        }
        if (s.fading) {
            return;
        }
        if (s.onCharSelect() && !fresh) {
            succeed("on char select");
            return;
        }
        if (s.onCharSelect() && fresh) {
            finishFresh(host, s);
            return;
        }
        if (s.abandonConfirmOpen) {
            host.abandonConfirm();
            return;
        }
        if (s.endScreen) {
            host.returnToMenu();
            return;
        }
    }

    private static void finishFresh(LabHost host, LabStateSnapshot s) {
        synchronized (LabRecipeRunner.class) {
            if (!clearsDone) {
                clearsDone = true;
                host.clearSaves();
                return;
            }
            if (!stripDone && (s.onMainMenu() || s.hasPlay || !s.buttons.isEmpty())) {
                stripDone = true;
                host.stripResumeButtons();
                return;
            }
            LabStateSnapshot after = host.dump();
            if (after.hasResume) {
                fail("resume still present after strip");
                return;
            }
            succeed("fresh menu");
        }
    }

    private static void stepStartRun() {
        LabHost host = StsLabNav.host();
        LabStateSnapshot s = host.dump();
        boolean didEmbark;
        synchronized (LabRecipeRunner.class) {
            didEmbark = embarkDone;
        }
        if (s.isRunReady()) {
            if (didEmbark) {
                synchronized (LabRecipeRunner.class) {
                    if (!seedDone) {
                        seedDone = true;
                        host.setSeed(seed);
                        return;
                    }
                }
                succeed("embark ready char=" + s.selectedCharacter + " seed=" + seed);
                return;
            }
            host.abandon();
            return;
        }
        if (s.inGame && didEmbark) {
            synchronized (LabRecipeRunner.class) {
                if (!seedDone) {
                    seedDone = true;
                    host.setSeed(seed);
                }
            }
            return;
        }
        if (s.inGame && !s.charSelectOpen) {
            // Player assignment precedes a stable dungeon frame during embark. Waiting here keeps
            // the recipe from issuing map/proceed commands against the lingering character select.
            return;
        }
        if (s.fading && !s.charSelectOpen) {
            return;
        }
        // Fresh prep first when still on main menu with resume, or never cleared this job.
        synchronized (LabRecipeRunner.class) {
            if (!clearsDone && (s.onMainMenu() || s.hasResume || s.hasAbandon)) {
                clearsDone = true;
                host.clearSaves();
                return;
            }
            if (!stripDone && s.onMainMenu()) {
                stripDone = true;
                host.stripResumeButtons();
                return;
            }
        }
        if (s.abandonConfirmOpen || s.endScreen || (s.hasAbandon && !s.onMainMenu())) {
            stepEnsureMenu(true);
            return;
        }
        if (s.charSelectOpen && s.inGame && s.isRunReady() && !s.embarkEnabled) {
            host.openCharSelect();
            return;
        }
        if (!s.charSelectOpen && !s.onCharSelect()) {
            host.openCharSelect();
            return;
        }
        String want;
        synchronized (LabRecipeRunner.class) {
            want = characterId;
        }
        if (!s.characterSelected
                || (s.charSelectOpen && !s.embarkEnabled)
                || (s.selectedCharacter != null
                        && !s.selectedCharacter.isEmpty()
                        && !s.selectedCharacter.equalsIgnoreCase(want))) {
            host.selectCharacter(want);
            return;
        }
        if (!s.embarkEnabled && s.characterSelected) {
            synchronized (LabRecipeRunner.class) {
                embarkDone = true;
            }
            host.embark();
            return;
        }
        synchronized (LabRecipeRunner.class) {
            embarkDone = true;
        }
        host.embark();
    }

    private static synchronized void succeed(String message) {
        kind = Kind.IDLE;
        lastStatus = "ok";
        lastMessage = message != null ? message : "";
        ticksLeft = 0;
    }

    private static synchronized void fail(String message) {
        kind = Kind.IDLE;
        lastStatus = "unavailable";
        lastMessage = message != null ? message : "";
        ticksLeft = 0;
    }
}
