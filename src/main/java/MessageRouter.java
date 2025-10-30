import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

/**
 * MessageRouter handles message routing to subscribers in a concurrent environment.
 *
 * This interface provides methods to subscribe to channels, publish messages,
 * and manage subscriber lifecycle safely across multiple threads.
 */
public interface MessageRouter extends AutoCloseable {

    /**
     * Subscribe creates a subscription to a channel.
     *
     * @param channel the channel name to subscribe to
     * @return a Subscription that can be used to receive messages
     * @throws MessageRouterException if subscription fails
     * @throws IllegalArgumentException if channel is null or empty
     */
    Subscription subscribe(String channel) throws MessageRouterException;

    /**
     * Publish sends a message to all subscribers of a channel.
     *
     * @param channel the channel to publish to
     * @param message the message bytes to send
     * @return a CompletableFuture that completes when publishing is done
     * @throws MessageRouterException if publishing fails
     * @throws IllegalArgumentException if channel is null or empty, or message is null
     */
    CompletableFuture<Void> publish(String channel, byte[] message) throws MessageRouterException;

    /**
     * Unsubscribe removes a specific subscription by its ID.
     * This is a bonus method - implement if time allows.
     *
     * @param subscriptionId the unique ID of the subscription to remove
     * @return a CompletableFuture that completes when unsubscription is done
     * @throws MessageRouterException if unsubscription fails
     */
    CompletableFuture<Void> unsubscribe(String subscriptionId) throws MessageRouterException;

    /**
     * Close shuts down the router and cleans up all resources.
     * This will close all active subscriptions.
     *
     * @throws Exception if closing fails
     */
    @Override
    void close() throws Exception;

    /**
     * Factory method to create a new MessageRouter instance.
     *
     * @return a new MessageRouter implementation
     */
    static MessageRouter create() {
        // TODO: Implement this
        throw new UnsupportedOperationException("not implemented");
    }
}

/**
 * Subscription represents a subscription to a channel that can receive messages.
 *
 * Implements Flow.Publisher to provide reactive streams support for message delivery.
 */
interface Subscription extends Flow.Publisher<byte[]>, AutoCloseable {

    /**
     * Returns a unique identifier for this subscription.
     *
     * @return the subscription ID
     */
    String getId();

    /**
     * Returns the channel name this subscription is listening to.
     *
     * @return the channel name
     */
    String getChannel();

    /**
     * Checks if this subscription is still active.
     *
     * @return true if the subscription is active, false if closed
     */
    boolean isActive();

    /**
     * Close closes the subscription and cleans up resources.
     * This will complete the subscriber streams and prevent further message delivery.
     *
     * @throws Exception if closing fails
     */
    @Override
    void close() throws Exception;
}

/**
 * Custom exception for MessageRouter operations.
 */
class MessageRouterException extends Exception {

    public MessageRouterException(String message) {
        super(message);
    }

    public MessageRouterException(String message, Throwable cause) {
        super(message, cause);
    }

    // Common error types as static factory methods
    public static MessageRouterException channelEmpty() {
        return new MessageRouterException("Channel name cannot be empty");
    }

    public static MessageRouterException subscribeFailed(String reason) {
        return new MessageRouterException("Failed to subscribe: " + reason);
    }

    public static MessageRouterException publishFailed(String reason) {
        return new MessageRouterException("Failed to publish message: " + reason);
    }

    public static MessageRouterException subscriptionClosed() {
        return new MessageRouterException("Subscription is closed");
    }

    public static MessageRouterException routerClosed() {
        return new MessageRouterException("MessageRouter is closed");
    }
}

/*
 * TODO: Implement a concrete MessageRouter
 * Consider:
 * - What data structures do you need to track subscriptions?
 * - How will you handle concurrent access safely?
 * - How will you route messages to the right subscribers?
 * - How will you handle subscriber lifecycle (creation/cleanup)?
 * - What happens when a subscriber's queue is full?
 * - Should you use ExecutorService for async operations?
 * - How will you handle backpressure in the reactive streams?
 *
 * TODO: Implement a concrete Subscription
 * Consider:
 * - How will you uniquely identify subscriptions?
 * - How will you deliver messages safely using Flow.Publisher?
 * - How will you handle cleanup when the subscription is closed?
 * - What should happen if the subscriber can't keep up (backpressure)?
 * - Should you buffer messages? If so, what's the buffer size limit?
 *
 * Concurrency Considerations:
 * - Use ConcurrentHashMap for thread-safe collections
 * - Consider using ReentrantReadWriteLock for read-heavy operations
 * - Use AtomicReference for lock-free operations where possible
 * - Be careful with synchronized blocks - keep them minimal
 * - Consider using CompletableFuture.supplyAsync for async operations
 *
 * Java-Specific Patterns:
 * - Implement Flow.Publisher for reactive message delivery
 * - Use try-with-resources for automatic resource management
 * - Consider using Optional for nullable returns
 * - Use method references and lambda expressions where appropriate
 * - Follow Java naming conventions (camelCase)
 */
