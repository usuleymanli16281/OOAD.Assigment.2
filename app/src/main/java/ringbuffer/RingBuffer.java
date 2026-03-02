package ringbuffer;

import java.util.ArrayList;
import java.util.List;

public class RingBuffer<T> {
    private final Object[] buffer;
    private long nextWriteSequence;
    private final List<Reader<T>> readers;

    public RingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be > 0");
        }
        this.buffer = new Object[capacity];
        this.nextWriteSequence = 0;
        this.readers = new ArrayList<>();
    }

    public synchronized Reader<T> createReader() {
        Reader<T> reader = new Reader<>(this, oldestAvailableSequence());
        readers.add(reader);
        return reader;
    }

    public synchronized void write(T value) {
        int index = indexFor(nextWriteSequence);
        buffer[index] = value;
        nextWriteSequence++;
    }

    synchronized ReadResult<T> readFor(Reader<T> reader) {
        long readerSequence = reader.getNextSequence();
        long oldest = oldestAvailableSequence();
        long skipped = 0;


        //* Reader was pointing to the value that was already overwritten
        // Therefore, that readers pointer is forwarded to the oldest available value */
        if (readerSequence < oldest) {  
            skipped = oldest - readerSequence;
            readerSequence = oldest;
        }

        if (readerSequence >= nextWriteSequence) {
            reader.setNextSequence(readerSequence);
            return ReadResult.empty(skipped);
        }

        @SuppressWarnings("unchecked")
        T value = (T) buffer[indexFor(readerSequence)];
        reader.setNextSequence(readerSequence + 1);
        return ReadResult.withValue(value, skipped);
    }

    public synchronized int capacity() {
        return buffer.length;
    }

    public synchronized int size() {
        return (int) Math.min(nextWriteSequence, buffer.length);
    }

    public synchronized long totalWrites() {
        return nextWriteSequence;
    }

    public synchronized int readerCount() {
        return readers.size();
    }

    private long oldestAvailableSequence() {
        return Math.max(0, nextWriteSequence - buffer.length);
    }

    private int indexFor(long sequence) {
        return (int) (sequence % buffer.length);
    }
}
