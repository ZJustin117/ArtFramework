package artframework.sts1.lab;

import java.util.Locale;

/** Vanilla event aliases accepted by the deterministic device lab. */
public final class LabEventIds {

    private LabEventIds() {}

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().replace('_', ' ');
        if (value.isEmpty()) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if ("the cleric".equals(lower)) {
            return "The Cleric";
        }
        if ("world of goop".equals(lower)) {
            return "World of Goop";
        }
        if ("golden shrine".equals(lower)) {
            return "Golden Shrine";
        }
        if ("upgrade shrine".equals(lower)) {
            return "Upgrade Shrine";
        }
        return "";
    }

    public static boolean isSupported(String raw) {
        return !normalize(raw).isEmpty();
    }
}
