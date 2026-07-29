package artframework.c1.layout;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.megacrit.cardcrawl.core.Settings;
import artframework.api.ArtFramework;
import artframework.api.UiOps;
import artframework.c1.C1NodeContext;
import artframework.c1.C1NodeFactories;
import artframework.c1.C1NodeFactory;
import artframework.component.ArtNodeTypes;
import artframework.component.UiNode;
import artframework.component.UiTypes;
import artframework.component.WidgetSession;
import artframework.component.WidgetSessions;

/**
 * Builds scene2d actors from a composition {@link UiNode} tree (Nest containers + leaves).
 * Uses scene2d Table layout (not absolute LayoutEngine coords). Call only with a live Skin.
 * Child types are resolved via {@link artframework.c1.C1NodeFactories}.
 */
public final class ComponentActors {

    private ComponentActors() {}

    public static Actor toActor(String windowId, UiNode root, Skin skin, final Runnable onClose) {
        if (windowId == null || windowId.isEmpty()) {
            throw new IllegalArgumentException("windowId required");
        }
        if (root == null) {
            throw new IllegalArgumentException("root required");
        }
        if (skin == null) {
            throw new IllegalArgumentException("skin required");
        }
        if (!UiTypes.WINDOW.equals(root.type)) {
            throw new IllegalArgumentException("root must be window");
        }

        float scale = Settings.scale > 0f ? Settings.scale : 1f;
        float width = root.layout.hasWidth() ? root.layout.width * scale : 400f * scale;
        float height = root.layout.hasHeight() ? root.layout.height * scale : 240f * scale;
        String title = root.propString("title", windowId);

        Window window = new Window(title, skin);
        window.setModal(true);
        window.setMovable(true);
        window.setSize(width, height);
        float pad = root.layout.pad > 0f ? root.layout.pad * scale : 8f * scale;
        window.defaults().pad(pad);

        for (UiNode child : root.children) {
            Actor a = buildNode(windowId, child, skin, onClose, scale);
            if (a != null) {
                window.add(a).growX().padBottom(6f * scale).row();
            }
        }

        float screenW = Settings.WIDTH > 0 ? Settings.WIDTH : 1920f;
        float screenH = Settings.HEIGHT > 0 ? Settings.HEIGHT : 1080f;
        window.setPosition((screenW - width) * 0.5f, (screenH - height) * 0.5f);
        return window;
    }

    /**
     * Resolve type via {@link artframework.c1.C1NodeFactories} and inflate.
     */
    public static Actor buildNode(
            String windowId, UiNode node, Skin skin, Runnable onClose, float scale) {
        if (node == null) {
            return null;
        }
        C1NodeFactory factory = C1NodeFactories.global().get(node.type);
        if (factory == null) {
            throw new IllegalArgumentException(
                    "no C1 factory for type: " + node.type + " id=" + node.id);
        }
        return factory.create(node, new C1NodeContext(windowId, skin, onClose, scale));
    }

