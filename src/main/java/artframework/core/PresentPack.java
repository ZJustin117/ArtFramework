package artframework.core;

import artframework.component.EffectDecl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UI module pack: classpath templates (LML/JSON) + synthetic windows + optional ambient tables.
 * Selected via {@link PresentProfile#packId} or {@link PresentPacks#activate(String)} — no
 * profile-id special cases in core.
 */
public final class PresentPack {

    public final String id;
    public final String profileId;
    public final String version;
    public final String provider;
    public final List<TemplateEntry> templates;
    public final List<WindowEntry> windows;
    public final List<String> autoOpen;
    /** type → effects applied when node.effects is empty (table-driven). */
    public final Map<String, List<EffectDecl>> effectDefaults;
    /** Full-frame effects while pack is active. */
    public final List<EffectDecl> fullFrameEffects;
    /** C2 surface ids to bind to {@link #profileId} (or pack id) on activate. */
    public final List<String> bindSurfaces;
    /** C2 surface id -> effects applied to the surface target while the pack is active. */
    public final Map<String, List<EffectDecl>> surfaceEffects;
    public final boolean unregisterTemplatesOnDeactivate;
    public final boolean unregisterWindowsOnDeactivate;
    public final boolean autoCloseOnDeactivate;

    public PresentPack(
            String id,
            String profileId,
            String version,
            String provider,
            List<TemplateEntry> templates,
            List<WindowEntry> windows,
            List<String> autoOpen,
            Map<String, List<EffectDecl>> effectDefaults,
            List<EffectDecl> fullFrameEffects,
            List<String> bindSurfaces,
            Map<String, List<EffectDecl>> surfaceEffects,
            boolean unregisterTemplatesOnDeactivate,
            boolean unregisterWindowsOnDeactivate,
            boolean autoCloseOnDeactivate) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("pack id required");
        }
        this.id = id;
        this.profileId = profileId != null ? profileId : "";
        this.version = version != null ? version : "";
        this.provider = provider != null ? provider : "";
        this.templates =
                templates != null
                        ? Collections.unmodifiableList(new ArrayList<TemplateEntry>(templates))
                        : Collections.<TemplateEntry>emptyList();
        this.windows =
                windows != null
                        ? Collections.unmodifiableList(new ArrayList<WindowEntry>(windows))
                        : Collections.<WindowEntry>emptyList();
        this.autoOpen =
                autoOpen != null
                        ? Collections.unmodifiableList(new ArrayList<String>(autoOpen))
                        : Collections.<String>emptyList();
        this.effectDefaults = freezeDefaults(effectDefaults);
        this.fullFrameEffects =
                fullFrameEffects != null
                        ? Collections.unmodifiableList(new ArrayList<EffectDecl>(fullFrameEffects))
                        : Collections.<EffectDecl>emptyList();
        this.bindSurfaces =
                bindSurfaces != null
                        ? Collections.unmodifiableList(new ArrayList<String>(bindSurfaces))
                        : Collections.<String>emptyList();
        this.surfaceEffects = freezeDefaults(surfaceEffects);
        this.unregisterTemplatesOnDeactivate = unregisterTemplatesOnDeactivate;
        this.unregisterWindowsOnDeactivate = unregisterWindowsOnDeactivate;
        this.autoCloseOnDeactivate = autoCloseOnDeactivate;
    }

    private static Map<String, List<EffectDecl>> freezeDefaults(
            Map<String, List<EffectDecl>> src) {
        if (src == null || src.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<EffectDecl>> m = new LinkedHashMap<String, List<EffectDecl>>();
        for (Map.Entry<String, List<EffectDecl>> e : src.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) {
                continue;
            }
            m.put(
                    e.getKey(),
                    Collections.unmodifiableList(new ArrayList<EffectDecl>(e.getValue())));
        }
        return Collections.unmodifiableMap(m);
    }

    public List<EffectDecl> effectDefaultsFor(String type) {
        if (type == null || effectDefaults.isEmpty()) {
            return Collections.emptyList();
        }
        List<EffectDecl> list = effectDefaults.get(type);
        return list != null ? list : Collections.<EffectDecl>emptyList();
    }

    public Map<String, Object> probeSummary() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", id);
        m.put("profileId", profileId);
        m.put("version", version);
        m.put("provider", provider);
        m.put("templateCount", Integer.valueOf(templates.size()));
        m.put("windowCount", Integer.valueOf(windows.size()));
        List<String> tNames = new ArrayList<String>();
        for (TemplateEntry t : templates) {
            tNames.add(t.name);
        }
        m.put("templates", tNames);
        List<String> wIds = new ArrayList<String>();
        for (WindowEntry w : windows) {
            wIds.add(w.id);
        }
        m.put("windows", wIds);
        m.put("autoOpen", new ArrayList<String>(autoOpen));
        m.put("effectDefaultTypes", new ArrayList<String>(effectDefaults.keySet()));
        m.put("fullFrameEffectCount", Integer.valueOf(fullFrameEffects.size()));
        m.put("bindSurfaces", new ArrayList<String>(bindSurfaces));
        m.put("surfaceEffectSurfaces", new ArrayList<String>(surfaceEffects.keySet()));
        return Collections.unmodifiableMap(m);
    }

    public static final class TemplateEntry {
        public final String name;
        public final String resource;

        public TemplateEntry(String name, String resource) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("template name required");
            }
            if (resource == null || resource.isEmpty()) {
                throw new IllegalArgumentException("template resource required");
            }
            this.name = name;
            this.resource = resource;
        }
    }

    public static final class WindowEntry {
        public final String id;
        public final String resource;

        public WindowEntry(String id, String resource) {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("window id required");
            }
            if (resource == null || resource.isEmpty()) {
                throw new IllegalArgumentException("window resource required");
            }
            this.id = id;
            this.resource = resource;
        }
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private String profileId = "";
        private String version = "";
        private String provider = "";
        private final List<TemplateEntry> templates = new ArrayList<TemplateEntry>();
        private final List<WindowEntry> windows = new ArrayList<WindowEntry>();
        private final List<String> autoOpen = new ArrayList<String>();
        private final Map<String, List<EffectDecl>> effectDefaults =
                new LinkedHashMap<String, List<EffectDecl>>();
        private final List<EffectDecl> fullFrameEffects = new ArrayList<EffectDecl>();
        private final List<String> bindSurfaces = new ArrayList<String>();
        private final Map<String, List<EffectDecl>> surfaceEffects =
                new LinkedHashMap<String, List<EffectDecl>>();
        private boolean unregisterTemplatesOnDeactivate = true;
        private boolean unregisterWindowsOnDeactivate = false;
        private boolean autoCloseOnDeactivate = false;

        public Builder(String id) {
            this.id = id;
        }

        public Builder profileId(String profileId) {
            this.profileId = profileId != null ? profileId : "";
            return this;
        }

        public Builder version(String version) {
            this.version = version != null ? version : "";
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider != null ? provider : "";
            return this;
        }

        public Builder template(String name, String resource) {
            templates.add(new TemplateEntry(name, resource));
            return this;
        }

        public Builder window(String id, String resource) {
            windows.add(new WindowEntry(id, resource));
            return this;
        }

        public Builder autoOpen(String windowId) {
            if (windowId != null && !windowId.isEmpty()) {
                autoOpen.add(windowId);
            }
            return this;
        }

        public Builder effectDefault(String type, EffectDecl effect) {
            if (type == null || type.isEmpty() || effect == null) {
                return this;
            }
            List<EffectDecl> list = effectDefaults.get(type);
            if (list == null) {
                list = new ArrayList<EffectDecl>();
                effectDefaults.put(type, list);
            }
            list.add(effect);
            return this;
        }

        public Builder fullFrameEffect(EffectDecl effect) {
            if (effect != null) {
                fullFrameEffects.add(effect);
            }
            return this;
        }

        public Builder bindSurface(String surfaceId) {
            if (surfaceId != null && !surfaceId.isEmpty()) {
                bindSurfaces.add(surfaceId);
            }
            return this;
        }

        public Builder surfaceEffect(String surfaceId, EffectDecl effect) {
            if (surfaceId == null || surfaceId.isEmpty() || effect == null) {
                return this;
            }
            List<EffectDecl> list = surfaceEffects.get(surfaceId);
            if (list == null) {
                list = new ArrayList<EffectDecl>();
                surfaceEffects.put(surfaceId, list);
            }
            list.add(effect);
            return this;
        }

        public Builder unregisterTemplatesOnDeactivate(boolean v) {
            this.unregisterTemplatesOnDeactivate = v;
            return this;
        }

        public Builder unregisterWindowsOnDeactivate(boolean v) {
            this.unregisterWindowsOnDeactivate = v;
            return this;
        }

        public Builder autoCloseOnDeactivate(boolean v) {
            this.autoCloseOnDeactivate = v;
            return this;
        }

        public PresentPack build() {
            return new PresentPack(
                    id,
                    profileId,
                    version,
                    provider,
                    templates,
                    windows,
                    autoOpen,
                    effectDefaults,
                    fullFrameEffects,
                    bindSurfaces,
                    surfaceEffects,
                    unregisterTemplatesOnDeactivate,
                    unregisterWindowsOnDeactivate,
                    autoCloseOnDeactivate);
        }
    }
}
