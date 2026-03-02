package ringbuffer;

import java.util.Optional;

public class ReadResult<T> {
    private final T value;
    private final long skippedCount;

    private ReadResult(T value, long skippedCount) {
        this.value = value;
        this.skippedCount = skippedCount;
    }

    public static <T> ReadResult<T> withValue(T value, long skippedCount) {
        return new ReadResult<>(value, skippedCount);
    }

    public static <T> ReadResult<T> empty(long skippedCount) {
        return new ReadResult<>(null, skippedCount);
    }

    public Optional<T> value() {
        return Optional.ofNullable(value);
    }

    public long skippedCount() {
        return skippedCount;
    }

    public boolean hasValue() {
        return value != null;
    }
}
