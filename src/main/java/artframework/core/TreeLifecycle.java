package artframework.core;

/**
 * Optional hooks for {@link UiTree} mount lifecycle (Godot enter_tree / ready / exit_tree).
 */
public interface TreeLifecycle {
    void onMount(UiInstance node);

    void onReady(UiInstance node);

    void onUnmount(UiInstance node);
}
