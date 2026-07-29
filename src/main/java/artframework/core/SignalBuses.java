package artframework.core;

/** Process-global bus shared by C1, C2, presentation, and backend listeners. */
public final class SignalBuses {
    private static final SignalBus BUS = new SignalBus();
    private SignalBuses() {}
    public static SignalBus get() { return BUS; }
    public static void resetForTests() { BUS.clear(); }
}
