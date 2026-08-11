package artframework.presentation;

/** Resolved visual chrome for a visual entity. */
public final class ChromeComponent {
    public final float panelR, panelG, panelB, panelAlpha;
    public final float borderR, borderG, borderB, borderA, borderWidth;

    public ChromeComponent(float panelR, float panelG, float panelB, float panelAlpha,
            float borderR, float borderG, float borderB, float borderA, float borderWidth) {
        this.panelR = panelR; this.panelG = panelG; this.panelB = panelB; this.panelAlpha = panelAlpha;
        this.borderR = borderR; this.borderG = borderG; this.borderB = borderB;
        this.borderA = borderA; this.borderWidth = borderWidth;
    }
}
