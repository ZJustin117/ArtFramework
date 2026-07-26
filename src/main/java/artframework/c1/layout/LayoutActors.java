package artframework.c1.layout;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.megacrit.cardcrawl.core.Settings;

/**
 * Builds scene2d actors from a {@link LayoutNode} tree using {@link Skin} (typically StsSkin).
 */
public final class LayoutActors {

    private LayoutActors() {}

    public static Actor toActor(LayoutNode root, Skin skin, final Runnable onClose) {
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

        Window window = new Window(root.title, skin);
        window.setModal(true);
        window.setMovable(true);
        window.setSize(width, height);
        window.defaults().pad(8f * scale);

        for (LayoutNode child : root.children) {
            if (child.type == LayoutNode.Type.LABEL) {
                window.add(new Label(child.text, skin)).growX().padBottom(6f * scale).row();
            } else if (child.type == LayoutNode.Type.BUTTON) {
                TextButton button = new TextButton(child.text, skin);
                if ("close".equals(child.id) && onClose != null) {
                    button.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            onClose.run();
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
