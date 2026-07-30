package artframework.core;

import java.util.List;
import java.util.Map;

/**
 * Read/write view of the global {@link PresentProfile} resource catalog (skin register).
 * Delegates to {@link PresentProfiles} — no second store.
 *
 * <p>Register does not apply; use {@link ProjectPresent#set(String)} or {@link
 * SurfacePresent#bind(String, String)}.
 */
public final class PresentProfileCatalog {

    private static final PresentProfileCatalog INSTANCE = new PresentProfileCatalog();

    private PresentProfileCatalog() {}

    public static PresentProfileCatalog get() {
        return INSTANCE;
    }

    public void register(PresentProfile profile) {
        PresentProfiles.register(profile);
    }

    public void register(String id, Theme theme) {
        PresentProfiles.register(new PresentProfile(id, theme));
    }

    public void register(String id, Theme theme, String packId) {
        PresentProfiles.register(
                new PresentProfile(
                        id,
                        theme,
                        PresentChromeStyle.fromTheme(theme),
                        packId != null ? packId : ""));
    }

    public PresentProfile get(String id) {
        return PresentProfiles.get(id);
    }

    public boolean contains(String id) {
        return PresentProfiles.contains(id);
    }

    public List<String> ids() {
        return PresentProfiles.ids();
    }

    public Map<String, Object> probeSummary() {
        return PresentProfiles.catalogProbeSummary();
    }
}
