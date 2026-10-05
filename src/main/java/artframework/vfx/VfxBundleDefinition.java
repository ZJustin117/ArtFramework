package artframework.vfx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VfxBundleDefinition {
    public final String bundleId;
    public final int schemaVersion;
    public final VfxCapability capability;
    public final List<VfxSceneEntry> scenes;
    public final List<VfxResourceRef> resources;
    /** Optional bundle-relative diagnostics path; empty when the manifest omits it. */
    public final String diagnosticsPath;

    public VfxBundleDefinition(String bundleId, int schemaVersion, VfxCapability capability,
                               List<VfxSceneEntry> scenes, List<VfxResourceRef> resources) {
        this(bundleId, schemaVersion, capability, scenes, resources, "");
    }

    public VfxBundleDefinition(String bundleId, int schemaVersion, VfxCapability capability,
                               List<VfxSceneEntry> scenes, List<VfxResourceRef> resources,
                               String diagnosticsPath) {
        this.bundleId = bundleId; this.schemaVersion = schemaVersion; this.capability = capability;
        this.scenes = Collections.unmodifiableList(new ArrayList<VfxSceneEntry>(scenes));
        this.resources = Collections.unmodifiableList(new ArrayList<VfxResourceRef>(resources));
        this.diagnosticsPath = diagnosticsPath == null ? "" : diagnosticsPath;
    }

    public String getDiagnosticsPath() { return diagnosticsPath; }

    public static final class VfxSceneEntry {
        public final String id, path;
        public final VfxCapability capability;
        public VfxSceneEntry(String id, String path, VfxCapability capability) {
            this.id = id; this.path = path; this.capability = capability;
        }
    }
}
