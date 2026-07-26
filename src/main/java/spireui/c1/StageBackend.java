package spireui.c1;

import spireui.c1.layout.LayoutNode;
import spireui.component.UiNode;

/**
 * Optional scene2d attach surface for C1. Logic open/close works without a backend;
 * production installs {@link spireui.c1.host.StageHost} after PostInitialize.
 */
public interface StageBackend {

    boolean isReady();

    void attach(String id, LayoutNode root);

    /**
     * Attach a full composition tree. Default implementations may ignore if not overridden.
     * {@link spireui.c1.host.StageHost} builds scene2d via {@link spireui.c1.layout.ComponentActors}.
     */
    void attachComposition(String id, UiNode root);

    void detach(String id);

    boolean isAttached(String id);

    int attachedCount();
}
