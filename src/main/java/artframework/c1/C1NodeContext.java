package artframework.c1;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import artframework.c1.layout.ComponentActors;
import artframework.component.UiNode;

/**
 * Inflate context for {@link C1NodeFactory}.
 */
public final class C1NodeContext {

    public final String windowId;
    public final Skin skin;
    public final Runnable onClose;
    public final float scale;
    /** Parent effect-target key for anonymous path ids ({@link artframework.component.LayoutEngine#effectKey}). */
    public final String parentEffectKey;
    public final int siblingIndex;

    public C1NodeContext(String windowId, Skin skin, Runnable onClose, float scale) {
        this(windowId, skin, onClose, scale, "", 0);
    }

    public C1NodeContext(
            String windowId,
            Skin skin,
            Runnable onClose,
            float scale,
            String parentEffectKey,
            int siblingIndex) {
        this.windowId = windowId;
        this.skin = skin;
        this.onClose = onClose;
        this.scale = scale;
        this.parentEffectKey = parentEffectKey != null ? parentEffectKey : "";
        this.siblingIndex = siblingIndex;
    }

    public C1NodeContext childContext(String thisEffectKey) {
        return new C1NodeContext(windowId, skin, onClose, scale, thisEffectKey, 0);
    }

    public Actor buildChild(UiNode node) {
        return buildChild(node, 0);
    }

    public Actor buildChild(UiNode node, int index) {
        return ComponentActors.buildNode(
                windowId, node, skin, onClose, scale, parentEffectKey, index);
    }
}
