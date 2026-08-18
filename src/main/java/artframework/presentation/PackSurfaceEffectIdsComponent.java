package artframework.presentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** ECS bookkeeping for ambient pack effects currently projected onto one C2 item. */
public final class PackSurfaceEffectIdsComponent {
    private final List<String> effectIds;

    public PackSurfaceEffectIdsComponent(List<String> effectIds) {
        this.effectIds = effectIds == null || effectIds.isEmpty()
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(effectIds));
    }

    public List<String> effectIds() {
        return effectIds;
    }
}
