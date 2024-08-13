package pl.antoniuk;

import java.util.LinkedList;
import java.util.OptionalDouble;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


class Consumer {
    private final LinkedList<NumberWithTimestamp> numbers = new LinkedList<>();
    private final TimestampProvider timestampProvider;
    private final long windowSizeInMillis;
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();
    private long currentSumOfNumbers = 0;

    public Consumer(TimestampProvider timestampProvider, long windowSizeInMillis) {
        this.timestampProvider = timestampProvider;
        this.windowSizeInMillis = windowSizeInMillis;
    }

    public Consumer(TimestampProvider timestampProvider) {
        this(timestampProvider, 5 * 60 * 1000);
    }

    /**
     * Called periodically to consume an integer.
     */
    public void accept(int number) {
        writeLock.lock();
        try {
            long currentTimestamp = timestampProvider.getCurrentTimestamp();
            numbers.addLast(new NumberWithTimestamp(number, currentTimestamp));
            currentSumOfNumbers += number;
            removeExpiredNumbers(currentTimestamp);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Returns the mean (aka average) of numbers consumed in the last 5 minute period (by default).
     *
     * @return An OptionalDouble containing the mean value if numbers have been consumed
     * in the last 5 minutes, or an empty OptionalDouble if no numbers have been consumed.
     */
    public OptionalDouble mean() {
        writeLock.lock();
        try {
            removeExpiredNumbers(timestampProvider.getCurrentTimestamp());
        } finally {
            writeLock.unlock();
        }

        readLock.lock();
        try {
            return numbers.isEmpty()
                    ? OptionalDouble.empty()
                    : OptionalDouble.of((double) currentSumOfNumbers / numbers.size());
        } finally {
            readLock.unlock();
        }
    }

    private void removeExpiredNumbers(long currentTimestamp) {
        long nowMinusOffset = currentTimestamp - windowSizeInMillis;
        while (!numbers.isEmpty() && numbers.getFirst().timestamp < nowMinusOffset) {
            NumberWithTimestamp expired = numbers.removeFirst();
            currentSumOfNumbers -= expired.number;
        }
    }

    private static class NumberWithTimestamp {
        int number;
        long timestamp;

        NumberWithTimestamp(int number, long timestamp) {
            this.number = number;
            this.timestamp = timestamp;
        }
    }
}