    /**
     * Built-in inflate path used by registered builtin factories.
     */
    public static Actor inflateBuiltin(UiNode node, C1NodeContext context) {
        if (node == null || context == null) {
            return null;
        }
        String windowId = context.windowId;
        Skin skin = context.skin;
        Runnable onClose = context.onClose;
        float scale = context.scale;
        if (UiTypes.FRAGMENT.equals(node.type)) {
            Table t = new Table(skin);
            for (UiNode c : node.children) {
                Actor a = buildNode(windowId, c, skin, onClose, scale);
                if (a != null) {
                    t.add(a).growX().row();
                }
            }
            return t;
        }
        if (UiTypes.COL.equals(node.type)
                || UiTypes.PANEL.equals(node.type)
                || UiTypes.GLASS.equals(node.type)
                || UiTypes.SCROLL.equals(node.type)
                || UiTypes.MARGIN.equals(node.type)
                || UiTypes.CENTER.equals(node.type)
                || UiTypes.TABS.equals(node.type)) {
            if (UiTypes.TABS.equals(node.type)) {
                return buildTabs(windowId, node, skin, onClose, scale);
            }
            return buildCol(windowId, node, skin, onClose, scale);
        }
        if (UiTypes.ROW.equals(node.type)) {
            return buildRow(windowId, node, skin, onClose, scale);
        }
        if (UiTypes.GRID.equals(node.type)) {
            return buildGrid(windowId, node, skin, onClose, scale);
        }
        if (UiTypes.STACK.equals(node.type)) {
            Stack stack = new Stack();
            for (UiNode c : node.children) {
                Actor a = buildNode(windowId, c, skin, onClose, scale);
                if (a != null) {
                    stack.add(a);
                }
            }
            return stack;
        }
        if (UiTypes.LABEL.equals(node.type)) {
            return new Label(node.propString("text", ""), skin);
        }
        if (UiTypes.BUTTON.equals(node.type)) {
            return buildButton(windowId, node, skin, onClose, scale);
        }
        if (UiTypes.SLIDER.equals(node.type)) {
            return buildSlider(windowId, node, skin, scale);
        }
        if (UiTypes.HITAREA.equals(node.type)) {
            return buildHitArea(windowId, node, skin, scale);
        }
        if (UiTypes.TEXTFIELD.equals(node.type)) {
            String text = node.propString("text", "");
            WidgetSession session = WidgetSessions.get(windowId);
            if (session != null && session.hasTextField(node.id)) {
                text = session.getText(node.id);
            }
            String ph = node.propString("placeholder", "");
            TextField field = new TextField(text, skin);
            if (!ph.isEmpty()) {
                field.setMessageText(ph);
            }
            final String fieldId = node.id;
            field.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    TextField input = (TextField) actor;
                    WidgetSession current = WidgetSessions.get(windowId);
                    if (current != null && current.hasTextField(fieldId)) {
                        current.setText(fieldId, input.getText());
                    }
                    ArtFramework.ops().setText(windowId, fieldId, input.getText());
                }
            });
            return field;
        }
        if (UiTypes.CHECKBOX.equals(node.type)) {
            return buildCheckbox(windowId, node, skin, scale);
        }
        if (UiTypes.PROGRESS.equals(node.type)) {
            float v = node.propFloat("value", node.propFloat("progress", 0f));
            WidgetSession session = WidgetSessions.get(windowId);
            if (session != null && session.hasProgress(node.id)) {
                v = session.getProgress(node.id);
            }
            return new Label(String.format("%.0f%%", Float.valueOf(v * 100f)), skin);
        }
        if (ArtNodeTypes.ANIMATION_PLAYER.equals(node.type)
                || ArtNodeTypes.SKELETON.equals(node.type)) {
            // Behavior / non-layout visual: invisible placeholder for scene2d tree integrity.
            return new Actor();
        }
        if (ArtNodeTypes.SHADER_EFFECT.equals(node.type)) {
            Table wrap = new Table(skin);
            for (UiNode c : node.children) {
                Actor a = buildNode(windowId, c, skin, onClose, scale);
                if (a != null) {
                    wrap.add(a).grow();
                }
            }
            return wrap;
        }
        throw new IllegalArgumentException("unhandled builtin type: " + node.type);
    }

    private static Actor buildCheckbox(
            final String windowId, final UiNode node, Skin skin, float scale) {
        boolean checked = node.propBool("checked", false);
        WidgetSession session = WidgetSessions.get(windowId);
        if (session != null && session.hasCheckbox(node.id)) {
            checked = session.getChecked(node.id);
        }
        String mark = checked ? "[x] " : "[ ] ";
        final TextButton box = new TextButton(mark + node.propString("text", ""), skin);
        final String id = node.id;
        box.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ArtFramework.ops().toggleCheckbox(windowId, id);
                WidgetSession s = WidgetSessions.get(windowId);
                if (s != null && s.hasCheckbox(id)) {
                    String m = s.getChecked(id) ? "[x] " : "[ ] ";
                    box.setText(m + node.propString("text", ""));
                }
            }
        });
        return box;
    }

    private static Table buildCol(
            String windowId, UiNode node, Skin skin, Runnable onClose, float scale) {
        Table t = new Table(skin);
        float pad = node.layout.pad * scale;
        float gap = node.layout.gap * scale;
        if (pad > 0f) {
            t.pad(pad);
        }
        for (int i = 0; i < node.children.size(); i++) {
            Actor a = buildNode(windowId, node.children.get(i), skin, onClose, scale);
            if (a == null) {
                continue;
            }
            if (node.children.get(i).layout.grow) {
                t.add(a).growX().expandY().fillY().padBottom(gap).row();
            } else {
                t.add(a).growX().padBottom(gap).row();
            }
        }
        return t;
    }

    private static Table buildRow(
            String windowId, UiNode node, Skin skin, Runnable onClose, float scale) {
        Table t = new Table(skin);
        float pad = node.layout.pad * scale;
        float gap = node.layout.gap * scale;
        if (pad > 0f) {
            t.pad(pad);
        }
        for (int i = 0; i < node.children.size(); i++) {
            UiNode c = node.children.get(i);
            Actor a = buildNode(windowId, c, skin, onClose, scale);
            if (a == null) {
                continue;
            }
            if (c.layout.grow) {
                t.add(a).growX().expandX().fillX().padRight(gap);
            } else {
                t.add(a).padRight(gap);
            }
        }
        return t;
    }

    private static Table buildGrid(
            String windowId, UiNode node, Skin skin, Runnable onClose, float scale) {
        Table t = new Table(skin);
        float pad = node.layout.pad * scale;
        float gap = node.layout.gap * scale;
        int columns = Math.max(1, node.propInt("columns", 2));
        if (pad > 0f) {
            t.pad(pad);
        }
        int col = 0;
        for (int i = 0; i < node.children.size(); i++) {
            Actor a = buildNode(windowId, node.children.get(i), skin, onClose, scale);
            if (a == null) {
                continue;
            }
            if (node.children.get(i).layout.grow) {
                t.add(a).grow().pad(gap);
            } else {
                t.add(a).pad(gap);
            }
            col++;
            if (col >= columns) {
                t.row();
                col = 0;
            }
        }
        return t;
    }

    private static Actor buildTabs(
            String windowId, UiNode node, Skin skin, Runnable onClose, float scale) {
        int active = Math.max(0, node.propInt("active", 0));
        if (node.children.isEmpty()) {
            return new Table(skin);
        }
        if (active >= node.children.size()) {
            active = node.children.size() - 1;
        }
        return buildNode(windowId, node.children.get(active), skin, onClose, scale);
    }

    private static TextButton buildButton(
            final String windowId,
            final UiNode node,
            Skin skin,
            final Runnable onClose,
            float scale) {
        final String buttonId = node.id;
        TextButton button = new TextButton(node.propString("text", ""), skin);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if ("close".equals(buttonId) && onClose != null) {
                    onClose.run();
                    return;
                }
                UiOps ops = ArtFramework.ops();
                // Prefer registered handler; clickButton validates layout
                try {
                    ops.clickButton(windowId, buttonId);
                } catch (RuntimeException ignored) {
                }
            }
        });
        if (node.layout.hasHeight()) {
            button.setHeight(node.layout.height * scale);
        } else {
            button.setHeight(40f * scale);
        }
        return button;
    }

    private static Actor buildSlider(
            final String windowId, final UiNode node, Skin skin, float scale) {
        float min = node.propFloat("min", 0f);
        float max = node.propFloat("max", 1f);
        if (max < min) {
            float t = min;
            min = max;
            max = t;
        }
        float step = (max - min) / 100f;
        if (step <= 0f) {
            step = 0.01f;
        }
        float value = min;
        WidgetSession session = WidgetSessions.get(windowId);
        if (session != null && session.hasSlider(node.id)) {
            value = session.getSlider(node.id);
        } else {
            value = node.propFloat("value", min);
        }
        final Slider slider = new Slider(min, max, step, false, skin);
        slider.setValue(value);
        final String sliderId = node.id;
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ArtFramework.ops().setSlider(windowId, sliderId, slider.getValue());
            }
        });
        Table wrap = new Table(skin);
        wrap.add(slider).growX().height(28f * scale);
        return wrap;
    }

    private static Actor buildHitArea(
            final String windowId, final UiNode node, Skin skin, float scale) {
        TextButton area = new TextButton("", skin);
        float w = node.layout.hasWidth() ? node.layout.width * scale : 120f * scale;
        float h = node.layout.hasHeight() ? node.layout.height * scale : 64f * scale;
        area.setSize(w, h);
        final String hitId = node.id;
        area.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                ArtFramework.ops().clickHitArea(windowId, hitId);
            }
        });
        return area;
    }
}
