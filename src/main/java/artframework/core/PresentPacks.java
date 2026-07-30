package artframework.core;

import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.c1.SyntheticRuntime;
import artframework.component.ComponentRegistry;
import artframework.component.UiNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Global catalog of {@link PresentPack} UI modules. Activate loads templates/windows into existing
 * registries from classpath data — no profile-id special cases.
 */
public final class PresentPacks {

    private static final Map<String, PresentPack> BY_ID = new LinkedHashMap<String, PresentPack>();
    private static final Map<String, Set<String>> OWNED_TEMPLATES =
            new LinkedHashMap<String, Set<String>>();
    private static final Map<String, Set<String>> OWNED_WINDOWS =
            new LinkedHashMap<String, Set<String>>();
    private static String activePackId = "";

    private PresentPacks() {}

    public static void register(PresentPack pack) {
        if (pack == null) {
            throw new IllegalArgumentException("pack required");
        }
        BY_ID.put(pack.id, pack);
    }

    public static void registerClasspath(String manifestResource) {
        register(PresentPackLoader.loadClasspath(manifestResource));
    }

    public static PresentPack get(String id) {
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

    public static String activeId() {
        return activePackId;
    }

    public static PresentPack active() {
        return get(activePackId);
    }

    public static boolean isActive(String id) {
        return id != null && id.equals(activePackId);
    }

    /**
     * Activate pack by id: register templates + windows; optional autoOpen. Deactivates previous
     * active pack first when different.
     */
    public static void activate(String packId) {
        if (packId == null || packId.isEmpty()) {
            deactivateActive();
            return;
        }
        PresentPack pack = BY_ID.get(packId);
        if (pack == null) {
            throw new IllegalArgumentException("unknown present pack: " + packId);
        }
        if (packId.equals(activePackId)) {
            ensureLoaded(pack);
            PresentPackApply.syncFromActivePack();
            return;
        }
        if (!activePackId.isEmpty()) {
            deactivate(activePackId);
        }
        ensureLoaded(pack);
        activePackId = pack.id;
        PresentPackApply.syncFromActivePack();
        for (String win : pack.autoOpen) {
            try {
                if (ArtFramework.isRegistered(win) && !ArtFramework.listOpenIds().contains(win)) {
                    ArtFramework.open(win);
                }
            } catch (RuntimeException ignored) {
            }
        }
    }

    /** Activate pack linked to profile: packId field, else pack whose profileId matches. */
    public static void activateForProfile(PresentProfile profile) {
        if (profile == null) {
            deactivateActive();
            return;
        }
        String packId = profile.packId;
        if (packId != null && !packId.isEmpty() && BY_ID.containsKey(packId)) {
            activate(packId);
            return;
        }
        for (PresentPack p : BY_ID.values()) {
            if (profile.id.equals(p.profileId)) {
                activate(p.id);
                return;
            }
        }
        // No pack for this profile — clear previous pack UI module.
        deactivateActive();
    }

    public static void deactivateActive() {
        if (!activePackId.isEmpty()) {
            deactivate(activePackId);
        }
    }

    public static void deactivate(String packId) {
        if (packId == null || packId.isEmpty()) {
            return;
        }
        PresentPack pack = BY_ID.get(packId);
        if (pack == null) {
            if (packId.equals(activePackId)) {
                activePackId = "";
            }
            return;
        }
        if (pack.autoCloseOnDeactivate) {
            for (String win : pack.autoOpen) {
                try {
                    ArtFramework.close(win);
                } catch (RuntimeException ignored) {
                }
            }
            Set<String> ownedW = OWNED_WINDOWS.get(packId);
            if (ownedW != null) {
                for (String win : ownedW) {
                    try {
                        ArtFramework.close(win);
                    } catch (RuntimeException ignored) {
                    }
                }
            }
        }
        if (pack.unregisterTemplatesOnDeactivate) {
            Set<String> owned = OWNED_TEMPLATES.get(packId);
            if (owned != null) {
                ComponentRegistry reg = ComponentRegistry.global();
                for (String name : owned) {
                    reg.unregister(name);
                }
            }
            OWNED_TEMPLATES.remove(packId);
        }
        if (pack.unregisterWindowsOnDeactivate) {
            Set<String> owned = OWNED_WINDOWS.get(packId);
            if (owned != null) {
                for (String win : owned) {
                    ArtFramework.unregisterWindow(win);
                }
            }
            OWNED_WINDOWS.remove(packId);
        }
        if (packId.equals(activePackId)) {
            activePackId = "";
            PresentPackApply.syncFromActivePack();
        }
    }

    private static void ensureLoaded(PresentPack pack) {
        Set<String> tOwned = OWNED_TEMPLATES.get(pack.id);
        if (tOwned == null) {
            tOwned = new LinkedHashSet<String>();
            OWNED_TEMPLATES.put(pack.id, tOwned);
        }
        ComponentRegistry reg = ComponentRegistry.global();
        for (PresentPack.TemplateEntry te : pack.templates) {
            UiNode root = SyntheticRuntime.loadLayoutResource(te.resource);
            reg.register(te.name, root);
            tOwned.add(te.name);
        }
        Set<String> wOwned = OWNED_WINDOWS.get(pack.id);
        if (wOwned == null) {
            wOwned = new LinkedHashSet<String>();
            OWNED_WINDOWS.put(pack.id, wOwned);
        }
        for (PresentPack.WindowEntry we : pack.windows) {
            ArtFramework.register(new WindowDef(we.id, WindowClass.SYNTHETIC, we.resource));
            wOwned.add(we.id);
        }
    }

    public static List<String> idsMatching(String regex) {
        Pattern p = compile(regex);
        List<String> out = new ArrayList<String>();
        for (String id : BY_ID.keySet()) {
            if (p.matcher(id).find()) {
                out.add(id);
            }
        }
        return Collections.unmodifiableList(out);
    }

    public static Map<String, Object> probeSummary() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("ids", ids());
        m.put("count", Integer.valueOf(BY_ID.size()));
        m.put("active", activePackId);
        Map<String, Object> byId = new LinkedHashMap<String, Object>();
        for (PresentPack pack : BY_ID.values()) {
            Map<String, Object> one = new LinkedHashMap<String, Object>(pack.probeSummary());
            one.put("active", Boolean.valueOf(pack.id.equals(activePackId)));
            one.put(
                    "templatesLoaded",
                    Integer.valueOf(
                            OWNED_TEMPLATES.containsKey(pack.id)
                                    ? OWNED_TEMPLATES.get(pack.id).size()
                                    : 0));
            byId.put(pack.id, one);
        }
        m.put("byId", byId);
        m.put("apply", PresentPackApply.probeSummary());
        return Collections.unmodifiableMap(m);
    }

