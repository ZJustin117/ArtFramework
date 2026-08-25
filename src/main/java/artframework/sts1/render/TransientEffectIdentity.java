package artframework.sts1.render;

/** Stable, host-neutral identity derived for one native effect instance. */
public final class TransientEffectIdentity {
    public final String instanceId;
    public final String nativeClass;
    public final int nativeIdentityHash;
    public final long generation;

    public TransientEffectIdentity(String instanceId, String nativeClass,
            int nativeIdentityHash, long generation) {
        this.instanceId = instanceId != null ? instanceId : "";
        this.nativeClass = nativeClass != null ? nativeClass : "";
        this.nativeIdentityHash = nativeIdentityHash;
        this.generation = generation;
    }
}
