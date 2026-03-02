package ringbuffer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RingBufferTest {

    @Test
    void readersCanReadIndependently() {
        RingBuffer<Integer> buffer = new RingBuffer<>(3);
        Reader<Integer> readerA = buffer.createReader();
        Reader<Integer> readerB = buffer.createReader();

        buffer.write(10);
        buffer.write(20);

        ReadResult<Integer> aFirst = readerA.read();
        ReadResult<Integer> bFirst = readerB.read();
        ReadResult<Integer> aSecond = readerA.read();
        ReadResult<Integer> bSecond = readerB.read();

        assertEquals(10, aFirst.value().orElseThrow());
        assertEquals(10, bFirst.value().orElseThrow());
        assertEquals(20, aSecond.value().orElseThrow());
        assertEquals(20, bSecond.value().orElseThrow());
    }

    @Test
    void slowReaderSkipsOverwrittenItems() {
        RingBuffer<Integer> buffer = new RingBuffer<>(3);
        Reader<Integer> slowReader = buffer.createReader();

        buffer.write(1);
        buffer.write(2);
        buffer.write(3);
        buffer.write(4);
        buffer.write(5);

        ReadResult<Integer> result = slowReader.read();

        assertTrue(result.hasValue());
        assertEquals(3, result.value().orElseThrow());
        assertEquals(2, result.skippedCount());
    }

    @Test
    void readingWithoutNewDataReturnsEmpty() {
        RingBuffer<String> buffer = new RingBuffer<>(2);
        Reader<String> reader = buffer.createReader();

        ReadResult<String> first = reader.read();

        assertFalse(first.hasValue());
        assertEquals(0, first.skippedCount());

        buffer.write("A");
        ReadResult<String> second = reader.read();
        ReadResult<String> third = reader.read();

        assertEquals("A", second.value().orElseThrow());
        assertFalse(third.hasValue());
        assertEquals(0, third.skippedCount());
    }
}
