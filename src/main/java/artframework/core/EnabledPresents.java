package artframework.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Which present profiles are selectable from the global panel / API. Empty set = all registered
 * profiles are enabled. Regex helpers select or modify without id special cases.
 */
public final class EnabledPresents {

    /** Empty means all catalog ids are enabled. */
    private static final Set<String> ENABLED = new LinkedHashSet<String>();

    private EnabledPresents() {}

    public static boolean isRestricting() {
        return !ENABLED.isEmpty();
    }

    public static List<String> enabledIds() {
        if (ENABLED.isEmpty()) {
            return PresentProfiles.ids();
        }
        List<String> out = new ArrayList<String>();
        for (String id : PresentProfiles.ids()) {
            if (ENABLED.contains(id)) {
                out.add(id);
            }
        }
        return Collections.unmodifiableList(out);
    }

    public static boolean isEnabled(String profileId) {
        if (profileId == null || profileId.isEmpty()) {
            return false;
        }
        if (ENABLED.isEmpty()) {
            return PresentProfiles.contains(profileId);
        }
        return ENABLED.contains(profileId);
    }

    public static void setEnabled(List<String> ids) {
        ENABLED.clear();
        if (ids == null) {
            return;
        }
        for (String id : ids) {
            if (id != null && !id.isEmpty() && PresentProfiles.contains(id)) {
                ENABLED.add(id);
            }
        }
    }

    public static void enable(String profileId) {
        if (profileId == null || !PresentProfiles.contains(profileId)) {
            throw new IllegalArgumentException("unknown present profile: " + profileId);
        }
        if (ENABLED.isEmpty()) {
            // Switching from "all" to explicit: seed with all current then ensure id
            ENABLED.addAll(PresentProfiles.ids());
        }
        ENABLED.add(profileId);
    }

    public static void disable(String profileId) {
        if (profileId == null) {
            return;
        }
        if (ENABLED.isEmpty()) {
            ENABLED.addAll(PresentProfiles.ids());
        }
        ENABLED.remove(profileId);
    }

    public static void clearRestriction() {
        ENABLED.clear();
    }

    public static List<String> idsMatching(String regex) {
        Pattern p = compile(regex);
        List<String> out = new ArrayList<String>();
        for (String id : PresentProfiles.ids()) {
            if (p.matcher(id).find()) {
                out.add(id);
            }
        }
        return Collections.unmodifiableList(out);
    }

    public static List<String> enabledMatching(String regex) {
        Pattern p = compile(regex);
        List<String> out = new ArrayList<String>();
        for (String id : enabledIds()) {
            if (p.matcher(id).find()) {
                out.add(id);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Enable or disable every registered profile whose id matches {@code regex}.
     *
     * @return number of profiles modified
     */
    public static int modifyMatching(String regex, boolean enable) {
        Pattern p = compile(regex);
        int n = 0;
        for (String id : PresentProfiles.ids()) {
            if (!p.matcher(id).find()) {
                continue;
            }
            if (enable) {
                enable(id);
            } else {
                disable(id);
            }
            n++;
        }
        return n;
    }

    /**
     * Select first <b>enabled</b> profile matching regex and {@link ProjectPresent#set}.
     *
     * @return selected profile id
     */
    public static String selectMatching(String regex) {
        List<String> hits = enabledMatching(regex);
        if (hits.isEmpty()) {
            throw new IllegalArgumentException("no enabled present profile matches: " + regex);
        }
        String id = hits.get(0);
        ProjectPresent.set(id);
        return id;
    }

    /**
     * Patch packId on every matching registered profile (re-register). Null packId in patch keeps
     * existing. Does not change project apply unless {@code selectAfter} is true (first match).
     */
    public static int modifyProfilesMatching(String regex, String newPackId, boolean selectFirst) {
        Pattern p = compile(regex);
        int n = 0;
        String first = null;
        List<String> ids = new ArrayList<String>(PresentProfiles.ids());
        for (String id : ids) {
            if (!p.matcher(id).find()) {
                continue;
            }
            PresentProfile cur = PresentProfiles.get(id);
            if (cur == null) {
                continue;
            }
            String pack = newPackId != null ? newPackId : cur.packId;
            PresentProfiles.register(
                    new PresentProfile(cur.id, cur.theme, cur.chrome, pack));
            if (first == null) {
                first = id;
            }
            n++;
        }
        if (selectFirst && first != null) {
            if (!isEnabled(first)) {
                enable(first);
            }
            ProjectPresent.set(first);
        }
        return n;
    }

    public static Map<String, Object> probeSummary() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("restricting", Boolean.valueOf(isRestricting()));
        m.put("enabledIds", enabledIds());
        m.put("enabledCount", Integer.valueOf(enabledIds().size()));
        return Collections.unmodifiableMap(m);
    }

    public static void resetForTests() {
        ENABLED.clear();
    }

    private static Pattern compile(String regex) {
        if (regex == null || regex.isEmpty()) {
            throw new IllegalArgumentException("regex required");
        }
        return Pattern.compile(regex);
    }
}
