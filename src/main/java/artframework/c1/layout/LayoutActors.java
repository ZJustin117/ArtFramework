package artframework.c1.layout;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.megacrit.cardcrawl.core.Settings;
import artframework.api.ArtFramework;
import artframework.api.UiOpResult;

/**
 * Builds scene2d actors from a {@link LayoutNode} tree using {@link Skin} (typically StsSkin).
 */
public final class LayoutActors {

    private LayoutActors() {}

    public static Actor toActor(
            final String windowId, LayoutNode root, Skin skin, final Runnable onClose) {
        if (windowId == null || windowId.isEmpty()) {
            throw new IllegalArgumentException("windowId required");
        }
        if (root == null) {
            throw new IllegalArgumentException("root required");
        }
        if (skin == null) {
            throw new IllegalArgumentException("skin required");
        }
        if (root.type != LayoutNode.Type.WINDOW) {
            throw new IllegalArgumentException("root must be window");
        }

        float scale = Settings.scale > 0f ? Settings.scale : 1f;
        float width = root.width > 0f ? root.width * scale : 400f * scale;
        float height = root.height > 0f ? root.height * scale : 240f * scale;

        Window window = new Window("", skin);
        window.setModal(true);
        window.setMovable(true);
        window.setSize(width, height);
        window.defaults().pad(8f * scale);
        if (root.title != null && !root.title.isEmpty()) {
            window.add(new StsTextActor(root.title, false)).growX().padBottom(8f * scale).row();
        }

        for (LayoutNode child : root.children) {
            if (child.type == LayoutNode.Type.LABEL) {
                window.add(new StsTextActor(child.text, false)).growX().padBottom(6f * scale).row();
            } else if (child.type == LayoutNode.Type.BUTTON) {
                final String buttonId = child.id;
                Table button = new Table(skin);
                try {
                    TextButton.TextButtonStyle st = skin.get(TextButton.TextButtonStyle.class);
                    if (st != null && st.up != null) {
                        button.setBackground(st.up);
                    }
                } catch (Throwable ignored) {
                }
                button.add(new StsTextActor(child.text != null ? child.text : "", true))
                        .expand()
                        .fill()
                        .pad(6f * scale);
                if ("close".equals(child.id) && onClose != null) {
                    button.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            UiOpResult result = ArtFramework.ops().clickButton(windowId, buttonId);
                            if (result.status != UiOpResult.Status.BLOCKED) {
                                onClose.run();
                            }
                        }
                    });
                } else if (child.id != null && !child.id.isEmpty()) {
                    button.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            ArtFramework.ops().clickButton(windowId, buttonId);
                        }
                    });
                }
                window.add(button).growX().padBottom(6f * scale).height(40f * scale).row();
            }
        }

        float screenW = Settings.WIDTH > 0 ? Settings.WIDTH : 1920f;
        float screenH = Settings.HEIGHT > 0 ? Settings.HEIGHT : 1080f;
        window.setPosition((screenW - width) * 0.5f, (screenH - height) * 0.5f);
        return window;
    }
}
