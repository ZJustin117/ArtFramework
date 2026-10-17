package artframework.sts1.assets;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;

import java.lang.reflect.Field;

/**
 * Small STS adapter the backend installs once: exposes the native {@code cards} atlas
 * ({@code AbstractCard.cardAtlas}) to {@link Sts1AtlasMaterializer}'s live-atlas seam.
 *
 * <p>The card PORTRAIT page is {@code cards/cards.atlas}, whose region names ARE the native card
 * art urls ({@code AbstractCard.assetUrl}, for example {@code red/attack/strike}) - the exact
 * string native {@code AbstractCard} passes to {@code cardAtlas.findRegion(...)}. That page lives
 * in {@code com.megacrit.cardcrawl.cards.AbstractCard}'s static {@code cardAtlas} field. The field is
 * {@code private} in the host jar, so it is read reflectively here; {@code atlas("cards")} borrows
 * it so a caller can resolve {@code cards#red/attack/strike}. Any other key - including a
 * null/blank key and a case variant such as {@code "Cards"} (atlas keys are exact) - returns
 * {@code null}.
 *
 * <p><strong>Fail-open is mandatory.</strong> The whole reflection read is wrapped in a
 * {@code try/catch (Throwable)}: off-game (unset field, absent class, an
 * {@code ExceptionInInitializerError} from the host static initializer, or a security/access
 * failure) this returns {@code null} rather than throwing, so a missing host atlas never breaks the
 * caller. The materializer owns caching; this adapter holds no state of its own.
 */
public final class CardArtAtlasSource implements Sts1AtlasMaterializer.LiveAtlasSource {

    private static final String CARDS_KEY = "cards";
    private static final String ABSTRACT_CARD_CLASS = "com.megacrit.cardcrawl.cards.AbstractCard";
    private static final String CARD_ATLAS_FIELD = "cardAtlas";

    @Override
    public TextureAtlas atlas(String atlasKey) {
        if (!CARDS_KEY.equals(atlasKey)) {
            return null;
        }
        try {
            Field field = Class.forName(ABSTRACT_CARD_CLASS).getDeclaredField(CARD_ATLAS_FIELD);
            field.setAccessible(true);
            Object value = field.get(null);
            return value instanceof TextureAtlas ? (TextureAtlas) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
