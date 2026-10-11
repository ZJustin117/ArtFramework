package artframework.sts1.assets;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * U02. The STS1 {@code cardui} live atlas adapter: {@code atlas("cardui")} exposes
 * {@code ImageMaster.cardUiAtlas} (the card base-background page source) and fails open (null,
 * never throwing) for any other key or when the host field is unset off-game.
 */
public class CardUiAtlasSourceTest {

    private Object previous;
    private boolean seeded;

    @After
    public void restoreImageMasterField() {
        if (seeded) {
            setStaticField("cardUiAtlas", previous);
            seeded = false;
        }
    }

    @Test
    public void cardUiKeyReturnsImageMasterCardUiAtlas() {
        TextureAtlas atlas = new TextureAtlas();
        seedCardUiAtlas(atlas);

        assertSame(atlas, new CardUiAtlasSource().atlas("cardui"));
    }

    @Test
    public void nonCardUiAndBlankKeysReturnNull() {
        seedCardUiAtlas(new TextureAtlas());
        CardUiAtlasSource source = new CardUiAtlasSource();

        assertNull(source.atlas("other"));
        assertNull(source.atlas(null));
        assertNull(source.atlas(""));
    }

    @Test
    public void offGameUnsetFieldReturnsNullWithoutThrowing() {
        seedCardUiAtlas(null);

        assertNull(new CardUiAtlasSource().atlas("cardui"));
    }

    private void seedCardUiAtlas(TextureAtlas value) {
        previous = getStaticField("cardUiAtlas");
        seeded = true;
        setStaticField("cardUiAtlas", value);
    }

    private static Object getStaticField(String name) {
        try {
            Field field = ImageMaster.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(null);
        } catch (Exception failure) {
            throw new AssertionError("could not read ImageMaster." + name, failure);
        }
    }

    private static void setStaticField(String name, Object value) {
        try {
            Field field = ImageMaster.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (Exception failure) {
            throw new AssertionError("could not set ImageMaster." + name, failure);
        }
    }
}
