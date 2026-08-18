package artframework.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One reversible PresentPack mutation against a PackWorld domain. */
public abstract class PackOperation {
    private final String id;
    private final List<String> requiredPacks = new ArrayList<String>();

    protected PackOperation(String id) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("operation id required");
        this.id = id;
    }

    public final String id() { return id; }

    public final PackOperation requiresPack(String packId) {
        if (packId == null || packId.isEmpty()) throw new IllegalArgumentException("pack id required");
        requiredPacks.add(packId);
        return this;
    }

    final List<String> requiredPacks() {
        return Collections.unmodifiableList(requiredPacks);
    }

    abstract Undo apply(PackWorld world);

    interface Undo {
        void undo(PackWorld world);
    }
}
