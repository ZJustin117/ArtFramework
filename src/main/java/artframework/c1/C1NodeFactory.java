package artframework.c1;

import com.badlogic.gdx.scenes.scene2d.Actor;
import artframework.component.UiNode;

/**
 * C1 (scene2d) inflater for a registered presentation node type.
 */
public interface C1NodeFactory {

    /** Node type name matching {@link artframework.component.UiNode#type}. */
    String type();

    Actor create(UiNode node, C1NodeContext context);
}
