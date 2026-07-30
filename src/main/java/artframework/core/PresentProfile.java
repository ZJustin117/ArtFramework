package artframework.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Named presentation configuration: theme tokens + C2 chrome + optional pack / FX hints.
 * Does not suppress native surfaces; FullPresentMode remains authoritative for FULL.
 */
public final class PresentProfile {

    public final String id;
    public final Theme theme;
    public final PresentChromeStyle chrome;
    public final String packId;

    public PresentProfile(String id, Theme theme, PresentChromeStyle chrome, String packId) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("profile id required");
        }
        this.id = id;
        this.theme = theme != null ? theme : StsTheme.createDefault();
        this.chrome = chrome != null ? chrome : PresentChromeStyle.fromTheme(this.theme);
        this.packId = packId != null ? packId : "";
    }

    public PresentProfile(String id, Theme theme) {
        this(id, theme, PresentChromeStyle.fromTheme(theme), "");
    }

    public Map<String, Object> probeSummary() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", id);
        m.put("packId", packId);
        Map<String, Object> themeMap = new LinkedHashMap<String, Object>(theme.probeSummary());
        if (theme.name() != null && !theme.name().isEmpty()) {
            themeMap.put("name", theme.name());
        }
        m.put("theme", themeMap);
        m.put("chrome", chrome.probeSummary());
        return Collections.unmodifiableMap(m);
    }
}
