package ringbuffer;

public class Reader<T> {
    private final RingBuffer<T> ringBuffer;
    private long nextSequence;

    Reader(RingBuffer<T> ringBuffer, long startSequence) {
        this.ringBuffer = ringBuffer;
        this.nextSequence = startSequence;
    }

    public ReadResult<T> read() {
        return ringBuffer.readFor(this);
    }

    synchronized long getNextSequence() {
        return nextSequence;
    }

    synchronized void setNextSequence(long nextSequence) {
        this.nextSequence = nextSequence;
    }
}
