package spireui.c2.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import com.megacrit.cardcrawl.screens.select.HandCardSelectScreen;
import com.megacrit.cardcrawl.ui.buttons.GridSelectConfirmButton;
import spireui.c2.GateResult;
import spireui.c2.SelectKind;
import spireui.c2.hooks.NativeUiHooks;

/**
 * Confirm-button gates for grid/hand select. Card-level clicks vary by STS version;
 * consumers can still use {@link spireui.api.UiOps#selectCard}.
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
            GateResult r = NativeUiHooks.onSelectConfirm(SelectKind.GRID);
            if (r == GateResult.BLOCK) {
                __instance.hb.clicked = false;
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }

    /**
     * Hand select uses screen update; when confirm is pressed STS sets flags — gate via
     * {@link HandCardSelectScreen#update} is too broad. Prefer UiOps for automation;
     * this patch no-ops unless bound (still runs ALLOW path).
     */
    @SpirePatch(clz = HandCardSelectScreen.class, method = "update", paramtypez = {})
    public static class HandSelectObserve {
        @SpirePrefixPatch
        public static void Prefix(HandCardSelectScreen __instance) {
            // Intentionally empty: bind-only presence for future fine-grained hooks.
            // Avoid blocking entire hand select update loop.
        }
    }

    @SpirePatch(clz = GridCardSelectScreen.class, method = "update", paramtypez = {})
    public static class GridSelectObserve {
        @SpirePrefixPatch
        public static void Prefix(GridCardSelectScreen __instance) {
            // bind-only presence; card pick automation via UiOps
        }
    }
}
