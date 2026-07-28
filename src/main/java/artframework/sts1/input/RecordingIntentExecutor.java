package artframework.sts1.input;

import artframework.context.IntentResult;
import artframework.context.UiIntent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Test / lab executor: logs intents and accepts by default. */
public final class RecordingIntentExecutor implements IntentExecutor {

    private final List<UiIntent> log = new ArrayList<UiIntent>();
    private boolean accept = true;
    private String rejectMessage = "rejected by recording executor";

    public RecordingIntentExecutor setAccept(boolean accept) {
        this.accept = accept;
        return this;
    }

    public RecordingIntentExecutor setRejectMessage(String message) {
        this.rejectMessage = message != null ? message : "rejected";
        return this;
    }

    @Override
    public IntentResult execute(UiIntent intent) {
        if (intent == null) {
            return IntentResult.rejected("intent required");
        }
        log.add(intent);
        if (!accept) {
            return IntentResult.rejected(rejectMessage);
        }
        return IntentResult.accepted(intent.name);
    }

    public List<UiIntent> log() {
        return Collections.unmodifiableList(new ArrayList<UiIntent>(log));
    }

    public void clear() {
        log.clear();
    }
}
