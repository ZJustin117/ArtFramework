package artframework.c1.host;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Owns disposable window skins while the current default skin remains host-owned. */
final class SkinOwnership {

    interface Releaser {
        void release(Object skin);
    }

    private final Releaser releaser;
    private final Map<String, Object> active = new LinkedHashMap<String, Object>();
    private final Set<Object> retired = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());

    SkinOwnership(Releaser releaser) {
        this.releaser = releaser;
    }

    void attach(String windowId, Object skin) {
        Object previous = active.put(windowId, skin);
        if (previous != null && previous != skin) release(previous, null);
    }

    void detach(String windowId, Object currentDefault) {
        release(active.remove(windowId), currentDefault);
    }

    void releaseUnattached(Object skin, Object currentDefault) {
        if (skin != null && skin != currentDefault) dispose(skin);
    }

    void replaceDefault(Object previousDefault) {
        if (previousDefault == null) return;
        retired.add(previousDefault);
        disposeRetiredIfUnreferenced(previousDefault);
    }

    void clear(Object currentDefault) {
        Set<Object> attached = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        attached.addAll(active.values());
        active.clear();
        for (Object skin : attached) release(skin, currentDefault);
        for (Object skin : new ArrayList<Object>(retired)) dispose(skin);
        retired.clear();
    }

    private void release(Object skin, Object currentDefault) {
        if (skin == null || active.containsValue(skin) || skin == currentDefault) return;
        if (retired.remove(skin)) dispose(skin);
        else dispose(skin);
    }

    private void disposeRetiredIfUnreferenced(Object skin) {
        if (!active.containsValue(skin) && retired.remove(skin)) dispose(skin);
    }

    private void dispose(Object skin) {
        releaser.release(skin);
    }
}
