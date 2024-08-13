package pl.antoniuk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;

class ConsumerTest {
    private final double DELTA_EPSILON = 0.001;
    private TestTimestampProvider testTimestampProvider;
    private Consumer consumer;

    @BeforeEach
    void setUp() {
        testTimestampProvider = new TestTimestampProvider(0);
        consumer = new Consumer(testTimestampProvider, 5);
    }

    @Test
    void mean_correctly_calculates_average_within_time_window() {
        acceptAndAdvanceTime(consumer, 10, 1);
        acceptAndAdvanceTime(consumer, 20, 1);
        acceptAndAdvanceTime(consumer, 30, 1);
        OptionalDouble result = consumer.mean();
        assertTrue(result.isPresent());
        assertEquals(20.0, result.getAsDouble(), DELTA_EPSILON);
    }

    @Test
    void mean_returns_empty_when_no_numbers_consumed() {
        OptionalDouble result = consumer.mean();
        assertFalse(result.isPresent());
    }

    @Test
    void mean_excludes_expired_entries() {
        acceptAndAdvanceTime(consumer, 10, 1);
        acceptAndAdvanceTime(consumer, 20, 1);
        acceptAndAdvanceTime(consumer, 30, 1);
        acceptAndAdvanceTime(consumer, 40, 5);
        acceptAndAdvanceTime(consumer, 50, 1);
        OptionalDouble result = consumer.mean();
        assertTrue(result.isPresent());
        assertEquals(50.0, result.getAsDouble(), DELTA_EPSILON);
    }

    @Test
    void custom_window_size_correctly_limits_included_entries() {
        Consumer customConsumer = new Consumer(testTimestampProvider, 2);
        acceptAndAdvanceTime(customConsumer, 10, 1);
        acceptAndAdvanceTime(customConsumer, 20, 1);
        acceptAndAdvanceTime(customConsumer, 30, 1);
        OptionalDouble result = customConsumer.mean();
        assertTrue(result.isPresent());
        assertEquals(25.0, result.getAsDouble(), DELTA_EPSILON);
    }

    private void acceptAndAdvanceTime(Consumer consumer, int acceptValue, long advance) {
        consumer.accept(acceptValue);
        testTimestampProvider.advanceTime(advance);
    }

}