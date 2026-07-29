package artframework.c2;

/** C2 end-turn lifecycle/presentation state; policy lives on the shared SignalBus. */
public final class EndTurnTemplate {
    public static final String RESOURCE = NativeTemplateIds.END_TURN;
    private boolean active;
    private boolean buttonEnabled = true;
    public void activate() { active = true; }
    public void deactivate() { active = false; buttonEnabled = true; }
    public boolean isActive() { return active; }
    public void setButtonEnabled(boolean enabled) { buttonEnabled = enabled; }
    public boolean isButtonEnabled() { return buttonEnabled; }
    void resetForTests() { active = false; buttonEnabled = true; }
}
