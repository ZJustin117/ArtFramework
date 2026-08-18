package artframework.core;

import artframework.assets.HostAssets;
import artframework.ecs.PresentationWorld;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transaction boundary for enabled PresentPack operations across ECS and host resource domains.
 * It owns applied-operation history, not the data owned by those individual domains.
 */
public final class PackWorld {
    private final PresentationWorld entities;
    private final HostAssets assets;
    private final Map<String, List<PackOperation.Undo>> applied =
            new LinkedHashMap<String, List<PackOperation.Undo>>();
    private boolean forceRestore;

    public PackWorld(PresentationWorld entities, HostAssets assets) {
        if (entities == null || assets == null) throw new IllegalArgumentException("world and assets required");
        this.entities = entities;
        this.assets = assets;
    }

    public PresentationWorld entities() { return entities; }

    public HostAssets assets() { return assets; }

    public boolean isEnabled(String packId) { return applied.containsKey(packId); }

    public List<String> enabledPackIds() {
        return java.util.Collections.unmodifiableList(new ArrayList<String>(applied.keySet()));
    }

    public void enable(PresentPack pack) {
        if (pack == null) throw new IllegalArgumentException("pack required");
        if (isEnabled(pack.id)) return;
        validate(pack);
        List<PackOperation.Undo> undo = new ArrayList<PackOperation.Undo>();
        try {
            for (PackOperation operation : pack.operations) {
                undo.add(operation.apply(this));
            }
            applied.put(pack.id, undo);
        } catch (RuntimeException e) {
            try {
                rollback(undo, true);
            } catch (RuntimeException rollbackFailure) {
                e.addSuppressed(rollbackFailure);
            }
            throw e;
        }
    }

    public void disable(String packId) {
        List<PackOperation.Undo> undo = applied.get(packId);
        if (undo == null) return;
        rollback(undo, false);
        applied.remove(packId);
    }

    /** Forcefully aborts an enabled pack after a later activation step fails. */
    void abort(String packId) {
        List<PackOperation.Undo> undo = applied.get(packId);
        if (undo == null) return;
        try {
            rollback(undo, true);
        } finally {
            applied.remove(packId);
        }
    }

    void discardForReset(String packId) {
        applied.remove(packId);
    }

    private void validate(PresentPack pack) {
        for (PackOperation operation : pack.operations) {
            for (String required : operation.requiredPacks()) {
                if (!isEnabled(required)) {
                    throw new IllegalStateException(operation.id() + " requires enabled pack: " + required);
                }
            }
        }
    }

    boolean restoreRegistrations() { return forceRestore; }

    private void rollback(List<PackOperation.Undo> undo, boolean forceRestore) {
        RuntimeException failure = null;
        boolean previous = this.forceRestore;
        this.forceRestore = forceRestore;
        try {
            for (int i = undo.size() - 1; i >= 0; i--) {
                try {
                    undo.get(i).undo(this);
                } catch (RuntimeException e) {
                    if (failure == null) failure = e;
                    else failure.addSuppressed(e);
                }
            }
        } finally {
            this.forceRestore = previous;
        }
        if (failure != null) throw failure;
    }
}
