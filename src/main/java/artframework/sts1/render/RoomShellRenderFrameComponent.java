package artframework.sts1.render;

/** Shared ECS publication point for the current immutable ART room-shell overlay frame. */
public final class RoomShellRenderFrameComponent {
    public final RoomShellRenderFrame value;

    public RoomShellRenderFrameComponent(RoomShellRenderFrame value) {
        this.value = value == null ? RoomShellRenderFrame.empty() : value;
    }
}
