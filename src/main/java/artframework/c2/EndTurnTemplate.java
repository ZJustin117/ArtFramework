package artframework.c2;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * C2 end-turn button template ({@code sts.endturn}).
 * Patches to {@code EndTurnButton} come later.
 */
public final class EndTurnTemplate {

    public static final String RESOURCE = NativeTemplateIds.END_TURN;

    private final CopyOnWriteArrayList<EndTurnInterceptor> interceptors =
            new CopyOnWriteArrayList<EndTurnInterceptor>();
    private boolean active;
    private boolean buttonEnabled = true;

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
        buttonEnabled = true;
    }

    public boolean isActive() {
        return active;
    }

    /** Presentation hint for consumers/decorators; no combat authority. */
    public void setButtonEnabled(boolean enabled) {
        this.buttonEnabled = enabled;
    }

    public boolean isButtonEnabled() {
        return buttonEnabled;
    }

    public void addInterceptor(EndTurnInterceptor interceptor) {
        if (interceptor == null) {
            throw new IllegalArgumentException("interceptor required");
        }
        interceptors.add(interceptor);
    }

    public void removeInterceptor(EndTurnInterceptor interceptor) {
        interceptors.remove(interceptor);
    }

    public GateResult dispatchPress() {
        if (!active) {
            return GateResult.ALLOW;
        }
        if (!buttonEnabled) {
            return GateResult.BLOCK;
        }
        for (EndTurnInterceptor interceptor : interceptors) {
            GateResult r = interceptor.intercept();
            if (r == GateResult.BLOCK) {
                return GateResult.BLOCK;
            }
        }
        return GateResult.ALLOW;
    }

    public int interceptorCount() {
        return interceptors.size();
    }

    void resetForTests() {
        interceptors.clear();
        active = false;
        buttonEnabled = true;
    }
}
