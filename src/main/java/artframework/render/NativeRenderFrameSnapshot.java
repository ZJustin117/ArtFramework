package artframework.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable ordered native render projection for one ECS frame. */
public final class NativeRenderFrameSnapshot {
    private final List<NativeRenderInputSnapshot> inputs;

    NativeRenderFrameSnapshot(List<NativeRenderInputSnapshot> inputs) {
        this.inputs = Collections.unmodifiableList(new ArrayList<NativeRenderInputSnapshot>(inputs));
    }

    public List<NativeRenderInputSnapshot> inputs() {
        return inputs;
    }

    public int size() {
        return inputs.size();
    }

    public NativeRenderInputSnapshot get(int index) {
        return inputs.get(index);
    }
}
