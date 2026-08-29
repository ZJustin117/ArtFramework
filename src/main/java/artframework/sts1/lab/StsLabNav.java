package artframework.sts1.lab;

import artframework.api.UiOpResult;

/**
 * L1 facade over the process {@link LabHost} (default {@link StsLabHost}).
 * Tests may {@link #install(LabHost)}.
 */
public final class StsLabNav {

    public static final String LOG_PREFIX = StsLabRecipes.LOG_PREFIX;

    private static LabHost host = StsLabHost.INSTANCE;
    private static StsLabRecipes recipes = new StsLabRecipes(host);

    private StsLabNav() {}

    public static void install(LabHost h) {
        host = h != null ? h : StsLabHost.INSTANCE;
        recipes = new StsLabRecipes(host);
    }

    public static void resetForTests() {
        host = StsLabHost.INSTANCE;
        recipes = new StsLabRecipes(host);
        LabRecipeRunner.resetForTests();
        StsLabNativeNavigator.resetForTests();
    }

    public static LabHost host() {
        return host;
    }

    public static StsLabRecipes recipes() {
        return recipes;
    }

    public static LabStateSnapshot dump() {
        return host.dump();
    }

    public static UiOpResult clearSaves() {
        return host.clearSaves();
    }

    public static UiOpResult stripResume() {
        return host.stripResumeButtons();
    }

    public static UiOpResult openCharSelect() {
        return host.openCharSelect();
    }

    public static UiOpResult selectCharacter(String id) {
        return host.selectCharacter(id);
    }

    public static UiOpResult embark() {
        return host.embark();
    }

    public static UiOpResult setSeed(String seed) {
        return host.setSeed(seed);
    }

    public static UiOpResult menuClick(String result) {
        return host.menuClick(result);
    }

    public static UiOpResult abandon() {
        return host.abandon();
    }

    public static UiOpResult abandonConfirm() {
        return host.abandonConfirm();
    }

    public static UiOpResult returnMenu() {
        return host.returnToMenu();
    }

    public static UiOpResult proceed() {
        return host.proceed();
    }

    public static UiOpResult enterEvent(String eventId) {
        return host.enterEvent(eventId);
    }

    public static UiOpResult enterRoom(String roomKind) {
        return host.enterRoom(roomKind);
    }

    public static UiOpResult enterSelect(String selectKind) {
        return host.enterSelect(selectKind);
    }

    /**
     * Synchronous recipe (FakeLabHost / unit tests). On device prefer {@link #armEnsureMenu()} so
     * the game thread is not blocked.
     */
    public static UiOpResult ensureMenu() {
        return recipes.ensureMenu();
    }

    public static UiOpResult ensureFreshMenu() {
        return recipes.ensureFreshMenu();
    }

    public static UiOpResult startRun(String characterId, String seed) {
        return recipes.startRun(characterId, seed);
    }

    public static UiOpResult reset() {
        return recipes.reset();
    }

    /** Arm async ensure-menu (advanced each {@link LabRecipeRunner#tick}). */
    public static UiOpResult armEnsureMenu() {
        return LabRecipeRunner.armEnsureMenu(StsLabRecipes.ASYNC_DEFAULT_BUDGET);
    }

    public static UiOpResult armEnsureFreshMenu() {
        return LabRecipeRunner.armEnsureFresh(StsLabRecipes.ASYNC_DEFAULT_BUDGET);
    }

    public static UiOpResult armStartRun(String characterId, String seed) {
        return LabRecipeRunner.armStartRun(characterId, seed, StsLabRecipes.ASYNC_DEFAULT_BUDGET);
    }
}
