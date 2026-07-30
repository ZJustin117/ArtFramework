package artframework.c1.skin;

import org.junit.Test;
import artframework.core.LightwaveTheme;
import artframework.core.StsTheme;
import artframework.core.Theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Pure mapping helpers — no GL / ImageMaster. */
public class StsSkinThemeMapTest {

    @Test
    public void lightwavePanelMoreTransparentThanSts() {
        float[] sts = StsSkin.panelBgRgba(StsTheme.createDefault());
        float[] lw = StsSkin.panelBgRgba(LightwaveTheme.createDefault());
        assertTrue(lw[3] < sts[3]);
    }

    @Test
    public void lightwaveLabelIsCoolTint() {
        float[] lw = StsSkin.labelFontRgba(LightwaveTheme.createDefault());
        assertTrue(lw[2] >= lw[0]);
    }

    @Test
    public void nullThemeFallsBack() {
        float[] bg = StsSkin.panelBgRgba(null);
        assertEquals(0.88f, bg[3], 0.001f);
    }

    @Test
    public void emptyThemeFallsBack() {
        float[] bg = StsSkin.panelBgRgba(new Theme());
        assertEquals(0.88f, bg[3], 0.001f);
    }
}
