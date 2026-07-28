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

    /**
     * Yield so scheduled GL/input work can apply before the next recipe tick.
     * Fake hosts no-op; STS host sleeps briefly.
     */
    void yieldFrame();
}
