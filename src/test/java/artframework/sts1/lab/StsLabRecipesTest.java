package artframework.sts1.lab;

import artframework.api.UiOpResult;
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
