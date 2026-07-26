package spireui.c2;

/**
 * Observes or gates map node activation while {@code sts.map} is bound.
 * Pure hook — game patches call {@link MapTemplate#dispatchNodeClick} when present.
 */
public interface MapNodeInterceptor {

    enum Result {
        /** Continue STS default navigation. */
        ALLOW,
        /** Consume the click; STS should not travel. */
        BLOCK
    }

    Result intercept(MapNodeRef node);
}
