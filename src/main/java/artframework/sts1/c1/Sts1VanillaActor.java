package artframework.sts1.c1;

import artframework.api.ArtFramework;
import artframework.assets.AssetResolveResult;
import artframework.component.UiNode;
import artframework.sts1.assets.Sts1AssetMaterializer;
import artframework.sts1.assets.Sts1NodeResources;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/** STS1-only semantic node actor backed by the current HostAssets resolve result. */
final class Sts1VanillaActor extends Actor {

    private final String windowId;
    private final UiNode node;
    private boolean hovered;

    Sts1VanillaActor(final String windowId, final UiNode node, float scale) {
        this.windowId = windowId;
        this.node = node;
        float width = node.layout.hasWidth() ? node.layout.width * scale : 160f * scale;
        float height = node.layout.hasHeight() ? node.layout.height * scale : 56f * scale;
        setSize(width, height);
        if (node.id != null && !node.id.isEmpty()) {
            setName(node.id);
        }
        if (node.declaresSignal(artframework.core.SignalNames.PRESSED)) {
            addListener(new ClickListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    hovered = true;
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    hovered = false;
                }

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    ArtFramework.ops().clickButton(windowId, node.id);
                }
            });
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        String key = hovered ? Sts1NodeResources.hoverResource(node) : "";
        if (key.isEmpty()) {
            key = Sts1NodeResources.primaryResource(node);
        }
        AssetResolveResult result = ArtFramework.assets().resolve(key);
        Texture texture = Sts1AssetMaterializer.resolveTexture(result);
        Color color = getColor();
        if (texture != null) {
            batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
            batch.draw(texture, getX(), getY(), getWidth(), getHeight());
        } else {
            batch.setColor(0.18f, 0.18f, 0.18f, 0.75f * parentAlpha);
            batch.draw(com.megacrit.cardcrawl.helpers.ImageMaster.WHITE_SQUARE_IMG,
                    getX(), getY(), getWidth(), getHeight());
        }
        batch.setColor(Color.WHITE);
    }
}
