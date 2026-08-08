package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.AssetResolveResult;
import artframework.sts1.assets.Sts1AssetMaterializer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** Private STS1 renderer bridge shared by C2 surfaces and semantic C1 nodes. */
final class Sts1VanillaDraw {

    private Sts1VanillaDraw() {}

    static boolean draw(SpriteBatch batch, String resourceId, float x, float y, float w, float h) {
        if (batch == null || resourceId == null || resourceId.isEmpty() || w <= 0f || h <= 0f) {
            return false;
        }
        AssetResolveResult result = ArtFramework.assets().resolve(resourceId);
        Texture texture = Sts1AssetMaterializer.resolveTexture(result);
        if (texture == null) {
            return false;
        }
        Color previous = batch.getColor();
        try {
            batch.draw(texture, x, y, w, h);
            return true;
        } finally {
            batch.setColor(previous);
        }
    }

    static boolean drawTinted(
            SpriteBatch batch, String resourceId, float x, float y, float w, float h, Color color) {
        if (color == null) {
            return draw(batch, resourceId, x, y, w, h);
        }
        Color previous = batch.getColor();
        batch.setColor(color);
        try {
            return draw(batch, resourceId, x, y, w, h);
        } finally {
            batch.setColor(previous);
        }
    }
}
