package artframework.sts1.patch;

import artframework.sts1.render.NativeRenderBridge;
import artframework.sts1.render.RenderDisposition;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

/**
 * Gates native monster intent rendering. When the intents surface is FULL + mounted + combat scene,
 * ART suppresses the original AbstractMonster.renderIntent and paints intents through the synced
 * C2 items. Otherwise the native renderer continues unchanged.
 */
public final class CombatIntentRenderPatches {
    private CombatIntentRenderPatches() {}

    @SpirePatch(
            clz = AbstractMonster.class,
            method = "renderIntent",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeIntent {
        public static SpireReturn<Void> Prefix(
                AbstractMonster __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            RenderDisposition disposition = NativeRenderBridge.beginSurface(
                    "sts1.combat.intents",
                    "com.megacrit.cardcrawl.monsters.AbstractMonster",
                    "renderIntent",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return disposition.nativeContinuation
                    ? SpireReturn.Continue()
                    : SpireReturn.Return(null);
        }
    }
}
