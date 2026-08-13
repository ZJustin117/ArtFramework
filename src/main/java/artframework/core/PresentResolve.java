package artframework.core;

import artframework.component.ArtNodeTypes;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRuntime;
import artframework.presentation.NodeHierarchyComponent;
import artframework.presentation.NodeIdentityComponent;
import artframework.presentation.NodePropertiesComponent;
import artframework.ecs.EntityId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Godot-like present cascade: walk node → root for {@link PresentBinding} layers, then project.
 *
 * <p>OVERRIDE truncates parent walk. ATTACH stacks; nearest (leaf-ward) layer supplies the whole
 * Theme + Chrome package (no field-wise merge).
 */
public final class PresentResolve {

    private PresentResolve() {}

    /** ECS-native present cascade for a presentation entity. */
    public static PresentResolved forEntity(PresentationContext context, EntityId entity) {
        if (context == null || entity == null) return ProjectPresent.resolved();
        List<PresentBinding> layers = new ArrayList<PresentBinding>();
        EntityId current = entity;
        while (current != null) {
            NodeIdentityComponent identity = PresentationRuntime.identity(context, current);
            NodePropertiesComponent props = PresentationRuntime.component(context, current, NodePropertiesComponent.class);
            PresentBinding binding = parseBindingFromProps(identity != null ? identity.type : "",
                    props != null ? props.view() : Collections.<String, Object>emptyMap());
            if (binding != null) {
                layers.add(binding);
                if (binding.mode == PresentMode.OVERRIDE) break;
            }
            NodeHierarchyComponent hierarchy = PresentationRuntime.hierarchy(context, current);
            current = hierarchy != null ? hierarchy.parent : null;
        }
        if (!layers.isEmpty()) {
            PresentProfile profile = layers.get(0).resolveResource();
            if (profile != null) return new PresentResolved(profile.id, profile.theme, profile.chrome, profile.packId, false);
        }
        current = entity;
        while (current != null) {
            Object themeName = PresentationRuntime.property(context, current, "theme");
            Theme theme = themeName != null ? Themes.get(String.valueOf(themeName).trim()) : null;
            if (theme != null) return new PresentResolved(theme.name(), theme, PresentChromeStyle.fromTheme(theme), "", false);
            NodeHierarchyComponent hierarchy = PresentationRuntime.hierarchy(context, current);
            current = hierarchy != null ? hierarchy.parent : null;
        }
        return ProjectPresent.resolved();
    }


    /** C2 / no-tree consumers: project fallback. */
    public static PresentChromeStyle chrome() {
        return ProjectPresent.chrome();
    }

    /** C2 surface chrome: {@link SurfacePresent} bind, else project. */
    public static PresentChromeStyle chromeForSurface(String surfaceId) {
        return SurfacePresent.chrome(surfaceId);
    }

    public static PresentResolved forSurface(String surfaceId) {
        return SurfacePresent.resolve(surfaceId);
    }

    /**
     * Parse present binding from node type/props (mount + pure decl).
     *
     * <ul>
     *   <li>{@code art.present_profile}: props {@code profile} or {@code id}, {@code mode}
     *   <li>Any node: {@code present_profile} / {@code presentProfile} sugar (default override)
     * </ul>
     */
    public static PresentBinding parseBindingFromProps(String type, Map<String, Object> props) {
        if (props == null || props.isEmpty()) {
            return null;
        }
        boolean presentNode = isPresentProfileType(type);
        String id = null;
        PresentMode mode = PresentMode.OVERRIDE;

        if (presentNode) {
            id = stringProp(props, "profile");
            if (id == null) {
                id = stringProp(props, "present_profile");
            }
            if (id == null) {
                id = stringProp(props, "presentProfile");
            }
            // Avoid treating node instance id as profile when type is art.present_profile
            // unless profile keys missing and "id" looks like a registered profile.
            if (id == null) {
                String maybe = stringProp(props, "id");
                if (maybe != null && PresentProfiles.get(maybe) != null) {
                    id = maybe;
                }
            }
            Object m = props.get("mode");
            if (m != null) {
                mode = PresentMode.parse(String.valueOf(m));
            }
        } else {
            id = stringProp(props, "present_profile");
            if (id == null) {
                id = stringProp(props, "presentProfile");
            }
            Object m = props.get("present_mode");
            if (m == null) {
                m = props.get("presentMode");
            }
            if (m != null) {
                mode = PresentMode.parse(String.valueOf(m));
            }
        }

        if (id == null || id.isEmpty() || PresentProfiles.get(id) == null) {
            return null;
        }
        return new PresentBinding(id, mode);
    }

    public static boolean isPresentProfileType(String type) {
        return ArtNodeTypes.PRESENT_PROFILE.equals(type)
                || "present_profile".equals(type)
                || "presentProfile".equals(type);
    }

    private static String stringProp(Map<String, Object> props, String key) {
        Object v = props.get(key);
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

}