    public static void installBuiltinLightwavePack() {
        if (BY_ID.containsKey(PresentProfiles.LIGHTWAVE)) {
            return;
        }
        try {
            registerClasspath("present-packs/lightwave/pack.json");
        } catch (RuntimeException e) {
            // Fallback builder if resource missing in odd classpaths
            // Prefer classpath manifest (has full effectDefaults). Fallback is minimal.
            Map<String, Object> p = new LinkedHashMap<String, Object>();
            p.put("intensity", Float.valueOf(0.55f));
            p.put("angle", Float.valueOf(35f));
            p.put("width", Float.valueOf(0.28f));
            p.put("border", Float.valueOf(1f));
            p.put("borderWidth", Float.valueOf(3f));
            artframework.component.EffectDecl lw =
                    new artframework.component.EffectDecl("lightwave", p);
            register(
                    PresentPack.builder(PresentProfiles.LIGHTWAVE)
                            .profileId(PresentProfiles.LIGHTWAVE)
                            .provider("artframework")
                            .version("1")
                            .template(
                                    "lightwave.panel_chrome",
                                    "present-packs/lightwave/panel_chrome.json")
                            .window("lightwave_pack_demo", "layouts/lightwave_demo.json")
                            .effectDefault("window", lw)
                            .effectDefault("panel", lw)
                            .effectDefault("button", lw)
                            .effectDefault("label", lw)
                            .fullFrameEffect(lw)
                            .bindSurface(artframework.context.SurfaceIds.COMBAT_HAND)
                            .bindSurface(artframework.context.SurfaceIds.EVENT)
                            .build());
        }
    }

    public static void resetForTests() {
        List<String> ids = new ArrayList<String>(BY_ID.keySet());
        for (String id : ids) {
            try {
                deactivate(id);
            } catch (RuntimeException ignored) {
            }
        }
        BY_ID.clear();
        OWNED_TEMPLATES.clear();
        OWNED_WINDOWS.clear();
        activePackId = "";
        PresentPackApply.resetForTests();
    }

    private static Pattern compile(String regex) {
        if (regex == null || regex.isEmpty()) {
            throw new IllegalArgumentException("regex required");
        }
        return Pattern.compile(regex);
    }
}
