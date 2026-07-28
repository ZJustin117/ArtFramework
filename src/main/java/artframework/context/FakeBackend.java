package artframework.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Scriptable Primary Backend for JUnit: push frames, log intents, optional auto-accept.
 */
public final class FakeBackend implements PresentationBackend {

    private final String id;
    private BackendMode mode = BackendMode.FULL_CONTROL;
    private ContextFrame current = ContextFrame.unavailable(0L);
    private final List<UiIntent> intentLog = new ArrayList<UiIntent>();
    private boolean acceptIntents = true;
    private String rejectMessage = "rejected by fake";

    public FakeBackend() {
        this("fake");
    }

    public FakeBackend(String id) {
        this.id = id != null ? id : "fake";
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public BackendMode mode() {
        return mode;
    }

    public FakeBackend setMode(BackendMode mode) {
        this.mode = mode != null ? mode : BackendMode.FULL_CONTROL;
        return this;
    }

    public FakeBackend setAcceptIntents(boolean accept) {
        this.acceptIntents = accept;
        return this;
    }

    public FakeBackend setRejectMessage(String message) {
        this.rejectMessage = message != null ? message : "rejected by fake";
        return this;
    }

    /** Replace current authority frame (monotonic frameId recommended). */
    public FakeBackend pushFrame(ContextFrame frame) {
        if (frame == null) {
            throw new IllegalArgumentException("frame required");
        }
        this.current = frame;
        return this;
    }

    @Override
    public ContextFrame snapshot() {
        return current;
    }

    @Override
    public IntentResult submitIntent(UiIntent intent) {
        if (intent == null) {
            return IntentResult.rejected("intent required");
        }
        intentLog.add(intent);
        if (mode == BackendMode.READ_ONLY) {
            return IntentResult.rejected("read-only backend");
        }
        if (!acceptIntents) {
            return IntentResult.rejected(rejectMessage);
        }
        return IntentResult.accepted(intent.name);
    }

    public List<UiIntent> intentLog() {
        return Collections.unmodifiableList(new ArrayList<UiIntent>(intentLog));
    }

    public void clearIntentLog() {
        intentLog.clear();
    }
}
