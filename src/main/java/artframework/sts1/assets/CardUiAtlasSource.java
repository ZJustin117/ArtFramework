package artframework.sts1.assets;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;

import java.lang.reflect.Field;

/**
 * Small STS adapter the backend installs once: exposes the native {@code cardui} atlas
 * ({@code ImageMaster.cardUiAtlas}) to {@link Sts1AtlasMaterializer}'s live-atlas seam.
 *
 * <p>The card base-background page lives in {@code com.megacrit.cardcrawl.helpers.ImageMaster}'s
 * static {@code cardUiAtlas} field. That field is {@code private} in the host jar, so it is read
 * reflectively here; {@code atlas("cardui")} borrows it so a caller can resolve
 * {@code cardui#512/bg_&lt;type&gt;_&lt;color&gt;}. Any other key (or a null/blank key) returns
 * {@code null}.
 *
 * <p><strong>Fail-open is mandatory.</strong> The whole reflection read is wrapped in a
 * {@code try/catch (Throwable)}: off-game (unset field, absent class, not yet initialized, or a
 * security/access failure) this returns {@code null} rather than throwing, so a missing host atlas
 * never breaks the caller. The materializer owns caching; this adapter holds no state of its own.
 */
public final class CardUiAtlasSource implements Sts1AtlasMaterializer.LiveAtlasSource {

    private static final String CARD_UI_KEY = "cardui";
    private static final String IMAGE_MASTER_CLASS = "com.megacrit.cardcrawl.helpers.ImageMaster";
    private static final String CARD_UI_ATLAS_FIELD = "cardUiAtlas";

    @Override
    public TextureAtlas atlas(String atlasKey) {
        if (!CARD_UI_KEY.equals(atlasKey)) {
            return null;
        }
        try {
            Field field = Class.forName(IMAGE_MASTER_CLASS).getDeclaredField(CARD_UI_ATLAS_FIELD);
            field.setAccessible(true);
            Object value = field.get(null);
            return value instanceof TextureAtlas ? (TextureAtlas) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
