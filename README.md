# Message Router Interview Problem - Java Implementation

## Overview

This is a coding interview problem designed to evaluate backend engineering skills, particularly around:
- Concurrency-safe design using Java's concurrent utilities
- Code correctness and readability
- Testing approach using JUnit 5
- Understanding of reactive streams (Flow API)
- Collaboration skills

**Time Allocation:** 45-60 minutes total
- Problem introduction: 5-10 minutes
- Coding session: 30-45 minutes
- Discussion and wrap-up: 5-10 minutes

## Problem Statement

Build a **MessageRouter** that can safely multiplex messages to multiple subscribers in a concurrent environment using Java's concurrency primitives and reactive streams.

### Requirements

Your MessageRouter should:

1. **Accept subscriptions** to named channels and return a way to receive messages
2. **Route incoming messages** to all current subscribers of that channel
3. **Handle concurrent access** safely (multiple threads subscribing/publishing simultaneously)
4. **Manage subscriber lifecycle** properly (creation, cleanup, resource management)
5. **Use Java's Flow API** for reactive message delivery with backpressure support

### Starting Point

The scaffold provides:
- `MessageRouter` interface with subscribe/publish/unsubscribe methods
- `Subscription` interface implementing `Flow.Publisher<byte[]>`
- `MessageRouterException` custom exception with factory methods
- Comprehensive JUnit 5 test suite with concurrency tests

### Implementation Phases

**Phase 1: Basic Structure (10-15 minutes)**
- Define concrete classes implementing the interfaces
- Basic subscribe/publish functionality using reactive streams
- Simple data structures for tracking subscriptions

**Phase 2: Concurrency Safety (10-15 minutes)**
- Add proper synchronization primitives
- Handle concurrent map access safely
- Ensure thread-safe operations

**Phase 3: Testing & Edge Cases (10-15 minutes)**
- Run provided tests and make them pass
- Handle edge cases (empty channels, no subscribers, etc.)
- Implement proper reactive streams backpressure handling

**Bonus (if time allows):**
- Implement unsubscribe functionality
- Add metrics/observability hooks
- Discuss scaling considerations

## Java-Specific Considerations

### Concurrency Tools to Consider
- `ConcurrentHashMap` for thread-safe collections
- `ReentrantReadWriteLock` for read-heavy operations
- `AtomicReference`/`AtomicLong` for lock-free operations
- `CompletableFuture` for asynchronous operations
- `ExecutorService` for thread pool management

### Reactive Streams
- Implement `Flow.Publisher<byte[]>` for subscriptions
- Handle `Flow.Subscriber` lifecycle properly
- Implement backpressure using `Flow.Subscription.request()`
- Manage subscriber completion and error handling

### Resource Management
- Use `AutoCloseable` for automatic resource cleanup
- Implement try-with-resources patterns
- Proper exception handling with custom exceptions

## Project Structure

```
candidate/
├── pom.xml                           # Maven build configuration
├── src/
│   ├── main/java/
│   │   └── MessageRouter.java        # Interfaces and exception definitions
│   └── test/java/
│       └── MessageRouterTest.java    # Comprehensive test suite
└── README.md                         # This file
```

## Running the Code

```bash
# Compile and run tests
mvn test

# Run tests with verbose output
mvn test -Dtest=MessageRouterTest

# Run specific test method
mvn test -Dtest=MessageRouterTest#testBasicSubscribeAndPublish

# Clean and compile
mvn clean compile

# Package the project
mvn package
```

## Getting Started

1. **Examine the interfaces** in `MessageRouter.java`
2. **Review the test cases** in `MessageRouterTest.java` to understand expected behavior
3. **Implement the concrete classes** following the TODO comments
4. **Run tests frequently** to verify your implementation
5. **Focus on thread safety** - this is a key evaluation criterion

## Evaluation Criteria

### Primary Focus Areas

**Concurrency Safety (Critical)**
- Proper use of Java concurrency utilities
- Thread-safe collection operations
- Avoids race conditions
- Safe reactive streams implementation

**Correctness**
- Messages reach all current subscribers via reactive streams
- Proper Flow.Publisher implementation
- Correct exception handling and validation
- Edge case handling (no subscribers, closed router)

**Code Quality**
- Clean, idiomatic Java code
- Proper use of modern Java features
- Meaningful naming following Java conventions
- Appropriate abstraction levels

**Testing Approach**
- Understanding of how to make the provided tests pass
- Consideration of concurrent testing patterns
- Thoughtful approach to reactive streams testing

## Key Questions to Consider

**Initial Design:**
- How would you structure this using Java's concurrent collections?
- What's your approach to implementing the Flow.Publisher interface?

**Concurrency:**
- How do you ensure thread safety when multiple threads subscribe simultaneously?
- What Java concurrency primitives would you use here?

**Reactive Streams:**
- How do you handle backpressure in your subscription implementation?
- What happens when a subscriber can't keep up with message delivery?

**Lifecycle Management:**
- How do you implement proper resource cleanup with AutoCloseable?
- What's your strategy for graceful shutdown?

## Expected Test Coverage

The provided test suite covers:
- Basic subscribe and publish functionality
- Multiple subscribers receiving the same message
- Concurrent access safety
- Publishing to channels with no subscribers
- Subscription lifecycle management
- Error cases and input validation
- Valid channel name formats
- Router close behavior
- Performance with many subscribers

## Tips for Success

- **Start simple** - get basic functionality working first
- **Think about thread safety** from the beginning
- **Use the tests** as your guide - they define the expected behavior
- **Ask questions** if requirements are unclear
- **Explain your thinking** as you work through the problem
- **Consider trade-offs** between different approaches

## Dependencies

The project uses:
- **Java 17+** for modern language features
- **JUnit 5** for testing framework
- **Maven** for build management
- **Flow API** for reactive streams (built into Java 9+)

No external reactive libraries are included to focus on core Java skills.

Good luck!