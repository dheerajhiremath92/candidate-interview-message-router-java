import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An individual subscription to a channel.
 *
 * This class handles the reactive streams part of the problem,
 * wrapping a SubmissionPublisher to manage subscribers and backpressure.
 */
class RouterSubscription implements Subscription {

    private final String id;
    private final String channel;
    private final MessageRouterImpl router;
    private final AtomicBoolean active = new AtomicBoolean(true);

    // The publisher that handles all the Flow.Publisher logic
    private final SubmissionPublisher<byte[]> publisher;

    public RouterSubscription(String id, String channel, MessageRouterImpl router, ExecutorService executor) {
        this.id = id;
        this.channel = channel;
        this.router = router;

        // Use the shared executor from the router
        this.publisher = new SubmissionPublisher<>(executor, Flow.defaultBufferSize());
    }

    /**
     * Internal method for the router to push a message to this sub.
     */
    void sendMessage(byte[] message) {
        if (active.get() && !publisher.isClosed()) {
            // submit() respects backpressure and will block the
            // calling (executor) thread if the subscriber is slow.
            // This is the desired behavior.
            publisher.submit(message);
        }
    }

    @Override
    public void subscribe(Flow.Subscriber<? super byte[]> subscriber) {
        // Just delegate to our internal publisher
        publisher.subscribe(subscriber);
    }

    @Override
    public void close() throws Exception {
        if (!active.compareAndSet(true, false)) {
            return; // Already closed
        }

        try {
            // Signal onComplete to all subscribers
            publisher.close();
        } finally {
            // Tell the router to remove us from its lists
            router.removeSubscription(this);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getChannel() {
        return channel;
    }

    @Override
    public boolean isActive() {
        return active.get();
    }
}