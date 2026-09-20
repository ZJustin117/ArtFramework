package artframework.vfx;

public final class VfxResourceRef {
    public final String resourceId, kind, sourcePath, outputPath, status;

    public VfxResourceRef(String resourceId, String kind, String sourcePath,
                          String outputPath, String status) {
        this.resourceId = resourceId; this.kind = kind; this.sourcePath = sourcePath;
        this.outputPath = outputPath; this.status = status;
    }

    public boolean available() { return outputPath != null && "supported".equals(status); }
}
