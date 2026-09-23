package artframework.sts1.render;

/** Immutable, host-neutral exemption data projected onto a native invocation entity. */
public final class NativeRenderExemptionComponent {
    public final long policyRevision;
    public final boolean exempt;
    public final String matchedTarget;

    public NativeRenderExemptionComponent(long policyRevision, boolean exempt, String matchedTarget) {
        this.policyRevision = policyRevision;
        this.exempt = exempt;
        this.matchedTarget = matchedTarget == null ? "" : matchedTarget;
    }
}
