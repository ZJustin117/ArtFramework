package artframework.assets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Unified HostAssets configuration (profiles, order, domain switches). */
public final class AssetsConfig {

    private String activeProfile = "default";
    private final List<String> packOrder = new ArrayList<String>();
    private final Map<AssetDomain, Boolean> domainEnable =
            new EnumMap<AssetDomain, Boolean>(AssetDomain.class);
    private final Map<String, Boolean> packEnable = new LinkedHashMap<String, Boolean>();
    private boolean strictMissing;

    public AssetsConfig() {
        for (AssetDomain d : AssetDomain.values()) {
            domainEnable.put(d, Boolean.TRUE);
        }
    }

    public String activeProfile() {
        return activeProfile;
    }

    public void setActiveProfile(String profile) {
        this.activeProfile = profile != null ? profile : "default";
    }

    public List<String> packOrder() {
        return Collections.unmodifiableList(new ArrayList<String>(packOrder));
    }

    public void setPackOrder(List<String> order) {
        packOrder.clear();
        if (order != null) {
            for (String id : order) {
                if (id != null && !id.isEmpty() && !packOrder.contains(id)) {
                    packOrder.add(id);
                }
            }
        }
    }

    public boolean isDomainEnabled(AssetDomain domain) {
        Boolean v = domainEnable.get(domain);
        return v == null || v.booleanValue();
    }

    public void setDomainEnabled(AssetDomain domain, boolean enabled) {
        if (domain != null) {
            domainEnable.put(domain, Boolean.valueOf(enabled));
        }
    }

    public boolean isPackEnabled(String packId) {
        if (packId == null) {
            return false;
        }
        Boolean v = packEnable.get(packId);
        return v == null || v.booleanValue();
    }

    public void setPackEnabled(String packId, boolean enabled) {
        if (packId != null) {
            packEnable.put(packId, Boolean.valueOf(enabled));
        }
    }

    public boolean isStrictMissing() {
        return strictMissing;
    }

    public void setStrictMissing(boolean strictMissing) {
        this.strictMissing = strictMissing;
    }

    public Map<String, Object> probeSummary() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("activeProfile", activeProfile);
        m.put("packOrder", new ArrayList<String>(packOrder));
        m.put("strictMissing", Boolean.valueOf(strictMissing));
        Map<String, Object> domains = new LinkedHashMap<String, Object>();
        for (AssetDomain d : AssetDomain.values()) {
            domains.put(d.name(), Boolean.valueOf(isDomainEnabled(d)));
        }
        m.put("domains", domains);
        return m;
    }
}
