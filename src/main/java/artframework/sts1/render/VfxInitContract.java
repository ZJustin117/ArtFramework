package artframework.sts1.render;

/**
 * Pure, host-neutral contract describing the native transient effects that require a
 * POST-CONSTRUCTOR runtime initializer before their draw fields are usable (NRO-04 A01).
 *
 * <p><strong>The contract.</strong> A claimable native effect may be constructed in one of two ways:
 * <ul>
 *   <li>Its constructor fully initializes every field the native {@code render} reads, so a freshly
 *       constructed instance is immediately drawable; or</li>
 *   <li>It is a runtime-initialized (pooled) effect: {@link com.badlogic.gdx.utils.Pool.Poolable},
 *       whose no-arg constructor only selects static art while the draw fields ({@code x}/{@code y}/
 *       {@code scale}/{@code color}/timing) are set by a separate initializer called after
 *       construction. Production obtains the instance from a pool and always calls that initializer
 *       before the effect is ever rendered.</li>
 * </ul>
 *
 * <p><strong>Why only pooled classes need it.</strong> A pooled instance is recycled, so its
 * constructor cannot carry the per-spawn position/color; those live in the initializer. Every
 * non-pooled claimable effect sets its own draw fields in the constructor and therefore needs no
 * post-constructor call.
 *
 * <p><strong>Audit result.</strong> A whole-repo audit of the claimable FQNs (verified with
 * {@code javap} against {@code $ART_STS_JAR}) found that only {@code CardTrailEffect} implements
 * {@link com.badlogic.gdx.utils.Pool.Poolable} among the 60 claimable FQNs; every other claimable
 * class is fully initialized by its constructor. This contract therefore reports {@code true} for
 * exactly {@link VfxDrawGeometry.Kind#CARD_TRAIL} and {@code false} for everything else (including
 * {@code null}).
 *
 * <p><strong>Fail-open obligation.</strong> Because production always calls the initializer before
 * render, any consumer (e.g. the lab spawn path, see F30/F30b) that constructs such an effect must
 * also initialize it, or DECLINE to present the uninitialized instance (fail-open) rather than draw
 * it. An uninitialized instance has a null {@code color} and the renderer's field reader declines it
 * — the F30b regression where a lab-spawned {@code CardTrailEffect} left {@code color} null and the
 * D1 scenario broke ({@code nativeRenderStrict.accepted=false}).
 *
 * <p>This type is pure data: it performs no GL work, holds no host handles, and touches no native
 * class. It is the single source of truth for the initializer method name and lab coordinates so the
 * lab path cannot drift from the native contract silently.
 */
public final class VfxInitContract {

    private VfxInitContract() {}

    /**
     * True iff {@code kind}'s native effect is obtained from a pool and its draw fields are only set
     * by a post-constructor initializer (today exactly {@link VfxDrawGeometry.Kind#CARD_TRAIL}).
     * {@code null} and every other kind are {@code false}.
     */
    public static boolean requiresRuntimeInit(VfxDrawGeometry.Kind kind) {
        return kind == VfxDrawGeometry.Kind.CARD_TRAIL;
    }

    /**
     * The native method the production gameplay path calls after construction, or {@code null} when
     * the kind needs no runtime init. Today that is {@code "init"} for
     * {@link VfxDrawGeometry.Kind#CARD_TRAIL} and {@code null} otherwise.
     */
    public static String initializerMethod(VfxDrawGeometry.Kind kind) {
        if (kind == VfxDrawGeometry.Kind.CARD_TRAIL) {
            return "init";
        }
        return null;
    }

    /**
     * The lab initialization arguments for {@code kind}, or {@code null} when the kind needs no
     * runtime init. Today that is {@code {960f, 540f}} for
     * {@link VfxDrawGeometry.Kind#CARD_TRAIL} (the lab screen-center point; the native initializer
     * applies its own fixed {@code -6f} offsets) and {@code null} otherwise. Returns a FRESH array
     * on every call so no caller can mutate shared state.
     */
    public static float[] initializerArgs(VfxDrawGeometry.Kind kind) {
        if (kind == VfxDrawGeometry.Kind.CARD_TRAIL) {
            return new float[] {960f, 540f};
        }
        return null;
    }
}
