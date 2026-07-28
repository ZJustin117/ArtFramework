package artframework.context;

/** Default unbound backend: unavailable frames, all intents rejected. */
public final class NoOpPresentationBackend implements PresentationBackend {

    public static final NoOpPresentationBackend INSTANCE = new NoOpPresentationBackend();

    private NoOpPresentationBackend() {}

    @Override
    public String id() {
        return "noop";
    }

    @Override
    public BackendMode mode() {
        return BackendMode.READ_ONLY;
    }

    @Override
    public ContextFrame snapshot() {
        return ContextFrame.unavailable(0L);
    }

    @Override
    public IntentResult submitIntent(UiIntent intent) {
        return IntentResult.rejected("no presentation backend bound");
    }
}
