package artframework.sts1.lab;

import artframework.api.UiOpResult;

/**
 * Lab navigation host. Production: {@link StsLabHost}. Tests: {@link FakeLabHost}.
 * No STS types on this boundary.
 */
public interface LabHost {

    LabStateSnapshot dump();

    UiOpResult clearSaves();

    UiOpResult stripResumeButtons();

    UiOpResult openCharSelect();

    UiOpResult selectCharacter(String characterId);

    UiOpResult embark();

    /** Optional seed text; null/empty = skip (ok). */
    UiOpResult setSeed(String seedText);

    UiOpResult menuClick(String clickResult);

    UiOpResult abandon();

    UiOpResult abandonConfirm();

    UiOpResult returnToMenu();

    UiOpResult proceed();

    /** Enter one validated vanilla event without invoking the native event console command. */
    UiOpResult enterEvent(String eventId);

    /** Enter a reachable native rest, shop, or treasure room through map navigation. */
    UiOpResult enterRoom(String roomKind);

    /** Open a native grid or hand selection screen using the current run's real cards. */
    UiOpResult enterSelect(String selectKind);

    /**
     * Yield so scheduled GL/input work can apply before the next recipe tick.
     * Fake hosts no-op; STS host sleeps briefly.
     */
    void yieldFrame();
}
