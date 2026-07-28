package artframework.context;

import artframework.core.UiSignalInterceptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Frame path + return channel: apply Backend snapshots, submit intents with intercept.
 */
public final class FrameRuntime {

    private final PresentProjection projection = new PresentProjection();
    private final List<UiSignalInterceptor> intentInterceptors =
            new ArrayList<UiSignalInterceptor>();

    public PresentProjection projection() {
        return projection;
    }

    /**
     * Pull snapshot from bound Primary Backend and apply.
     */
    public FrameDiff syncFromBackend() {
        return applyFrame(PresentBackends.get().snapshot());
    }

    public FrameDiff applyFrame(ContextFrame frame) {
        return projection.applyFrame(frame);
    }

    public void addIntentInterceptor(UiSignalInterceptor interceptor) {
        if (interceptor == null) {
            throw new IllegalArgumentException("interceptor required");
        }
        intentInterceptors.add(interceptor);
    }

    public void removeIntentInterceptor(UiSignalInterceptor interceptor) {
        intentInterceptors.remove(interceptor);
    }

    /**
     * Signal-level intercept then Backend intent. First BLOCK wins; no engine side effect.
     */
    public IntentResult submitIntent(UiIntent intent) {
        if (intent == null) {
            return IntentResult.rejected("intent required");
        }
        for (UiSignalInterceptor ix : intentInterceptors) {
            if (ix.intercept("", intent.surfaceId, intent.name, intent.args)
                    == UiSignalInterceptor.Result.BLOCK) {
                return IntentResult.rejected("blocked: " + intent.name);
            }
        }
        return PresentBackends.get().submitIntent(intent);
    }

    public void reset() {
        projection.reset();
        intentInterceptors.clear();
    }
}
