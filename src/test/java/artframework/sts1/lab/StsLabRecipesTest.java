package artframework.sts1.lab;

import artframework.api.UiOpResult;
import artframework.core.SignalBuses;
import artframework.core.SignalDecision;
import artframework.core.SignalListener;
import artframework.core.UiSignal;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StsLabRecipesTest {

    @After
    public void tearDown() {
        StsLabNav.resetForTests();
        SignalBuses.resetForTests();
    }

    @Test
    public void ensureMenuFromGameplayAbandons() {
        FakeLabHost host =
                new FakeLabHost(
                        LabStateSnapshot.builder()
                                .mode("GAMEPLAY")
                                .inGame(true)
                                .build());
        StsLabRecipes recipes = new StsLabRecipes(host, 8);
        UiOpResult r = recipes.ensureMenu();
        assertTrue(r.isOk());
        assertTrue(host.dump().onMainMenu() || !host.dump().inGame);
        assertTrue(host.actions.contains("abandon") || host.actions.contains("abandon-confirm"));
    }

    @Test
    public void ensureFreshMenuClearsAndStrips() {
        FakeLabHost host =
                new FakeLabHost(
                        LabStateSnapshot.builder()
                                .mode("CHAR_SELECT")
                                .menuScreen("MAIN_MENU")
                                .hasPlay(true)
                                .hasResume(true)
                                .hasAbandon(true)
                                .buttons(Arrays.asList("RESUME_GAME", "ABANDON_RUN", "OPTIONS"))
                                .build());
        StsLabRecipes recipes = new StsLabRecipes(host, 8);
        UiOpResult r = recipes.ensureFreshMenu();
        assertTrue(r.message, r.isOk());
        assertTrue(host.actions.contains("clear-saves"));
        assertTrue(host.actions.contains("strip-resume"));
        assertFalse(host.dump().hasResume);
        assertTrue(host.dump().hasPlay);
    }

    @Test
    public void startRunOpensSelectsEmbarks() {
        FakeLabHost host =
                new FakeLabHost(
                        LabStateSnapshot.builder()
                                .mode("CHAR_SELECT")
                                .menuScreen("MAIN_MENU")
                                .hasPlay(true)
                                .buttons(Arrays.asList("PLAY"))
                                .build());
        StsLabRecipes recipes = new StsLabRecipes(host, 12);
        UiOpResult r = recipes.startRun("IRONCLAD", "ABC12");
        assertTrue(r.message, r.isOk());
        assertTrue(host.dump().inGame);
        assertTrue(host.actions.contains("open-char-select"));
        assertTrue(host.actions.contains("char:IRONCLAD"));
        assertTrue(host.actions.contains("seed:ABC12"));
        assertTrue(host.actions.contains("embark"));
        assertEquals("IRONCLAD", host.dump().selectedCharacter);
    }

    @Test
    public void startRunUsesDefaultSeedWhenNoneIsProvided() {
        FakeLabHost host =
                new FakeLabHost(
                        LabStateSnapshot.builder()
                                .mode("CHAR_SELECT")
                                .menuScreen("MAIN_MENU")
                                .hasPlay(true)
                                .buttons(Arrays.asList("PLAY"))
                                .build());
        StsLabRecipes recipes = new StsLabRecipes(host, 12);

        UiOpResult result = recipes.startRun("IRONCLAD");

        assertTrue(result.message, result.isOk());
        assertTrue(host.actions.contains("seed:" + StsLabRecipes.DEFAULT_SEED));
    }

    @Test
    public void startRunWaitsForPlayerBeforeDungeonRoomExists() {
        FakeLabHost host =
                new FakeLabHost(
                        LabStateSnapshot.builder()
                                .mode("GAMEPLAY")
                                .menuScreen("CHAR_SELECT")
                                .inGame(true)
                                .fading(true)
                                .charSelectOpen(false)
                                .selectedCharacter("IRONCLAD")
                                .build());
        StsLabRecipes recipes = new StsLabRecipes(host, 2);
        UiOpResult r = recipes.startRun("IRONCLAD", null);
        assertFalse(r.isOk());
        assertFalse(host.actions.contains("abandon"));
        assertFalse(host.actions.contains("proceed"));
    }

    @Test
    public void startRunFailsMissingCharacter() {
        FakeLabHost host =
                new FakeLabHost(
                        LabStateSnapshot.builder()
                                .mode("CHAR_SELECT")
                                .menuScreen("MAIN_MENU")
                                .hasPlay(true)
                                .buttons(Arrays.asList("PLAY"))
                                .build());
        host.failCharacterSelect(true);
        StsLabRecipes recipes = new StsLabRecipes(host, 6);
        UiOpResult r = recipes.startRun("NOPE", null);
        assertFalse(r.isOk());
        assertEquals(UiOpResult.Status.UNAVAILABLE, r.status);
    }

    @Test
    public void navInstallUsesFakeForDump() {
        FakeLabHost host =
                new FakeLabHost(
                        LabStateSnapshot.builder()
                                .mode("GAMEPLAY")
                                .inGame(true)
                                .inCombat(true)
                                .roomPhase("COMBAT")
                                .build());
        StsLabNav.install(host);
        assertTrue(StsLabNav.dump().inCombat);
        assertTrue(StsLabNav.ensureMenu().isOk());
    }

    @Test
    public void snapshotToMapHasCoreKeys() {
        LabStateSnapshot s =
                LabStateSnapshot.builder()
                        .mode("GAMEPLAY")
                        .menuScreen("MAIN_MENU")
                        .inGame(false)
                        .hasPlay(true)
                        .buttons(Arrays.asList("PLAY"))
                        .build();
        assertEquals("GAMEPLAY", s.toMap().get("mode"));
        assertEquals(Boolean.TRUE, s.toMap().get("hasPlay"));
        assertTrue(s.onMainMenu());
    }

    @Test
    public void runReadyRequiresDungeonRoomEvenWhenPlayerExists() {
        LabStateSnapshot transition =
                LabStateSnapshot.builder()
                                .mode("GAMEPLAY")
                                .menuScreen("CHAR_SELECT")
                                .inGame(true)
                                .fading(true)
                                .charSelectOpen(false)
                        .build();
        assertFalse(transition.isRunReady());
        assertTrue(transition.isEmbarkTransition());
        assertEquals(Boolean.FALSE, transition.toMap().get("runReady"));
        assertTrue(
                LabStateSnapshot.builder()
                        .mode("GAMEPLAY")
                        .menuScreen("CHAR_SELECT")
                        .inGame(true)
                        .fading(true)
                        .charSelectOpen(true)
                        .roomPhase("EVENT")
                        .build()
                        .isRunReady());
    }

    @Test
    public void endScreenReturnsToMenu() {
        FakeLabHost host =
                new FakeLabHost(
                        LabStateSnapshot.builder()
                                .mode("GAMEPLAY")
                                .inGame(true)
                                .endScreen(true)
                                .build());
        StsLabRecipes recipes = new StsLabRecipes(host, 4);
        assertTrue(recipes.ensureMenu().isOk());
        assertTrue(host.actions.contains("return-menu"));
        assertTrue(host.dump().onMainMenu());
    }

    @Test
    public void eventAliasNormalizesOnlySupportedVanillaIds() {
        assertEquals("The Cleric", LabEventIds.normalize("The_Cleric"));
        assertEquals("The Cleric", LabEventIds.normalize(" the cleric "));
        assertTrue(LabEventIds.isSupported("Golden_Shrine"));
        assertFalse(LabEventIds.isSupported("WorldofGoop"));
        assertEquals("", LabEventIds.normalize("unknown"));
    }

    @Test
    public void fakeHostRejectsEventWhenNotInRun() {
        FakeLabHost host =
                new FakeLabHost(
                        LabStateSnapshot.builder()
                                .mode("GAMEPLAY")
                                .inGame(false)
                                .roomPhase("COMBAT")
                                .build());
        UiOpResult result = host.enterEvent("WorldofGoop");
        assertFalse(result.isOk());
        assertEquals("COMBAT", host.dump().roomPhase);
    }

    @Test
    public void fakeHostQueuesEventNavigationWithoutMutatingState() {
        SignalBuses.get()
                .connect(
                        LabNavigationSignals.REQUEST,
                        new SignalListener() {
                            @Override
                            public SignalDecision onSignal(UiSignal signal) {
                                assertTrue(signal.payload instanceof LabNavigationIntent);
                                LabNavigationIntent intent = (LabNavigationIntent) signal.payload;
                                assertEquals(LabIntentNames.ENTER_EVENT_ROOM, intent.name);
                                return SignalDecision.stopHandled("queued native event navigation");
                            }
                        });
        FakeLabHost host =
                new FakeLabHost(
                        LabStateSnapshot.builder()
                                .mode("GAMEPLAY")
                                .inGame(true)
                                .roomPhase("COMPLETE")
                                .selectedCharacter("IRONCLAD")
                                .build());
        UiOpResult result = host.enterEvent("");
        assertTrue(result.message, result.isOk());
        assertEquals("COMPLETE", host.dump().roomPhase);
        assertTrue(host.actions.contains("event:"));
    }

    @Test
    public void labNavigationDispatchFailsWithoutNativeNavigator() {
        UiOpResult result =
                LabNavigationSignals.dispatch(
                        LabNavigationIntent.of(LabIntentNames.ENTER_EVENT_ROOM));
        assertFalse(result.isOk());
        assertTrue(result.message.contains("no lab navigator"));
    }

    @Test
    public void labProbeCanExposeRecipeStatus() {
        StsLabNav.install(new FakeLabHost());
        assertTrue(LabRecipeRunner.armEnsureFresh(4).isOk());
        assertEquals(Boolean.TRUE, LabRecipeRunner.statusMap().get("busy"));
    }

    @Test
    public void asyncRunnerStartRunCompletesWithFakeHost() {
        FakeLabHost host =
                new FakeLabHost(
                        LabStateSnapshot.builder()
                                .mode("CHAR_SELECT")
                                .menuScreen("MAIN_MENU")
                                .hasPlay(true)
                                .buttons(Arrays.asList("PLAY"))
                                .build());
        StsLabNav.install(host);
        assertTrue(LabRecipeRunner.armStartRun("IRONCLAD", null, 20).isOk());
        for (int i = 0; i < 20 && LabRecipeRunner.isBusy(); i++) {
            LabRecipeRunner.tick();
        }
        assertFalse(LabRecipeRunner.isBusy());
        assertEquals("ok", LabRecipeRunner.statusMap().get("status"));
        assertTrue(host.dump().inGame);
        assertTrue(host.actions.contains("seed:" + StsLabRecipes.DEFAULT_SEED));
    }

    @Test
    public void asyncStartRunUsesFrameBudgetForDeviceTransitions() {
        StsLabNav.install(new FakeLabHost());
        assertTrue(StsLabNav.armStartRun("IRONCLAD", null).isOk());
        assertEquals(
                Integer.valueOf(StsLabRecipes.ASYNC_DEFAULT_BUDGET),
                LabRecipeRunner.statusMap().get("ticksLeft"));
    }

    @Test
    public void asyncRunnerStartRunRecoversDirtyResumeMenuWithoutPlay() {
        FakeLabHost host =
                new FakeLabHost(
                        LabStateSnapshot.builder()
                                .mode("CHAR_SELECT")
                                .menuScreen("MAIN_MENU")
                                .hasResume(true)
                                .hasAbandon(true)
                                .buttons(Arrays.asList("QUIT", "ABANDON_RUN", "RESUME_GAME"))
                                .build());
        StsLabNav.install(host);
        assertTrue(LabRecipeRunner.armStartRun("IRONCLAD", null, 20).isOk());
        for (int i = 0; i < 20 && LabRecipeRunner.isBusy(); i++) {
            LabRecipeRunner.tick();
        }
        assertEquals("ok", LabRecipeRunner.statusMap().get("status"));
        assertTrue(host.actions.contains("clear-saves"));
        assertTrue(host.actions.contains("strip-resume"));
        assertTrue(host.dump().inGame);
    }

    @Test
    public void asyncRunnerStartRunBypassesStaleMainMenuResumeButtonsAfterCleanupAttempt() {
        FakeLabHost host =
                new FakeLabHost(
                                LabStateSnapshot.builder()
                                        .mode("CHAR_SELECT")
                                        .menuScreen("MAIN_MENU")
                                        .hasResume(true)
                                        .hasAbandon(true)
                                        .buttons(
                                                Arrays.asList(
                                                        "QUIT", "ABANDON_RUN", "RESUME_GAME"))
                                        .build())
                        .stickyDirtyMenu(true);
        StsLabNav.install(host);
        assertTrue(LabRecipeRunner.armStartRun("IRONCLAD", null, 20).isOk());
        for (int i = 0; i < 20 && LabRecipeRunner.isBusy(); i++) {
            LabRecipeRunner.tick();
        }
        assertEquals("ok", LabRecipeRunner.statusMap().get("status"));
        assertTrue(host.actions.contains("clear-saves"));
        assertTrue(host.actions.contains("strip-resume"));
        assertTrue(host.actions.contains("open-char-select"));
        assertTrue(host.dump().inGame);
    }

    @Test
    public void asyncRunnerStartRunRecoversStaleDungeonWhileCharSelectVisible() {
        FakeLabHost host =
                new FakeLabHost(
                        LabStateSnapshot.builder()
                                .mode("GAMEPLAY")
                                .menuScreen("CHAR_SELECT")
                                .inGame(true)
                                .inCombat(true)
                                .fading(true)
                                .charSelectOpen(true)
                                .characterSelected(true)
                                .embarkEnabled(false)
                                .selectedCharacter("IRONCLAD")
                                .roomPhase("COMBAT")
                                .characters(Arrays.asList("IRONCLAD", "THE_SILENT"))
                                .hasPlay(true)
                                .buttons(Arrays.asList("PLAY"))
                                .build());
        StsLabNav.install(host);
        assertTrue(LabRecipeRunner.armStartRun("IRONCLAD", null, 20).isOk());
        for (int i = 0; i < 20 && LabRecipeRunner.isBusy(); i++) {
            LabRecipeRunner.tick();
        }
        assertEquals("ok", LabRecipeRunner.statusMap().get("status"));
        assertTrue(host.actions.contains("char:IRONCLAD"));
        assertTrue(host.actions.contains("embark"));
        assertTrue(host.dump().inGame);
    }

    @Test
    public void asyncRunnerEnsureFresh() {
        FakeLabHost host =
                new FakeLabHost(
                        LabStateSnapshot.builder()
                                .mode("CHAR_SELECT")
                                .menuScreen("MAIN_MENU")
                                .hasPlay(true)
                                .hasResume(true)
                                .buttons(Arrays.asList("RESUME_GAME", "PLAY"))
                                .build());
        StsLabNav.install(host);
        assertTrue(LabRecipeRunner.armEnsureFresh(12).isOk());
        for (int i = 0; i < 12 && LabRecipeRunner.isBusy(); i++) {
            LabRecipeRunner.tick();
        }
        assertEquals("ok", LabRecipeRunner.statusMap().get("status"));
        assertFalse(host.dump().hasResume);
    }
}
