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
        // Direct put then theme sync — avoid recursive apply during static init.
        putAndSyncTheme(new PresentProfile(STS, sts, PresentChromeStyle.stsDefault(), ""));
        Theme lw = LightwaveTheme.createDefault();
        lw.setName(LIGHTWAVE);
        // packId links to PresentPack id (data-driven UI module; no id special-case in apply)
        putAndSyncTheme(
                new PresentProfile(LIGHTWAVE, lw, PresentChromeStyle.fromTheme(lw), LIGHTWAVE));
    }

    /**
     * Register a present profile resource (global skin catalog entry). Does <b>not</b> change
     * {@link ProjectPresent} — use {@link ProjectPresent#set(String)} to apply.
     *
     * <p>When the profile theme has a non-empty name, also {@link Themes#register} so cascade
     * {@code theme} props resolve.
     */
    public static void register(PresentProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("profile required");
        }
        putAndSyncTheme(profile);
    }

    private static void putAndSyncTheme(PresentProfile profile) {
        BY_ID.put(profile.id, profile);
        Theme t = profile.theme;
        if (t == null) {
            return;
        }
        String name = t.name();
        if (name == null || name.isEmpty()) {
            name = profile.id;
            t.setName(name);
        }
        Themes.register(name, t);
        if (!name.equals(profile.id) && Themes.get(profile.id) == null) {
            Themes.register(profile.id, t);
        }
    }

    public static PresentProfile get(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return BY_ID.get(id);
    }

    public static boolean contains(String id) {
        return get(id) != null;
    }

    public static List<String> ids() {
        return Collections.unmodifiableList(new ArrayList<String>(BY_ID.keySet()));
    }

    /** Catalog probe: registered ids only (not project apply state). */
    public static Map<String, Object> catalogProbeSummary() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        List<String> idList = ids();
        m.put("ids", idList);
        m.put("count", Integer.valueOf(idList.size()));
        Map<String, Object> byId = new LinkedHashMap<String, Object>();
        for (String id : idList) {
            PresentProfile p = BY_ID.get(id);
            if (p != null) {
                Map<String, Object> one = new LinkedHashMap<String, Object>();
                one.put("id", p.id);
                one.put("packId", p.packId);
                if (p.theme != null && p.theme.name() != null) {
                    one.put("themeName", p.theme.name());
                }
                byId.put(id, one);
            }
        }
        m.put("byId", byId);
        return Collections.unmodifiableMap(m);
    }

    public static Map<String, Object> probeSummary() {
        return ProjectPresent.probeSummary();
    }

    public static void resetForTests() {
        installBuiltins();
        ProjectPresent.resetForTests();
    }
}
