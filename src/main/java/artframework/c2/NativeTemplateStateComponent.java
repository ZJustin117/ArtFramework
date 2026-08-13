package artframework.c2;

/** Data-only bind lifecycle state observed from a native template adapter. */
public final class NativeTemplateStateComponent {
    public final String templateId;
    public final boolean bound;

    public NativeTemplateStateComponent(String templateId, boolean bound) {
        this.templateId = templateId != null ? templateId : "";
        this.bound = bound;
    }
}
