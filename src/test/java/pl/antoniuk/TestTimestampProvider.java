package pl.antoniuk;

import java.util.concurrent.atomic.AtomicLong;

class TestTimestampProvider implements TimestampProvider {
    private final AtomicLong currentTimeStamp;

    TestTimestampProvider(long currentTimeStamp) {
        this.currentTimeStamp = new AtomicLong(currentTimeStamp);
    }

    @Override
    public long getCurrentTimestamp() {
        return currentTimeStamp.get();
    }

    public void advanceTime(long milliseconds) {
        this.currentTimeStamp.addAndGet(milliseconds);
    }
}
