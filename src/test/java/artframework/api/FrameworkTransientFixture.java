package artframework.api;

import artframework.core.SignalListener;
import artframework.core.SignalSubscription;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Test fixture for interceptors attached to the framework singleton transient runtime. */
public final class FrameworkTransientFixture implements AutoCloseable {
    private final List<SignalSubscription> subscriptions = new ArrayList<SignalSubscription>();

    public SignalSubscription connect(String path, SignalListener listener) {
        SignalSubscription subscription = ArtFramework.transientSignalRuntimeForTests()
                .connect(path, listener);
        subscriptions.add(subscription);
        return subscription;
    }

    public SignalSubscription connect(Pattern path, SignalListener listener) {
        SignalSubscription subscription = ArtFramework.transientSignalRuntimeForTests()
                .connect(path, listener);
        subscriptions.add(subscription);
        return subscription;
    }

    @Override public void close() {
        for (SignalSubscription subscription : subscriptions) subscription.disconnect();
        subscriptions.clear();
    }
}
