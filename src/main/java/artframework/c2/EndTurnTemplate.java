package artframework.c2;

/** C2 end-turn lifecycle/presentation state; policy lives on the shared SignalBus. */
public final class EndTurnTemplate {
    public static final String RESOURCE = NativeTemplateIds.END_TURN;
    public void activate() {}
    public void deactivate() { NativeTemplateRuntime.setEndTurnEnabled(true); }
    public boolean isActive() { return NativeTemplateRuntime.isEndTurnBound(); }
    public void setButtonEnabled(boolean enabled) { NativeTemplateRuntime.setEndTurnEnabled(enabled); }
    public boolean isButtonEnabled() { return NativeTemplateRuntime.endTurnEnabled(); }
    void resetForTests() {}
}
