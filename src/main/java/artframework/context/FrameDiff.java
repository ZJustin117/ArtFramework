package artframework.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of identity-align + diff for one applyFrame. */
public final class FrameDiff {

    public final List<String> added;
    public final List<String> removed;
    public final List<String> updated;
    public final boolean applied;
    public final String message;

    public FrameDiff(
            List<String> added,
            List<String> removed,
            List<String> updated,
            boolean applied,
            String message) {
        this.added = freeze(added);
        this.removed = freeze(removed);
        this.updated = freeze(updated);
        this.applied = applied;
        this.message = message != null ? message : "";
    }

    public static FrameDiff skipped(String message) {
        return new FrameDiff(null, null, null, false, message);
    }

    private static List<String> freeze(List<String> in) {
        if (in == null || in.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<String>(in));
    }
}
