package pl.antoniuk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ConsumerConcurrentTest {
    private final double DELTA_EPSILON = 0.001;
    private TestTimestampProvider testTimestampProvider;
    private Consumer consumer;

    @BeforeEach
    void setUp() {
        testTimestampProvider = new TestTimestampProvider(0);
        consumer = new Consumer(testTimestampProvider, 5);
    }

    @Test
    void concurrent_accept_and_mean_operations_maintain_consistency() throws InterruptedException {
        int threadCount = 5;
        int operationsPerThread = 10000;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        AtomicReference<Exception> exceptionHolder = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < operationsPerThread; j++) {
                        if (j % 2 == 0) {
                            consumer.accept(1);
                        } else {
                            consumer.mean();
                        }
                        testTimestampProvider.advanceTime(1);
                    }
                } catch (Exception e) {
                    exceptionHolder.set(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        assertNull(exceptionHolder.get());

        OptionalDouble result = consumer.mean();
        assertTrue(result.isPresent());
        assertEquals(1.0, result.getAsDouble(), DELTA_EPSILON);
    }

    @Test
    void concurrent_operations_with_expiration_produce_correct_results() throws InterruptedException {
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<OptionalDouble> results = new ArrayList<>();
        AtomicReference<Exception> exceptionHolder = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            final int number = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    consumer.accept(number);
                    results.add(consumer.mean());
                    testTimestampProvider.advanceTime(1);
                    results.add(consumer.mean());
                } catch (Exception e) {
                    exceptionHolder.set(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        testTimestampProvider.advanceTime(1000);

        assertNull(exceptionHolder.get());
        OptionalDouble finalResult = consumer.mean();
        assertFalse(finalResult.isPresent());
        assertTrue(results.stream()
                .allMatch(r -> r.isPresent()
                        && r.getAsDouble() >= 0.0
                        && r.getAsDouble() <= 5.0));
    }

}