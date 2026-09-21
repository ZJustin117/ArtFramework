package artframework.sts1.render;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Pure, package-local policy that marks unrelated native families as filtered while the selected
 * family continues. It is consulted only from {@link NativeRenderBridge#beginSurface} after the
 * panic/unknown-owner fail-open branches and only inside the delegation branch, so its sole effect
 * is to DOWNGRADE a would-be {@code DELEGATE_TO_ART} invocation to pass-through/capture.
 *
 * <p>INVARIANT: a scope must never turn a {@code failOpen}/{@code unknown_owner}/{@code panic}/
 * {@code PASS_THROUGH} invocation into {@code DELEGATE_TO_ART}. It cannot grant new permission to
 * suppress native pixels; it can only narrow an existing delegation decision. A filtered family
 * that is not the selected family is always forced back to native continuation.
 */
final class NativeFilterScope {
    private boolean active;
    private final Set<String> selectedFamilies = new LinkedHashSet<String>();
    private final Set<String> filteredFamilies = new LinkedHashSet<String>();

    void activate() {
        active = true;
    }

    void deactivate() {
        active = false;
    }

    boolean isActive() {
        return active;
    }

    void select(String family) {
        String key = normalize(family);
        if (key == null) return;
        selectedFamilies.add(key);
    }

    void selectAll(Set<String> families) {
        if (families == null) return;
        for (String family : families) select(family);
    }

    void filter(String family) {
        String key = normalize(family);
        if (key == null) return;
        filteredFamilies.add(key);
    }

    void filterAll(Set<String> families) {
        if (families == null) return;
        for (String family : families) filter(family);
    }

    void unfilter(String family) {
        String key = normalize(family);
        if (key == null) return;
        filteredFamilies.remove(key);
        if (filteredFamilies.isEmpty()) active = false;
    }

    /**
     * True only when the scope is active, the family is explicitly filtered, and it is not one of
     * the selected families. Callers must treat {@code true} as a downgrade to native continuation,
     * never as authorization to delegate.
     */
    boolean blocksDelegation(String family) {
        String key = normalize(family);
        if (!active || key == null) return false;
        if (!filteredFamilies.contains(key)) return false;
        return !selectedFamilies.contains(key);
    }

    /** True when the scope is active and the family is explicitly in the filtered set. */
    boolean isFiltered(String family) {
        String key = normalize(family);
        return active && key != null && filteredFamilies.contains(key);
    }

    void clear() {
        active = false;
        selectedFamilies.clear();
        filteredFamilies.clear();
    }

    Map<String, Object> probeSlice() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("active", Boolean.valueOf(active));
        out.put("selectedFamilies", new java.util.ArrayList<String>(selectedFamilies));
        out.put("filteredFamilies", new java.util.ArrayList<String>(filteredFamilies));
        return Collections.unmodifiableMap(out);
    }

    private static String normalize(String family) {
        if (family == null) return null;
        String trimmed = family.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
