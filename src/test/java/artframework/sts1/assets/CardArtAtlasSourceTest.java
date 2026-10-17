package artframework.sts1.assets;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * U04. The STS1 {@code cards} live atlas adapter: {@code atlas("cards")} exposes the PRIVATE STATIC
 * {@code AbstractCard.cardAtlas} — the {@code cards/cards.atlas} page whose region names ARE the
 * native card art urls ({@code red/attack/strike}) — so the SELECT card portrait descriptor
 * {@code cards#<artUrl>} can resolve through {@link Sts1AtlasMaterializer}. Every other key (and an
 * unset/absent host field) fails open to {@code null} without throwing.
 *
 * <p>{@code AbstractCard}'s static initializer reads
 * {@code CardCrawlGame.languagePack.getUIString("SingleCardViewPopup")} and {@code
 * "AbstractCard"}, so off-game it throws {@code ExceptionInInitializerError} before the
 * {@code cardAtlas} field can be seeded. The fixture therefore installs a minimal
 * {@link LocalizedStrings} language pack (allocated without its constructor, which needs
 * {@code Settings.language} and game files) whose {@code ui} map answers those two keys with empty
 * {@link UIStrings}. The language pack, its {@code ui} map and the {@code cardAtlas} field are all
 * restored, so no test-order leakage remains.
 */
public class CardArtAtlasSourceTest {

    private static final String CARDS_KEY = "cards";

    private static Object previousLanguagePack;
    private static boolean languagePackSeeded;
    private static Object previousUiMap;
    private static boolean uiMapSeeded;

    private Object previousCardAtlas;
    private boolean cardAtlasSeeded;

    @BeforeClass
    public static void installHeadlessLanguagePack() {
        if (CardCrawlGame.languagePack == null) {
            previousLanguagePack = null;
            languagePackSeeded = true;
            setStaticField(CardCrawlGame.class, "languagePack", headlessLanguagePack());
        }
    }

    @AfterClass
    public static void restoreLanguagePack() {
        if (uiMapSeeded) {
            setStaticField(LocalizedStrings.class, "ui", previousUiMap);
            uiMapSeeded = false;
        }
        if (languagePackSeeded) {
            setStaticField(CardCrawlGame.class, "languagePack", previousLanguagePack);
            languagePackSeeded = false;
        }
    }

    @After
    public void restoreCardAtlas() {
        if (cardAtlasSeeded) {
            setStaticField(AbstractCard.class, "cardAtlas", previousCardAtlas);
            cardAtlasSeeded = false;
        }
    }

    @Test
    public void cardsKeyReturnsAbstractCardCardAtlas() {
        TextureAtlas atlas = new TextureAtlas();
        seedCardAtlas(atlas);

        assertSame("atlas(\"cards\") must expose AbstractCard.cardAtlas",
                atlas, new CardArtAtlasSource().atlas(CARDS_KEY));
    }

    @Test
    public void otherAndBlankKeysReturnNull() {
        seedCardAtlas(new TextureAtlas());
        CardArtAtlasSource source = new CardArtAtlasSource();

        assertNull("the cardui page has its own adapter", source.atlas("cardui"));
        assertNull(source.atlas("other"));
        assertNull(source.atlas(null));
        assertNull(source.atlas(""));
        assertNull(source.atlas("   "));
        assertNull("atlas keys are exact, not case-folded", source.atlas("Cards"));
    }

    @Test
    public void offGameUnsetFieldReturnsNullWithoutThrowing() {
        seedCardAtlas(null);

        assertNull(new CardArtAtlasSource().atlas(CARDS_KEY));
    }

    private void seedCardAtlas(TextureAtlas value) {
        if (!cardAtlasSeeded) {
            previousCardAtlas = getStaticField(AbstractCard.class, "cardAtlas");
            cardAtlasSeeded = true;
        }
        setStaticField(AbstractCard.class, "cardAtlas", value);
    }

    private static LocalizedStrings headlessLanguagePack() {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            LocalizedStrings pack =
                    (LocalizedStrings) unsafe.allocateInstance(LocalizedStrings.class);

            if (!uiMapSeeded) {
                previousUiMap = getStaticField(LocalizedStrings.class, "ui");
                uiMapSeeded = true;
            }
            UIStrings empty = new UIStrings();
            empty.TEXT = new String[] {"", "", "", ""};
            Map<String, UIStrings> ui = new HashMap<String, UIStrings>();
            ui.put("SingleCardViewPopup", empty);
            ui.put("AbstractCard", empty);
            setStaticField(LocalizedStrings.class, "ui", ui);
            return pack;
        } catch (Exception failure) {
            throw new AssertionError("could not build a headless language pack", failure);
        }
    }

    private static Object getStaticField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(null);
        } catch (Exception failure) {
            throw new AssertionError("could not read " + owner.getName() + "." + name, failure);
        }
    }

    private static void setStaticField(Class<?> owner, String name, Object value) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (Exception failure) {
            throw new AssertionError("could not set " + owner.getName() + "." + name, failure);
        }
    }
}
