import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for MessageRouter implementation.
 * Tests concurrency safety, correctness, and edge cases.
 */
public class MessageRouterTest {

    private MessageRouter router;

    @BeforeEach
    void setUp() {
        router = new MessageRouterImpl();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (router != null) {
            router.close();
        }
    }

    @Test
    @DisplayName("Basic subscribe and publish functionality")
    void testBasicSubscribeAndPublish() throws Exception {
        String channel = "test-channel";
        byte[] message = "hello world".getBytes();

        // TODO: Subscribe to a channel
        Subscription subscription = router.subscribe(channel);
        assertNotNull(subscription);
        assertTrue(subscription.isActive());
        assertEquals(channel, subscription.getChannel());
        assertNotNull(subscription.getId());
        assertFalse(subscription.getId().isEmpty());

        // Set up message collection
        List<byte[]> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(1);

        subscription.subscribe(new Flow.Subscriber<byte[]>() {
            @Override
            public void onSubscribe(Flow.Subscription s) {
                s.request(Long.MAX_VALUE); // Request unlimited messages
            }

            @Override
            public void onNext(byte[] item) {
                receivedMessages.add(item);
                messageLatch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                fail("Unexpected error: " + throwable.getMessage());
            }

            @Override
            public void onComplete() {
                // Subscription completed
            }
        });

        // TODO: Publish a message
        CompletableFuture<Void> publishFuture = router.publish(channel, message);
        publishFuture.get(5, TimeUnit.SECONDS);

        // TODO: Verify the message is received
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Message should be received within timeout");
        assertEquals(1, receivedMessages.size());
        assertArrayEquals(message, receivedMessages.get(0));

        subscription.close();
    }

    @Test
    @DisplayName("Multiple subscribers receive the same message")
    void testMultipleSubscribers() throws Exception {
        String channel = "multi-channel";
        byte[] message = "broadcast message".getBytes();
        int numSubscribers = 3;

        // TODO: Create multiple subscribers
        List<Subscription> subscriptions = new ArrayList<>();
        List<List<byte[]>> receivedMessages = new ArrayList<>();
        List<CountDownLatch> latches = new ArrayList<>();

        for (int i = 0; i < numSubscribers; i++) {
            Subscription sub = router.subscribe(channel);
            subscriptions.add(sub);

            List<byte[]> messages = new ArrayList<>();
            receivedMessages.add(messages);

            CountDownLatch latch = new CountDownLatch(1);
            latches.add(latch);

            final int subscriberIndex = i;
            sub.subscribe(new Flow.Subscriber<byte[]>() {
                @Override
                public void onSubscribe(Flow.Subscription s) {
                    s.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(byte[] item) {
                    messages.add(item);
                    latch.countDown();
                }

                @Override
                public void onError(Throwable throwable) {
                    fail("Subscriber " + subscriberIndex + " error: " + throwable.getMessage());
                }

                @Override
                public void onComplete() {
                    // Completed
                }
            });
        }

        // TODO: Publish one message
        CompletableFuture<Void> publishFuture = router.publish(channel, message);
        publishFuture.get(5, TimeUnit.SECONDS);

        // TODO: Verify all subscribers receive the message
        for (int i = 0; i < numSubscribers; i++) {
            assertTrue(latches.get(i).await(5, TimeUnit.SECONDS),
                    "Subscriber " + i + " should receive message within timeout");
            assertEquals(1, receivedMessages.get(i).size(),
                    "Subscriber " + i + " should receive exactly one message");
            assertArrayEquals(message, receivedMessages.get(i).get(0),
                    "Subscriber " + i + " should receive correct message");
        }

        // Clean up
        for (Subscription sub : subscriptions) {
            sub.close();
        }
    }

    @Test
    @DisplayName("Concurrent access safety")
    void testConcurrentAccess() throws Exception {
        String channel = "concurrent-channel";
        int numPublishers = 5;
        int messagesPerPublisher = 10;
        int expectedMessages = numPublishers * messagesPerPublisher;

        // TODO: Test concurrent subscribing and publishing
        Subscription subscription = router.subscribe(channel);

        // Count received messages
        AtomicLong receivedCount = new AtomicLong(0);
        AtomicLong publishedCount = new AtomicLong(0);
        CountDownLatch completionLatch = new CountDownLatch(1);

        subscription.subscribe(new Flow.Subscriber<byte[]>() {
            @Override
            public void onSubscribe(Flow.Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(byte[] item) {
                long count = receivedCount.incrementAndGet();
                if (count == expectedMessages) {
                    completionLatch.countDown();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                fail("Unexpected error: " + throwable.getMessage());
            }

            @Override
            public void onComplete() {
                completionLatch.countDown();
            }
        });

        // Start concurrent publishers
        ExecutorService executor = Executors.newFixedThreadPool(numPublishers);
        List<CompletableFuture<Void>> publishTasks = new ArrayList<>();

        for (int publisherId = 0; publisherId < numPublishers; publisherId++) {
            final int id = publisherId;
            CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < messagesPerPublisher; j++) {
                    try {
                        String messageText = String.format("message from publisher %d msg %d", id, j);
                        byte[] message = messageText.getBytes();

                        CompletableFuture<Void> publishFuture = router.publish(channel, message);
                        publishFuture.get(5, TimeUnit.SECONDS);
                        publishedCount.incrementAndGet();
                    } catch (Exception e) {
                        fail("Publisher " + id + " failed to publish message " + j + ": " + e.getMessage());
                    }
                }
            }, executor);
            publishTasks.add(task);
        }

        // Wait for all publishers to finish
        CompletableFuture.allOf(publishTasks.toArray(new CompletableFuture[0]))
                .get(30, TimeUnit.SECONDS);

        // Wait for all messages to be received (with timeout)
        assertTrue(completionLatch.await(30, TimeUnit.SECONDS),
                "All messages should be received within timeout");

        long actualPublished = publishedCount.get();
        long actualReceived = receivedCount.get();

        assertEquals(expectedMessages, actualPublished, "All messages should be published");
        assertEquals(expectedMessages, actualReceived, "All messages should be received");

        System.out.printf("Successfully delivered %d out of %d messages%n", actualReceived, actualPublished);

        executor.shutdown();
        subscription.close();
    }

