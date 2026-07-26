package artframework.c2;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * C2 card-select template for grid and hand screens.
 * Resources: {@code sts.select.grid}, {@code sts.select.hand}.
 */
public final class SelectTemplate {

    private final SelectKind kind;
    private final String resource;
    private final CopyOnWriteArrayList<SelectCardInterceptor> cardInterceptors =
            new CopyOnWriteArrayList<SelectCardInterceptor>();
    private final CopyOnWriteArrayList<SelectConfirmInterceptor> confirmInterceptors =
            new CopyOnWriteArrayList<SelectConfirmInterceptor>();
    private boolean active;

    public SelectTemplate(SelectKind kind, String resource) {
        if (kind == null || resource == null || resource.isEmpty()) {
            throw new IllegalArgumentException("kind and resource required");
        }
        this.kind = kind;
        this.resource = resource;
    }

    public SelectKind kind() {
        return kind;
    }

    public String resource() {
        return resource;
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    public void addCardInterceptor(SelectCardInterceptor interceptor) {
        if (interceptor == null) {
            throw new IllegalArgumentException("interceptor required");
        }
        cardInterceptors.add(interceptor);
    }

    public void removeCardInterceptor(SelectCardInterceptor interceptor) {
        cardInterceptors.remove(interceptor);
    }

    public void addConfirmInterceptor(SelectConfirmInterceptor interceptor) {
        if (interceptor == null) {
            throw new IllegalArgumentException("interceptor required");
        }
        confirmInterceptors.add(interceptor);
    }

    public void removeConfirmInterceptor(SelectConfirmInterceptor interceptor) {
        confirmInterceptors.remove(interceptor);
    }

    public GateResult dispatchCard(SelectCardRef card) {
        if (card == null) {
            throw new IllegalArgumentException("card required");
        }
        if (!active) {
            return GateResult.ALLOW;
        }
        for (SelectCardInterceptor interceptor : cardInterceptors) {
            GateResult r = interceptor.intercept(kind, card);
            if (r == GateResult.BLOCK) {
                return GateResult.BLOCK;
            }
        }
        return GateResult.ALLOW;
    }

    public GateResult dispatchConfirm() {
        if (!active) {
            return GateResult.ALLOW;
        }
        for (SelectConfirmInterceptor interceptor : confirmInterceptors) {
            GateResult r = interceptor.intercept(kind);
            if (r == GateResult.BLOCK) {
                return GateResult.BLOCK;
            }
        }
        return GateResult.ALLOW;
    }

    void resetForTests() {
        cardInterceptors.clear();
        confirmInterceptors.clear();
        active = false;
    }
}
