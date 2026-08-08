package artframework.sts1.c1;

import artframework.c1.C1NodeContext;
import artframework.c1.C1NodeFactory;
import artframework.component.ArtNodeTypes;
import artframework.component.UiNode;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/** C1 adapter factory for the built-in semantic STS1 node family. */
public final class Sts1VanillaNodeFactory implements C1NodeFactory {

    private final String type;

    public Sts1VanillaNodeFactory(String type) {
        this.type = type;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public Actor create(UiNode node, C1NodeContext context) {
        Sts1VanillaActor art = new Sts1VanillaActor(context.windowId, node, context.scale);
        if (!isContainer(node.type)) {
            return art;
        }
        Stack stack = new Stack();
        stack.add(art);
        Table content = new Table(context.skin);
        for (int i = 0; i < node.children.size(); i++) {
            Actor child = context.buildChild(node.children.get(i), i);
            if (child != null) {
                content.add(child).growX().row();
            }
        }
        stack.add(content);
        return stack;
    }

    private static boolean isContainer(String type) {
        return ArtNodeTypes.STS_PANEL.equals(type) || ArtNodeTypes.STS_MAP.equals(type);
    }
}