    @Test
    @DisplayName("Publishing to channel with no subscribers")
    void testNoSubscribers() throws Exception {
        String channel = "empty-channel";
        byte[] message = "message to nowhere".getBytes();

        // TODO: Publishing to a channel with no subscribers should not error
        assertDoesNotThrow(() -> {
            CompletableFuture<Void> future = router.publish(channel, message);
            future.get(5, TimeUnit.SECONDS);
        }, "Publishing to empty channel should not throw exception");
    }

    @Test
    @DisplayName("Subscription lifecycle management")
    void testSubscriptionLifecycle() throws Exception {
        String channel = "lifecycle-channel";

        // TODO: Test subscription creation and cleanup
        Subscription subscription = router.subscribe(channel);
        assertNotNull(subscription);
        assertTrue(subscription.isActive());
        assertNotNull(subscription.getId());
        assertFalse(subscription.getId().isEmpty());
        assertEquals(channel, subscription.getChannel());

        // Set up completion tracking
        CountDownLatch completionLatch = new CountDownLatch(1);
        AtomicInteger messageCount = new AtomicInteger(0);

        subscription.subscribe(new Flow.Subscriber<byte[]>() {
            @Override
            public void onSubscribe(Flow.Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(byte[] item) {
                messageCount.incrementAndGet();
            }

            @Override
            public void onError(Throwable throwable) {
                fail("Unexpected error: " + throwable.getMessage());
            }

            @Override
            public void onComplete() {
                completionLatch.countDown();
            }
        });

        // Close the subscription
        subscription.close();
        assertFalse(subscription.isActive(), "Subscription should be inactive after close");

        // TODO: Verify that the stream is completed
        assertTrue(completionLatch.await(5, TimeUnit.SECONDS),
                "Subscription stream should complete after close");

        // Verify no messages are received after closing
        router.publish(channel, "test message".getBytes()).get(1, TimeUnit.SECONDS);
        assertEquals(0, messageCount.get(), "No messages should be received after close");
    }

    @Test
    @DisplayName("Error cases and validation")
    void testErrorCases() {
        // TODO: Test error cases

        // Test null/empty channel name for subscribe
        assertThrows(IllegalArgumentException.class, () -> router.subscribe(null),
                "Subscribe with null channel should throw IllegalArgumentException");

        assertThrows(IllegalArgumentException.class, () -> router.subscribe(""),
                "Subscribe with empty channel should throw IllegalArgumentException");

        assertThrows(IllegalArgumentException.class, () -> router.subscribe("   "),
                "Subscribe with whitespace-only channel should throw IllegalArgumentException");

        // Test null/empty channel name for publish
        assertThrows(IllegalArgumentException.class, () -> router.publish(null, "test".getBytes()),
                "Publish with null channel should throw IllegalArgumentException");

        assertThrows(IllegalArgumentException.class, () -> router.publish("", "test".getBytes()),
                "Publish with empty channel should throw IllegalArgumentException");

        assertThrows(IllegalArgumentException.class, () -> router.publish("   ", "test".getBytes()),
                "Publish with whitespace-only channel should throw IllegalArgumentException");

        // Test null message
        assertThrows(IllegalArgumentException.class, () -> router.publish("test-channel", null),
                "Publish with null message should throw IllegalArgumentException");
    }

    @ParameterizedTest
    @ValueSource(strings = {"test-channel", "channel.with.dots", "channel_with_underscores", "channel-with-dashes"})
    @DisplayName("Valid channel names")
    void testValidChannelNames(String channelName) throws Exception {
        assertDoesNotThrow(() -> {
            Subscription sub = router.subscribe(channelName);
            assertEquals(channelName, sub.getChannel());
            sub.close();
        }, "Valid channel name should not throw exception: " + channelName);
    }

    @Test
    @DisplayName("Router close behavior")
    void testRouterClose() throws Exception {
        String channel = "close-test-channel";
        Subscription subscription = router.subscribe(channel);

        assertTrue(subscription.isActive());

        // Close the router
        router.close();

        // Subscription should be closed
        assertFalse(subscription.isActive());

        // Further operations should fail
        assertThrows(Exception.class, () -> router.subscribe("new-channel"),
                "Subscribe after router close should throw exception");

        assertThrows(Exception.class, () -> router.publish(channel, "test".getBytes()).get(1, TimeUnit.SECONDS),
                "Publish after router close should throw exception");
    }

    // Benchmark-style test for performance awareness
    @Test
    @DisplayName("Performance test with many subscribers")
    void testManySubscribers() throws Exception {
        String channel = "perf-channel";
        int numSubscribers = 100;
        byte[] message = "performance test message".getBytes();

        List<Subscription> subscriptions = new ArrayList<>();
        List<CountDownLatch> latches = new ArrayList<>();

        // Create many subscribers
        for (int i = 0; i < numSubscribers; i++) {
            Subscription sub = router.subscribe(channel);
            subscriptions.add(sub);

            CountDownLatch latch = new CountDownLatch(1);
            latches.add(latch);

            sub.subscribe(new Flow.Subscriber<byte[]>() {
                @Override
                public void onSubscribe(Flow.Subscription s) {
                    s.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(byte[] item) {
                    latch.countDown();
                }

                @Override
                public void onError(Throwable throwable) {
                    // Ignore for performance test
                }

                @Override
                public void onComplete() {
                    // Ignore for performance test
                }
            });
        }

        // Measure publish time
        long startTime = System.nanoTime();
        router.publish(channel, message).get(10, TimeUnit.SECONDS);
        long endTime = System.nanoTime();

        // Verify all received the message
        for (CountDownLatch latch : latches) {
            assertTrue(latch.await(10, TimeUnit.SECONDS));
        }

        long durationMs = (endTime - startTime) / 1_000_000;
        System.out.printf("Published to %d subscribers in %d ms%n", numSubscribers, durationMs);

        // Clean up
        for (Subscription sub : subscriptions) {
            sub.close();
        }

        // Performance assertion - should complete reasonably quickly
        assertTrue(durationMs < 5000, "Publishing to " + numSubscribers + " subscribers should take less than 5 seconds");
    }

    // TODO: Additional test ideas:
    // - Test with very large messages
    // - Test with many different channels simultaneously
    // - Test unsubscribe functionality (if implemented)
    // - Test message ordering guarantees (if any)
    // - Test backpressure handling in reactive streams
    // - Test graceful shutdown scenarios
    // - Memory leak detection tests
    // - Stress tests with long-running scenarios
}
