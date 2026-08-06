package artframework.sts1.lab;

/** Small value object for lab navigation requests sent over the process signal bus. */
public final class LabNavigationIntent {

    public final String name;
    public final Object[] args;

    private LabNavigationIntent(String name, Object... args) {
        this.name = name != null ? name : "";
        this.args = args != null ? args : new Object[0];
    }

    public static LabNavigationIntent of(String name, Object... args) {
        return new LabNavigationIntent(name, args);
    }

    public String argString(int index) {
        if (index < 0 || index >= args.length || args[index] == null) {
            return "";
        }
        return String.valueOf(args[index]);
    }
}
