package artframework.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Process-wide project fallback present (Godot project theme analogue). Not an "active profile".
 */
public final class ProjectPresent {

    public static final String STS = PresentProfiles.STS;

    private static String projectId = STS;

    private ProjectPresent() {}

    public static String id() {
        return projectId;
    }

    public static PresentProfile profile() {
        PresentProfile p = PresentProfiles.get(projectId);
        return p != null ? p : PresentProfiles.get(STS);
    }

    public static Theme theme() {
        PresentProfile p = profile();
        return p != null ? p.theme : StsTheme.createDefault();
    }

    public static PresentChromeStyle chrome() {
        PresentProfile p = profile();
        return p != null ? p.chrome : PresentChromeStyle.stsDefault();
    }

    public static void set(String profileId) {
        PresentProfile p = PresentProfiles.get(profileId);
        if (p == null) {
            throw new IllegalArgumentException("unknown present profile: " + profileId);
        }
        projectId = p.id;
        Themes.setDefault(p.theme);
    }

    public static void setProfile(PresentProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("profile required");
        }
        PresentProfiles.register(profile);
        projectId = profile.id;
        Themes.setDefault(profile.theme);
    }

    public static PresentResolved resolved() {
        PresentProfile p = profile();
        return new PresentResolved(p.id, p.theme, p.chrome, p.packId, true);
    }

    public static Map<String, Object> probeSummary() {
        PresentProfile p = profile();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("project", p != null ? p.id : STS);
        // Alias for older fixtures / scenarios that read presentProfile.active
        m.put("active", p != null ? p.id : STS);
        m.put("ids", PresentProfiles.ids());
        if (p != null) {
            m.putAll(p.probeSummary());
        }
        return Collections.unmodifiableMap(m);
    }

    public static void resetForTests() {
        projectId = STS;
        Themes.setDefault(PresentProfiles.get(STS).theme);
    }
}
