package artframework.c1.host;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SkinOwnershipTest {

    @Test
    public void releasesDetachedWindowSkinOnce() {
        SkinOwnership ownership = ownership();
        RecordingSkin skin = new RecordingSkin();
        ownership.attach("window", skin);

        ownership.detach("window", null);
        ownership.detach("window", null);

        assertEquals(1, skin.disposeCount);
    }

    @Test
    public void retainsRetiredDefaultUntilLastWindowDetaches() {
        SkinOwnership ownership = ownership();
        RecordingSkin oldDefault = new RecordingSkin();
        RecordingSkin newDefault = new RecordingSkin();
        ownership.attach("window", oldDefault);

        ownership.replaceDefault(oldDefault);
        assertEquals(0, oldDefault.disposeCount);

        ownership.detach("window", newDefault);
        assertEquals(1, oldDefault.disposeCount);
    }

    @Test
    public void releasesUnattachedNonDefaultSkin() {
        SkinOwnership ownership = ownership();
        RecordingSkin skin = new RecordingSkin();

        ownership.releaseUnattached(skin, null);

        assertEquals(1, skin.disposeCount);
    }

    @Test
    public void clearReleasesActiveAndRetiredSkins() {
        SkinOwnership ownership = ownership();
        RecordingSkin active = new RecordingSkin();
        RecordingSkin retired = new RecordingSkin();
        RecordingSkin currentDefault = new RecordingSkin();
        ownership.attach("active", active);
        ownership.attach("retired", retired);
        ownership.replaceDefault(retired);

        ownership.clear(currentDefault);

        assertEquals(1, active.disposeCount);
        assertEquals(1, retired.disposeCount);
        assertEquals(0, currentDefault.disposeCount);
    }

    @Test
    public void clearReleasesSharedSkinOnlyOnce() {
        SkinOwnership ownership = ownership();
        RecordingSkin shared = new RecordingSkin();
        ownership.attach("first", shared);
        ownership.attach("second", shared);

        ownership.clear(null);

        assertEquals(1, shared.disposeCount);
    }

    private static SkinOwnership ownership() {
        return new SkinOwnership(new SkinOwnership.Releaser() {
            @Override
            public void release(Object skin) {
                ((RecordingSkin) skin).dispose();
            }
        });
    }

    private static final class RecordingSkin {
        private int disposeCount;

        private void dispose() {
            disposeCount++;
        }
    }
}
