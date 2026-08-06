package artframework.c1;

import com.badlogic.gdx.scenes.scene2d.Actor;
import artframework.c1.layout.ComponentActors;
import artframework.component.ArtNodeTypes;
import artframework.component.UiNode;
import artframework.component.UiTypes;

/**
 * Registers built-in C1 inflaters that delegate to {@link ComponentActors}.
 */
final class BuiltinC1Factories {

    private BuiltinC1Factories() {}

    static void registerAll(C1NodeFactories reg) {
        reg.register(delegate(UiTypes.FRAGMENT));
        reg.register(delegate(UiTypes.COL));
        reg.register(delegate(UiTypes.PANEL));
        reg.register(delegate(UiTypes.GLASS));
        reg.register(delegate(UiTypes.SCROLL));
        reg.register(delegate(UiTypes.MARGIN));
        reg.register(delegate(UiTypes.CENTER));
        reg.register(delegate(UiTypes.ROW));
        reg.register(delegate(UiTypes.GRID));
        reg.register(delegate(UiTypes.STACK));
        reg.register(delegate(UiTypes.TABS));
        reg.register(delegate(UiTypes.LABEL));
        reg.register(delegate(UiTypes.BUTTON));
        reg.register(delegate(UiTypes.SLIDER));
        reg.register(delegate(UiTypes.HITAREA));
        reg.register(delegate(UiTypes.TEXTFIELD));
        reg.register(delegate(UiTypes.CHECKBOX));
        reg.register(delegate(UiTypes.PROGRESS));
        reg.register(delegate(ArtNodeTypes.ANIMATION_PLAYER));
        reg.register(delegate(ArtNodeTypes.SHADER_EFFECT));
        reg.register(delegate(ArtNodeTypes.SKELETON));
        reg.register(delegate(ArtNodeTypes.PRESENT_PROFILE));
    }

    private static C1NodeFactory delegate(final String type) {
        return new C1NodeFactory() {
            @Override
            public String type() {
                return type;
            }

            @Override
            public Actor create(UiNode node, C1NodeContext context) {
                return ComponentActors.inflateBuiltin(node, context);
            }
        };
    }
}
