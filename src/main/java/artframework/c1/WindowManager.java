package artframework.c1;

import artframework.c1.layout.LayoutNode;
import artframework.c1.layout.LayoutNodeBridge;


/**
 * Legacy layout adapter over the immutable C1 declaration cache. It owns no window state.
 */
public final class WindowManager {

    private WindowManager() {}

    public static void put(String id, LayoutNode root) {
        // ECS declaration lifecycle owns layout state.
    }

    public static LayoutNode get(String id) {
        artframework.component.UiNode declaration = artframework.presentation.PresentationRuntime.declaration(
                artframework.presentation.PresentationRuntime.context(id));
        return declaration != null ? LayoutNodeBridge.toLegacyOrNull(declaration) : null;
    }

    public static void remove(String id) {
        // ECS declaration lifecycle owns layout state.
    }

    public static boolean contains(String id) {
        return artframework.presentation.PresentationRuntime.isOpen(id);
    }

    public static void resetForTests() {
        // PresentationRegistry owns test cleanup.
    }
}
