package artframework.c2.hooks;

import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import artframework.core.SignalDispatchResult;

/**
 * Single adapter from unified bus delivery to host {@code @SpirePatch} continue/return decisions
 * (milestone 21.5). Patches must not re-implement boolean interpretation of bus results.
 */
public final class HostPatchResults {

    private HostPatchResults() {}

    /** True when no native operation listener consumed or rejected the operation. */
    public static boolean allowsNative(SignalDispatchResult result) {
        return result == null || !result.isStopped();
    }

    /** Prefix-patch helper: continue native method or return early when blocked. */
    public static SpireReturn<Void> voidPrefix(SignalDispatchResult result) {
        return allowsNative(result) ? SpireReturn.Continue() : SpireReturn.Return(null);
    }
}
