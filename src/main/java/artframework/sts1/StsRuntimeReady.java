package artframework.sts1;

/** Startup marker safe to read from probes without linking BaseMod or STS classes. */
public final class StsRuntimeReady {

    private static volatile boolean ready;
    private static volatile boolean started;

    private StsRuntimeReady() {}

    public static boolean isReady() {
        return ready;
    }

    public static boolean hasStarted() {
        return started;
    }

    public static void setStarted(boolean value) {
        started = value;
    }

    public static void setReady(boolean value) {
        ready = value;
        if (value) {
            started = true;
        }
    }
}
