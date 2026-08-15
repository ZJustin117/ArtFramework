package artframework.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import artframework.ecs.EntityId;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;

/**
 * Process-wide project fallback present (Godot project theme analogue). Not an "active profile".
 */
public final class ProjectPresent {

    public static final String STS = PresentProfiles.STS;

    private ProjectPresent() {}

    public static String id() {
        return selection().profileId;
    }

    public static PresentProfile profile() {
        PresentProfile p = PresentProfiles.get(id());
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
        if (!EnabledPresents.isEnabled(p.id)) {
            throw new IllegalArgumentException("present profile not enabled: " + p.id);
        }
        setId(p.id);
        Themes.setDefault(p.theme);
        PresentRestyle.onProjectPresentChanged();
        try {
            PresentPacks.activateForProfile(p);
        } catch (RuntimeException ignored) {
            // Pack missing is OK for skin-only profiles
        }
    }

    public static void setProfile(PresentProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("profile required");
        }
        PresentProfiles.register(profile);
        if (!EnabledPresents.isEnabled(profile.id)) {
            EnabledPresents.enable(profile.id);
        }
        setId(profile.id);
        Themes.setDefault(profile.theme);
        PresentRestyle.onProjectPresentChanged();
        try {
            PresentPacks.activateForProfile(profile);
        } catch (RuntimeException ignored) {
        }
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
        m.put("registeredIds", PresentProfiles.ids());
        m.put("packActive", PresentPacks.activeId());
        m.putAll(EnabledPresents.probeSummary());
        if (p != null) {
            m.putAll(p.probeSummary());
        }
        return Collections.unmodifiableMap(m);
    }

    public static void resetForTests() {
        PresentationContext context = PresentationRegistry.context("present-state");
        for (EntityId entity : new java.util.ArrayList<EntityId>(context.entities())) {
            context.destroy(entity);
        }
        Themes.setDefault(PresentProfiles.get(STS).theme);
        // No PresentRestyle / pack activate — tests reset packs separately.
    }

    private static PresentSelectionComponent selection() {
        PresentationContext context = PresentationRegistry.context("present-state");
        PresentationKey key = new PresentationKey("present.project", "default");
        EntityId entity = context.entity(key);
        if (entity == null) {
            return new PresentSelectionComponent(STS);
        }
        PresentSelectionComponent value = context.world().get(entity, PresentSelectionComponent.class);
        if (value == null) {
            value = new PresentSelectionComponent(STS);
            context.world().put(entity, PresentSelectionComponent.class, value);
        }
        return value;
    }

    private static void setId(String profileId) {
        PresentationContext context = PresentationRegistry.context("present-state");
        PresentationKey key = new PresentationKey("present.project", "default");
        EntityId entity = context.entity(key);
        if (entity == null) {
            entity = context.create(key, "default", "present-selection", "artframework");
        }
        context.world().put(entity, PresentSelectionComponent.class,
                new PresentSelectionComponent(profileId));
    }
}
