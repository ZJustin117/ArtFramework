package artframework.sts1.render;

/** Coordinates native effect lifecycle observations with ART transient presentation entities. */
public final class TransientEffectLifecycleAdapter {
    private final TransientEffectLedger ledger;
    private final TransientEffectRegistry registry;

    public TransientEffectLifecycleAdapter(TransientEffectLedger ledger,
            TransientEffectRegistry registry) {
        if (ledger == null || registry == null) throw new IllegalArgumentException("lifecycle state required");
        this.ledger = ledger;
        this.registry = registry;
    }

    public void create(TransientEffectIdentity identity) {
        ledger.create(identity);
    }

    public void render(TransientEffectIdentity identity, long frameId, String method) {
        ledger.create(identity);
        ledger.render(identity);
        registry.present(identity, frameId, method);
    }

    public void update(TransientEffectIdentity identity, boolean done) {
        ledger.update(identity, done);
        if (done) registry.cleanup(identity);
    }

    public void complete(TransientEffectIdentity identity) {
        ledger.complete(identity);
        registry.cleanup(identity);
    }

    public void cancel(TransientEffectIdentity identity) {
        ledger.dispose(identity);
        registry.cleanup(identity);
    }

    public void cleanupAll() {
        registry.clear();
        ledger.clearLeaked();
    }

    /** Recovery owns the teardown, so unfinished effects are cancelled rather than leaked. */
    public void cleanupForRecovery() {
        registry.clear();
        ledger.clearCompletedForRecovery();
    }
}
