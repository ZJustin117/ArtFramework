package artframework.presentation;

/** NodeTree lifecycle callback order mirrors Godot mount/ready/unmount order. */
public interface NodeTreeLifecycle {
    void onMount(Node node);
    void onReady(Node node);
    void onUnmount(Node node);
}
