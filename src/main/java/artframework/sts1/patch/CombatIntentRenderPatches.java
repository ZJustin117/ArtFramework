package artframework.sts1.patch;

import artframework.sts1.render.NativeRenderBridge;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

/**
 * Observe native intent rendering for ART recording/projection. The original STS
 * AbstractMonster.renderIntent remains the visual authority; ART does not suppress it.
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
            NativeRenderBridge.beginSurface(
                    "sts1.combat.intents",
                    "com.megacrit.cardcrawl.monsters.AbstractMonster",
                    "renderIntent",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return SpireReturn.Continue();
        }
    }
}
