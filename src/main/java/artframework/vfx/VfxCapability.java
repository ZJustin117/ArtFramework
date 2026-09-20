package artframework.vfx;

public enum VfxCapability {
    SUPPORTED, DEGRADED, BAKED_ONLY, UNSUPPORTED;

    static VfxCapability parse(String value) {
        try {
            return value == null ? null : valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid capability: " + value);
        }
    }
}
