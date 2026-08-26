package artframework.sts1.patch;

import artframework.sts1.render.NativeRenderBridge;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * Observes container-driven effect renders at the three AbstractDungeon traversal sites
 * (effectList behind/regular, topLevelEffects). The single-arg
 * {@code AbstractGameEffect#render(SpriteBatch)} is abstract — ModTheSpire cannot attach a
 * Prefix to a bodyless method — so each call site is replaced with an observe-then-render
 * helper. The native render always executes; observation failures never interrupt drawing.
 */
public final class TransientEffectContainerPatches {

    private TransientEffectContainerPatches() {}

    /** Package-visible entry generated at each instrumented call site. */
    public static void observeThenRender(AbstractGameEffect effect, SpriteBatch sb) {
        try {
            NativeRenderBridge.beginEffectRender(effect, "render");
        } catch (Throwable error) {
            NativeRenderBridge.recordEffectObservationFailure();
        }
        try {
            effect.render(sb);
        } catch (Throwable error) {
            NativeRenderBridge.recordEffectObservationFailure();
        }
    }

    @SpirePatch(clz = AbstractDungeon.class, method = "render",
            paramtypez = {SpriteBatch.class})
    public static class ObserveContainerEffectRenders {
        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!"render".equals(call.getMethodName())
                            || !"com.megacrit.cardcrawl.vfx.AbstractGameEffect"
                                    .equals(call.getClassName())
                            || !"(Lcom/badlogic/gdx/graphics/g2d/SpriteBatch;)V"
                                    .equals(call.getSignature())) {
                        return;
                    }
                    call.replace("{"
                            + "artframework.sts1.patch.TransientEffectContainerPatches"
                            + ".observeThenRender($0, $1);"
                            + "}");
                }
            };
        }
    }
}
