package artframework.c2.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.ui.buttons.GridSelectConfirmButton;
import artframework.c2.SelectKind;
import artframework.c2.hooks.HostPatchResults;
import artframework.c2.hooks.NativeUiHooks;
import artframework.core.SignalDispatchResult;

/**
 * Grid confirm gate. Card pick automation uses {@link artframework.api.UiOps#selectCard}.
 */
@SuppressWarnings("unused")
public final class SelectScreenPatches {

    private SelectScreenPatches() {}

    @SpirePatch(clz = GridSelectConfirmButton.class, method = "update", paramtypez = {})
    public static class GateGridConfirmClick {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(GridSelectConfirmButton __instance) {
            if (__instance.hb == null || !__instance.hb.clicked) {
                return SpireReturn.Continue();
            }
            SignalDispatchResult result = NativeUiHooks.onSelectConfirm(SelectKind.GRID);
            if (!HostPatchResults.allowsNative(result)) {
                __instance.hb.clicked = false;
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }
}
