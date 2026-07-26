package spireui.c1;

import spireui.c1.layout.LayoutNode;

/**
 * Optional scene2d attach surface for C1. Logic open/close works without a backend;
 * production installs {@link spireui.c1.host.StageHost} after PostInitialize.
 */
public interface StageBackend {

    boolean isReady();

    void attach(String id, LayoutNode root);

    void detach(String id);

    boolean isAttached(String id);

    int attachedCount();
}
