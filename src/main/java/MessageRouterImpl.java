import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Concrete implementation of the MessageRouter.
 * Uses synchronized lists to manage channel subscriptions.
 */
class MessageRouterImpl implements MessageRouter {

    // Map channel names to their list of subscriptions
    private final Map<String, List<RouterSubscription>> channelSubs = new ConcurrentHashMap<>();

    // Map subscription IDs to the subscription object for quick removal
    private final Map<String, RouterSubscription> subsById = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    @Override
    public Subscription subscribe(String channel) throws MessageRouterException {
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("Channel must not be null or blank");
        }
        if (isClosed.get()) {
            throw MessageRouterException.routerClosed();
        }

        String id = UUID.randomUUID().toString();
        RouterSubscription subscription = new RouterSubscription(id, channel, this, executor);

        subsById.put(id, subscription);

        // Get or create the list for this channel
        List<RouterSubscription> subs = channelSubs.computeIfAbsent(
                channel,
                k -> Collections.synchronizedList(new ArrayList<>())
        );
        subs.add(subscription);

        return subscription;
    }

    @Override
    public CompletableFuture<Void> publish(String channel, byte[] message) throws MessageRouterException {
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("Channel must not be null or blank");
        }
        if (message == null) {
            throw new IllegalArgumentException("Message must not be null");
        }
        if (isClosed.get()) {
            throw MessageRouterException.routerClosed();
        }

        // Run this on a background thread
        return CompletableFuture.runAsync(() -> {
            List<RouterSubscription> subs = channelSubs.get(channel);

            if (subs != null) {
                // We have to lock the list to iterate it safely
                synchronized (subs) {
                    for (RouterSubscription sub : subs) {
                        sub.sendMessage(message);
                    }
                }
            }
        });
    }

    /**
     * Internal callback for subscriptions to de-register themselves.
     */
    void removeSubscription(RouterSubscription subscription) {
        if (subscription == null) return;

        subsById.remove(subscription.getId());

        List<RouterSubscription> subs = channelSubs.get(subscription.getChannel());
        if (subs != null) {
            subs.remove(subscription);
            // TODO: We could clean up the channelSubs map if this list is now empty,
            // but probably not necessary right now.
        }
    }

    @Override
    public void close() throws Exception {
        if (!isClosed.compareAndSet(false, true)) {
            return; // Already closing
        }

        // Stop new tasks
        executor.shutdown();

        // Close all active subscriptions
        // We iterate a copy since .close() will modify the underlying subsById map
        for (RouterSubscription sub : new ArrayList<>(subsById.values())) {
            sub.close();
        }

        // Clear all state
        subsById.clear();
        channelSubs.clear();
    }

    @Override
    public CompletableFuture<Void> unsubscribe(String subscriptionId) throws MessageRouterException {
        if (isClosed.get()) {
            return CompletableFuture.failedFuture(MessageRouterException.routerClosed());
        }

        RouterSubscription sub = subsById.get(subscriptionId);

        if (sub != null) {
            try {
                // Closing the sub will trigger the removeSubscription callback
                sub.close();
            } catch (Exception e) {
                return CompletableFuture.failedFuture(new MessageRouterException("Failed to unsubscribe", e));
            }
        }
        return CompletableFuture.completedFuture(null);
    }
}