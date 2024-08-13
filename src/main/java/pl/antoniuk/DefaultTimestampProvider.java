package pl.antoniuk;

public class DefaultTimestampProvider implements TimestampProvider {
    @Override
    public long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
}
