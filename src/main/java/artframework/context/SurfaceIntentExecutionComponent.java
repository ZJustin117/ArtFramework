package artframework.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One-shot data-only request consumed by {@link SurfaceIntentExecutionSystem}. */
public final class SurfaceIntentExecutionComponent {
    public final String name;
    public final String surfaceId;
    public final List<Object> args;

    public SurfaceIntentExecutionComponent(String name, String surfaceId, Object... args) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("name required");
        this.name = name;
        this.surfaceId = surfaceId != null ? surfaceId : "";
        List<Object> copy = new ArrayList<Object>();
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof List) copy.add(Collections.unmodifiableList(new ArrayList<Object>((List<?>) arg)));
                else copy.add(arg);
            }
        }
        this.args = Collections.unmodifiableList(copy);
    }
}
