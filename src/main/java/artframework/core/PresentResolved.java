package artframework.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Result of walking present-profile nodes + project fallback. */
public final class PresentResolved {

    public final String profileId;
    public final Theme theme;
    public final PresentChromeStyle chrome;
    public final String packId;
    public final boolean fromProject;

    public PresentResolved(
            String profileId,
            Theme theme,
            PresentChromeStyle chrome,
            String packId,
            boolean fromProject) {
        this.profileId = profileId != null ? profileId : ProjectPresent.STS;
        this.theme = theme != null ? theme : ProjectPresent.theme();
        this.chrome = chrome != null ? chrome : ProjectPresent.chrome();
        this.packId = packId != null ? packId : "";
        this.fromProject = fromProject;
    }

    public Map<String, Object> probeSummary() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", profileId);
        m.put("fromProject", Boolean.valueOf(fromProject));
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
