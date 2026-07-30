package artframework.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of {@link PresentProfile} resources. No process "active" profile — scope is node
 * cascade via {@link PresentResolve} plus {@link ProjectPresent} fallback.
 */
public final class PresentProfiles {

    public static final String STS = "sts";
    public static final String LIGHTWAVE = "lightwave";

    private static final Map<String, PresentProfile> BY_ID = new LinkedHashMap<String, PresentProfile>();

    static {
        installBuiltins();
    }

    private PresentProfiles() {}

    private static void installBuiltins() {
        BY_ID.clear();
        Theme sts = StsTheme.createDefault();
        sts.setName(STS);
        BY_ID.put(STS, new PresentProfile(STS, sts, PresentChromeStyle.stsDefault(), ""));
        Theme lw = LightwaveTheme.createDefault();
        lw.setName(LIGHTWAVE);
        BY_ID.put(LIGHTWAVE, new PresentProfile(LIGHTWAVE, lw, PresentChromeStyle.fromTheme(lw), ""));
    }

    public static void register(PresentProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("profile required");
        }
        BY_ID.put(profile.id, profile);
    }

    public static PresentProfile get(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return BY_ID.get(id);
    }

    public static List<String> ids() {
        return Collections.unmodifiableList(new ArrayList<String>(BY_ID.keySet()));
    }

    /** @deprecated use {@link ProjectPresent#id()} */
    @Deprecated
    public static String activeId() {
        return ProjectPresent.id();
    }

    /** @deprecated use {@link ProjectPresent#profile()} */
    @Deprecated
    public static PresentProfile active() {
        return ProjectPresent.profile();
    }

    /** @deprecated use {@link PresentResolve#chrome()} or {@link ProjectPresent#chrome()} */
    @Deprecated
    public static PresentChromeStyle chrome() {
        return ProjectPresent.chrome();
    }

    /**
     * @deprecated use {@link ProjectPresent#set(String)} — sets project fallback only.
     */
    @Deprecated
    public static void setActive(String id) {
        ProjectPresent.set(id);
    }

    /** @deprecated use {@link ProjectPresent#set(String)} */
    @Deprecated
    public static void apply(String id) {
        ProjectPresent.set(id);
    }

    public static Map<String, Object> probeSummary() {
        return ProjectPresent.probeSummary();
    }

    public static void resetForTests() {
        installBuiltins();
        ProjectPresent.resetForTests();
    }
}
