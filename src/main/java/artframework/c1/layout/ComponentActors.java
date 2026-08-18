package artframework.c1.layout;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
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
import artframework.presentation.ControlValueComponent;

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

        artframework.c1.host.EffectTargetActors.clearWindow(windowId);

        float scale = Settings.scale > 0f ? Settings.scale : 1f;
        float width = root.layout.hasWidth() ? root.layout.width * scale : 400f * scale;
        float height = root.layout.hasHeight() ? root.layout.height * scale : 240f * scale;
        String title = root.propString("title", windowId);

        // Empty scene2d title — STS fonts break Window title glyphs; draw title as StsTextActor.
        Window window = new Window("", skin);
        window.setName(root.id != null && !root.id.isEmpty() ? root.id : windowId);
        window.setModal(true);
        window.setMovable(true);
        float pad = root.layout.pad > 0f ? root.layout.pad * scale : 8f * scale;
        window.defaults().pad(pad);
        if (title != null && !title.isEmpty()) {
            StsTextActor titleActor = new StsTextActor(title, false);
            titleActor.setHeight(32f * scale);
            // Dedicated FX key so title gets pack label/window chrome without stealing widget ids.
            try {
                titleActor.setName("__art_title");
            } catch (Throwable ignored) {
            }
            artframework.c1.host.EffectTargetActors.put(windowId, "__art_title", titleActor);
            window.add(titleActor).growX().padBottom(8f * scale).row();
        }

        String rootKey =
                root.id != null && !root.id.isEmpty()
                        ? root.id
                        : windowId;
        for (int ci = 0; ci < root.children.size(); ci++) {
            UiNode child = root.children.get(ci);
            if (ArtNodeTypes.ANIMATION_PLAYER.equals(child.type)
                    || ArtNodeTypes.SKELETON.equals(child.type)) {
                buildNode(windowId, child, skin, onClose, scale, rootKey, ci);
                continue;
            }
            Actor a = buildNode(windowId, child, skin, onClose, scale, rootKey, ci);
            if (a != null) {
                // Labels: left with min width so FX frame is visible (not hairline on "Hello").
                if (UiTypes.LABEL.equals(child.type)) {
                    float minW = Math.max(a.getWidth() + 24f * scale, 120f * scale);
                    if (child.layout.hasHeight()) {
                        window.add(a)
                                .left()
                                .minWidth(minW)
                                .height(child.layout.height * scale)
                                .padBottom(6f * scale)
                                .row();
                    } else {
                        window.add(a).left().minWidth(minW).padBottom(6f * scale).row();
                    }
                } else if (child.layout.hasHeight()) {
                    window.add(a)
                            .growX()
                            .height(child.layout.height * scale)
                            .padBottom(6f * scale)
                            .row();
                } else {
                    window.add(a).growX().padBottom(6f * scale).row();
                }
            }
        }

        // Fixed outer size like legacy LayoutActors — pack first then enforce min client size.
        window.pack();
        window.setSize(Math.max(window.getWidth(), width), Math.max(window.getHeight(), height));
        float screenW = Settings.WIDTH > 0 ? Settings.WIDTH : 1920f;
        float screenH = Settings.HEIGHT > 0 ? Settings.HEIGHT : 1080f;
        window.setPosition(
                (screenW - window.getWidth()) * 0.5f, (screenH - window.getHeight()) * 0.5f);
        // Register window root for FX (same key as RenderHost window target uses tree root id).
        String winFxKey =
                root.id != null && !root.id.isEmpty() ? root.id : windowId;
        artframework.c1.host.EffectTargetActors.put(windowId, winFxKey, window);
        return window;
    }

    /**
     * Resolve type via {@link artframework.c1.C1NodeFactories} and inflate.
     */
    public static Actor buildNode(
            String windowId, UiNode node, Skin skin, Runnable onClose, float scale) {
        return buildNode(windowId, node, skin, onClose, scale, "", 0);
    }

    public static Actor buildNode(
            String windowId,
            UiNode node,
            Skin skin,
            Runnable onClose,
            float scale,
            String parentKey,
            int index) {
        if (node == null) {
            return null;
        }
        C1NodeFactory factory = C1NodeFactories.global().get(node.type);
        if (factory == null) {
            throw new IllegalArgumentException(
                    "no C1 factory for type: " + node.type + " id=" + node.id);
        }
        String key = artframework.component.LayoutEngine.effectKey(node, parentKey, index);
        Actor created =
                factory.create(
                        node,
                        new C1NodeContext(windowId, skin, onClose, scale, parentKey, index));
        nameActor(windowId, created, key);
        return created;
    }

    /** Name actors so StageHost can sync RenderHost effect targets to stage coordinates. */
    static Actor nameActor(Actor actor, UiNode node) {
        if (node == null) {
            return actor;
        }
        String key = node.id != null && !node.id.isEmpty() ? node.id : null;
        return nameActor(null, actor, key);
    }

    static Actor nameActor(String windowId, Actor actor, String effectKey) {
        if (actor != null && effectKey != null && !effectKey.isEmpty()) {
            try {
                actor.setName(effectKey);
            } catch (Throwable ignored) {
            }
            if (windowId != null && !windowId.isEmpty()) {
                artframework.c1.host.EffectTargetActors.put(windowId, effectKey, actor);
            }
        }
        return actor;
    }

    static Actor nameActor(Actor actor, String effectKey) {
        return nameActor(null, actor, effectKey);
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
        String myKey =
                artframework.component.LayoutEngine.effectKey(
                        node, context.parentEffectKey, context.siblingIndex);
        if (UiTypes.FRAGMENT.equals(node.type)
                || ArtNodeTypes.PRESENT_PROFILE.equals(node.type)) {
            Table t = new Table(skin);
            for (int i = 0; i < node.children.size(); i++) {
                Actor a =
                        buildNode(
                                windowId, node.children.get(i), skin, onClose, scale, myKey, i);
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
                return buildTabs(windowId, node, skin, onClose, scale, myKey);
            }
            return buildCol(windowId, node, skin, onClose, scale, myKey);
        }
        if (UiTypes.ROW.equals(node.type)) {
            return buildRow(windowId, node, skin, onClose, scale, myKey);
        }
        if (UiTypes.GRID.equals(node.type)) {
            return buildGrid(windowId, node, skin, onClose, scale, myKey);
        }
        if (UiTypes.STACK.equals(node.type)) {
            Stack stack = new Stack();
            for (int i = 0; i < node.children.size(); i++) {
                Actor a =
                        buildNode(
                                windowId, node.children.get(i), skin, onClose, scale, myKey, i);
                if (a != null) {
                    stack.add(a);
                }
            }
            return stack;
        }
        if (UiTypes.LABEL.equals(node.type)) {
            return buildStsLabel(node, scale);
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
            String text = String.valueOf(controlValue(windowId, node.id, node.propString("text", "")));
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
                    ArtFramework.ops().setText(windowId, fieldId, input.getText());
                }
            });
            return field;
        }
        if (UiTypes.CHECKBOX.equals(node.type)) {
            return buildCheckbox(windowId, node, skin, scale);
        }
        if (UiTypes.PROGRESS.equals(node.type)) {
            float v = number(controlValue(windowId, node.id,
                    Float.valueOf(node.propFloat("value", node.propFloat("progress", 0f)))));
            return buildStsLabelText(String.format("%.0f%%", Float.valueOf(v * 100f)), scale, false);
        }
        if (ArtNodeTypes.ANIMATION_PLAYER.equals(node.type)
                || ArtNodeTypes.SKELETON.equals(node.type)) {
            // Behavior / non-layout visual: invisible placeholder for scene2d tree integrity.
            return new Actor();
        }
        if (ArtNodeTypes.SHADER_EFFECT.equals(node.type)) {
            Table wrap = new Table(skin);
            for (int i = 0; i < node.children.size(); i++) {
                Actor a =
                        buildNode(
                                windowId, node.children.get(i), skin, onClose, scale, myKey, i);
                if (a != null) {
                    wrap.add(a).grow();
                }
            }
            return wrap;
        }
        throw new IllegalArgumentException("unhandled builtin type: " + node.type);
    }

    /** Panel / glass: theme-driven background (border via LightwaveEffect on panel target). */
    static void applyPanelChrome(Table table, UiNode node, Skin skin) {
        if (table == null || node == null || skin == null) {
            return;
        }
        if (!UiTypes.PANEL.equals(node.type) && !UiTypes.GLASS.equals(node.type)) {
            return;
        }
        try {
            if (skin.has("panel-bg", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                table.setBackground(skin.getDrawable("panel-bg"));
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Declared size is applied by the parent cell (window.add width/height), not by freezing the
     * Table before children layout — early setSize collapses labels/buttons.
     */
    static void applyDeclaredSize(Table table, UiNode node, float scale) {
        // no-op: parent cell + pack handle size; FX bounds come from stage actor after layout
    }

    /** Apply declared opacity prop (0..1) so animation_player tweens are visible. */
    static Actor applyOpacity(Actor actor, UiNode node) {
        if (actor == null || node == null) {
            return actor;
        }
        if (!node.props.containsKey("opacity")) {
            return actor;
        }
        float o = node.propFloat("opacity", 1f);
        if (o < 0f) {
            o = 0f;
        }
        if (o > 1f) {
            o = 1f;
        }
        try {
            com.badlogic.gdx.graphics.Color c = actor.getColor();
            actor.setColor(c.r, c.g, c.b, o);
        } catch (Throwable ignored) {
        }
        return actor;
    }

    private static Actor buildCheckbox(
            final String windowId, final UiNode node, Skin skin, float scale) {
        boolean checked = node.propBool("checked", false);
        checked = Boolean.TRUE.equals(controlValue(windowId, node.id, Boolean.valueOf(checked)));
        final String id = node.id;
        final String base = node.propString("text", "");
        String mark = checked ? "[x] " : "[ ] ";
        final StsTextActor caption = new StsTextActor(mark + base, false);
        Table box = new Table(skin);
        try {
            if (skin.has("default", TextButton.TextButtonStyle.class)) {
                TextButton.TextButtonStyle st = skin.get(TextButton.TextButtonStyle.class);
                if (st != null && st.up != null) {
                    box.setBackground(st.up);
                }
            }
        } catch (Throwable ignored) {
        }
        box.add(caption).growX().pad(6f * scale);
        box.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                ArtFramework.ops().toggleCheckbox(windowId, id);
                boolean current = Boolean.TRUE.equals(controlValue(windowId, id, Boolean.FALSE));
                caption.setText((current ? "[x] " : "[ ] ") + base);
            }
        });
        return box;
    }

    private static Table buildCol(
            String windowId,
            UiNode node,
            Skin skin,
            Runnable onClose,
            float scale,
            String parentKey) {
        Table t = new Table(skin);
        applyPanelChrome(t, node, skin);
        applyDeclaredSize(t, node, scale);
        float pad = node.layout.pad * scale;
        float gap = node.layout.gap * scale;
        if (pad > 0f) {
            t.pad(pad);
        }
        for (int i = 0; i < node.children.size(); i++) {
            Actor a =
                    buildNode(windowId, node.children.get(i), skin, onClose, scale, parentKey, i);
            if (a == null) {
                continue;
            }
            UiNode child = node.children.get(i);
            if (UiTypes.BUTTON.equals(child.type) || UiTypes.SLIDER.equals(child.type)) {
                float bh =
                        child.layout.hasHeight()
                                ? child.layout.height * scale
                                : (UiTypes.SLIDER.equals(child.type) ? 40f * scale : 44f * scale);
                t.add(a).growX().height(bh).padBottom(gap).row();
            } else if (child.layout.grow) {
                t.add(a).growX().expandY().fillY().padBottom(gap).row();
            } else {
                t.add(a).growX().padBottom(gap).row();
            }
        }
        return t;
    }

    private static Table buildRow(
            String windowId,
            UiNode node,
            Skin skin,
            Runnable onClose,
            float scale,
            String parentKey) {
        Table t = new Table(skin);
        float pad = node.layout.pad * scale;
        float gap = node.layout.gap * scale;
        if (pad > 0f) {
            t.pad(pad);
        }
        for (int i = 0; i < node.children.size(); i++) {
            UiNode c = node.children.get(i);
            Actor a = buildNode(windowId, c, skin, onClose, scale, parentKey, i);
            if (a == null) {
                continue;
            }
            float bh =
                    c.layout.hasHeight() ? c.layout.height * scale : 44f * scale;
            if (c.layout.grow) {
                t.add(a).growX().expandX().fillX().height(bh).minWidth(80f * scale).padRight(gap);
            } else {
                t.add(a).height(bh).minWidth(80f * scale).padRight(gap);
            }
        }
        return t;
    }

    private static Table buildGrid(
            String windowId,
            UiNode node,
            Skin skin,
            Runnable onClose,
            float scale,
            String parentKey) {
        Table t = new Table(skin);
        float pad = node.layout.pad * scale;
        float gap = node.layout.gap * scale;
        int columns = Math.max(1, node.propInt("columns", 2));
        if (pad > 0f) {
            t.pad(pad);
        }
        int col = 0;
        for (int i = 0; i < node.children.size(); i++) {
            Actor a =
                    buildNode(windowId, node.children.get(i), skin, onClose, scale, parentKey, i);
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
            String windowId,
            UiNode node,
            Skin skin,
            Runnable onClose,
            float scale,
            String parentKey) {
        int active = Math.max(0, node.propInt("active", 0));
        if (node.children.isEmpty()) {
            return new Table(skin);
        }
        if (active >= node.children.size()) {
            active = node.children.size() - 1;
        }
        return buildNode(windowId, node.children.get(active), skin, onClose, scale, parentKey, active);
    }

    private static Actor buildStsLabel(UiNode node, float scale) {
        StsTextActor a = buildStsLabelText(node.propString("text", ""), scale, false);
        if (node != null && node.id != null && !node.id.isEmpty()) {
            try {
                a.setName(node.id);
            } catch (Throwable ignored) {
            }
        }
        return a;
    }

    private static StsTextActor buildStsLabelText(String text, float scale, boolean centered) {
        StsTextActor a = new StsTextActor(text, centered);
        a.setHeight(28f * scale);
        return a;
    }

    /**
     * Hit target + chrome via Table; label via FontHelper (not scene2d TextButton glyphs).
     */
    private static Actor buildButton(
            final String windowId,
            final UiNode node,
            Skin skin,
            final Runnable onClose,
            float scale) {
        final String buttonId = node.id;
        final String label = node.propString("text", buttonId != null ? buttonId : "OK");
        float bh = node.layout.hasHeight() ? node.layout.height * scale : 48f * scale;

        Table button = new Table(skin);
        try {
            if (skin.has("default", TextButton.TextButtonStyle.class)) {
                TextButton.TextButtonStyle st = skin.get(TextButton.TextButtonStyle.class);
                if (st != null && st.up != null) {
                    button.setBackground(st.up);
                }
            }
        } catch (Throwable ignored) {
        }
        if (button.getBackground() == null) {
            try {
                if (skin.has("panel-bg", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                    button.setBackground(skin.getDrawable("panel-bg"));
                }
            } catch (Throwable ignored) {
            }
        }
        StsTextActor caption = new StsTextActor(label, true);
        caption.setHeight(bh - 8f * scale);
        button.add(caption).expand().fill().pad(6f * scale);
        button.setHeight(bh);
        // Do not setName here — buildNode nameActor assigns effectKey (id or path).
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleButton(windowId, buttonId, onClose);
            }
        });
        // Pressed visual: darken slightly
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // no-op; Table has no ChangeEvent by default
            }
        });
        return button;
    }

    private static long lastButtonMs;
    private static String lastButtonKey = "";

    private static void handleButton(String windowId, String buttonId, Runnable onClose) {
        // Debounce double fire from ChangeListener + ClickListener.
        long now = System.currentTimeMillis();
        String key = windowId + "/" + buttonId;
        if (key.equals(lastButtonKey) && now - lastButtonMs < 250L) {
            return;
        }
        lastButtonKey = key;
        lastButtonMs = now;
        if ("close".equals(buttonId) && onClose != null) {
            try {
                ArtFramework.ops().clickButton(windowId, buttonId);
            } catch (RuntimeException ignored) {
            }
            artframework.render.LightwaveControls.closeWithFx(windowId, onClose);
            return;
        }
        try {
            ArtFramework.ops().clickButton(windowId, buttonId);
        } catch (RuntimeException ignored) {
        }
        // Pulse / other behaviors: declarative connections → UiActions (see lightwave_demo).
    }

    private static Actor buildSlider(
            final String windowId, final UiNode node, Skin skin, float scale) {
        float[] bounds = controlBounds(windowId, node.id);
        float min = bounds[0];
        float max = bounds[1];
        float step = (max - min) / 100f;
        if (step <= 0f) {
            step = 0.01f;
        }
        float value = number(controlValue(windowId, node.id,
                Float.valueOf(node.propFloat("value", min))));
        final Slider slider = new Slider(min, max, step, false, skin);
        slider.setValue(value);
        final String sliderId = node.id;
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float v = slider.getValue();
                ArtFramework.ops().setSlider(windowId, sliderId, v);
            }
        });
        Table wrap = new Table(skin);
        if (node.id != null && !node.id.isEmpty()) {
            wrap.setName(node.id);
        }
        float sh = 40f * scale;
        wrap.add(slider).growX().height(sh).minHeight(sh).prefHeight(sh).pad(4f * scale);
        wrap.setHeight(sh + 8f * scale);
        return wrap;
    }

    private static Object controlValue(String windowId, String id, Object fallback) {
        artframework.presentation.PresentationContext context =
                artframework.presentation.PresentationRuntime.context(windowId);
        artframework.ecs.EntityId entity = artframework.presentation.PresentationRuntime.find(context, id);
        if (entity == null) return fallback;
        ControlValueComponent value = context.world().get(entity, ControlValueComponent.class);
        return value != null && value.value != null ? value.value : fallback;
    }

    private static float[] controlBounds(String windowId, String id) {
        artframework.presentation.PresentationContext context =
                artframework.presentation.PresentationRuntime.context(windowId);
        artframework.ecs.EntityId entity = artframework.presentation.PresentationRuntime.find(context, id);
        if (entity != null) {
            artframework.presentation.ControlBoundsComponent bounds = context.world().get(
                    entity, artframework.presentation.ControlBoundsComponent.class);
            if (bounds != null) return new float[] {bounds.min, bounds.max};
        }
        return new float[] {0f, 1f};
    }

    private static float number(Object value) {
        if (value instanceof Number) return ((Number) value).floatValue();
        try { return Float.parseFloat(String.valueOf(value)); }
        catch (RuntimeException ignored) { return 0f; }
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
