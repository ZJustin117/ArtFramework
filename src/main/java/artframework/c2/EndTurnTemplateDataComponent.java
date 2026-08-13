package artframework.c2;

/** Data-only presentation state observed from the native end-turn template. */
public final class EndTurnTemplateDataComponent {
    public final boolean buttonEnabled;

    public EndTurnTemplateDataComponent(boolean buttonEnabled) {
        this.buttonEnabled = buttonEnabled;
    }
}
