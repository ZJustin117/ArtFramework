package artframework.core;

import artframework.component.ArtNodeTypes;
import artframework.presentation.Node;
import artframework.presentation.NodeTree;

import java.util.ArrayList;
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

    public static PresentResolved forNode(Node node) {
        if (node == null) {
            return ProjectPresent.resolved();
        }
        List<PresentBinding> layers = new ArrayList<PresentBinding>();
        Node cur = node;
        while (cur != null) {
            PresentBinding b = bindingOf(cur);
            if (b != null) {
                layers.add(b);
                if (b.mode == PresentMode.OVERRIDE) {
                    break;
                }
            }
            cur = cur.parent();
        }
        if (!layers.isEmpty()) {
            PresentBinding top = layers.get(0);
            PresentProfile p = top.resolveResource();
            if (p != null) {
                return new PresentResolved(p.id, p.theme, p.chrome, p.packId, false);
            }
        }
        Theme named = nearestNamedTheme(node);
        if (named != null) {
            String name = named.name() != null ? named.name() : "theme";
            return new PresentResolved(name, named, PresentChromeStyle.fromTheme(named), "", false);
        }
        return ProjectPresent.resolved();
    }


    public static PresentResolved forTree(NodeTree tree) {
        if (tree == null || tree.root() == null) {
            return ProjectPresent.resolved();
        }
        return forNode(tree.root());
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

    public static PresentChromeStyle chromeFor(NodeTree tree) {
        return forTree(tree).chrome;
    }

    public static PresentChromeStyle chromeFor(Node node) {
        return forNode(node).chrome;
    }


    public static Theme themeFor(NodeTree tree) {
        return forTree(tree).theme;
    }

    public static Theme themeFor(Node node) {
        return forNode(node).theme;
    }


    static PresentBinding bindingOf(Node inst) {
        if (inst == null) {
            return null;
        }
        Map<String, Object> props = new java.util.LinkedHashMap<String, Object>();
        String[] keys = new String[] {
            "profile", "present_profile", "presentProfile", "present_mode", "presentMode", "mode"
        };
        for (String key : keys) {
            Object value = inst.get(key);
            if (value != null) props.put(key, value);
        }
        return parseBindingFromProps(inst.type(), props);
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

    private static Theme nearestNamedTheme(Node node) {
        Node cur = node;
        while (cur != null) {
            Object themeName = cur.prop("theme");
            if (themeName != null) {
                String name = String.valueOf(themeName).trim();
                if (!name.isEmpty()) {
                    Theme t = Themes.get(name);
                    if (t != null) {
                        return t;
                    }
                }
            }
            cur = cur.parent();
        }
        return null;
    }

}
