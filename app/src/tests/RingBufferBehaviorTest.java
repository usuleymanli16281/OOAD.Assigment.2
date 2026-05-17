package ringbuffer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingBufferBehaviorTest {

    @Test
    void rejectsNonPositiveCapacity() {
        IllegalArgumentException zero = assertThrows(IllegalArgumentException.class, () -> new RingBuffer<>(0));
        IllegalArgumentException negative = assertThrows(IllegalArgumentException.class, () -> new RingBuffer<>(-1));
        IllegalArgumentException minimum = assertThrows(IllegalArgumentException.class, () -> new RingBuffer<>(Integer.MIN_VALUE));

        assertEquals("Capacity must be > 0", zero.getMessage());
        assertEquals("Capacity must be > 0", negative.getMessage());
        assertEquals("Capacity must be > 0", minimum.getMessage());
    }

    @Test
    void acceptsLargeCapacity() {
        RingBuffer<Integer> buffer = new RingBuffer<>(10_000);

        assertEquals(10_000, buffer.capacity());
        assertEquals(0, buffer.size());
        assertEquals(0, buffer.totalWrites());
    }

    @Test
    void reportsCapacitySizeTotalWritesAndReaderCount() {
        RingBuffer<String> buffer = new RingBuffer<>(2);

        assertEquals(2, buffer.capacity());
        assertEquals(0, buffer.size());
        assertEquals(0, buffer.totalWrites());
        assertEquals(0, buffer.readerCount());

        Reader<String> firstReader = buffer.createReader();
        Reader<String> secondReader = buffer.createReader();

        assertEquals(2, buffer.readerCount());
        assertFalse(firstReader.read().hasValue());
        assertFalse(secondReader.read().hasValue());

        buffer.write("A");
        buffer.write("B");
        buffer.write("C");

        assertEquals(2, buffer.size());
        assertEquals(3, buffer.totalWrites());
    }

    @Test
    void readerCreatedBeforeWritesConsumesValuesInWriteOrderThenReturnsEmpty() {
        RingBuffer<Integer> buffer = new RingBuffer<>(4);
        Reader<Integer> reader = buffer.createReader();

        assertFalse(reader.read().hasValue());

        buffer.write(10);
        buffer.write(20);
        buffer.write(30);

        ReadResult<Integer> first = reader.read();
        ReadResult<Integer> second = reader.read();
        ReadResult<Integer> third = reader.read();
        ReadResult<Integer> fourth = reader.read();

        assertTrue(first.hasValue());
        assertEquals(10, first.value().orElseThrow());
        assertEquals(0, first.skippedCount());
        assertEquals(20, second.value().orElseThrow());
        assertEquals(0, second.skippedCount());
        assertEquals(30, third.value().orElseThrow());
        assertEquals(0, third.skippedCount());
        assertFalse(fourth.hasValue());
        assertEquals(0, fourth.skippedCount());
    }

    @Test
    void drainsFullBufferInWriteOrder() {
        int capacity = 16;
        RingBuffer<Integer> buffer = new RingBuffer<>(capacity);
        Reader<Integer> reader = buffer.createReader();

        for (int i = 0; i < capacity; i++) {
            buffer.write(i);
        }

        for (int i = 0; i < capacity; i++) {
            ReadResult<Integer> result = reader.read();

            assertTrue(result.hasValue());
            assertEquals(i, result.value().orElseThrow());
            assertEquals(0, result.skippedCount());
        }

        assertFalse(reader.read().hasValue());
    }

    @Test
    void interleavedWriteAndReadKeepsReaderCaughtUp() {
        RingBuffer<Integer> buffer = new RingBuffer<>(4);
        Reader<Integer> reader = buffer.createReader();

        for (int i = 0; i < 100; i++) {
            buffer.write(i);

            ReadResult<Integer> result = reader.read();

            assertTrue(result.hasValue());
            assertEquals(i, result.value().orElseThrow());
            assertEquals(0, result.skippedCount());
        }

        assertFalse(reader.read().hasValue());
        assertEquals(100, buffer.totalWrites());
        assertEquals(4, buffer.size());
    }

    @Test
    void readersMaintainIndependentPositions() {
        RingBuffer<Integer> buffer = new RingBuffer<>(3);
        Reader<Integer> firstReader = buffer.createReader();
        Reader<Integer> secondReader = buffer.createReader();

        buffer.write(1);
        buffer.write(2);

        assertEquals(1, firstReader.read().value().orElseThrow());
        assertEquals(2, firstReader.read().value().orElseThrow());
        assertEquals(1, secondReader.read().value().orElseThrow());

        buffer.write(3);

        assertEquals(2, secondReader.read().value().orElseThrow());
        assertEquals(3, secondReader.read().value().orElseThrow());
        assertEquals(3, firstReader.read().value().orElseThrow());
    }

    @Test
    void manyReadersSeeSameStreamWhenCapacityIsNotExceeded() {
        RingBuffer<Integer> buffer = new RingBuffer<>(64);
        Reader<Integer> first = buffer.createReader();
        Reader<Integer> second = buffer.createReader();
        Reader<Integer> third = buffer.createReader();

        for (int i = 0; i < 50; i++) {
            buffer.write(i);
        }

        for (int i = 0; i < 50; i++) {
            assertEquals(i, first.read().value().orElseThrow());
            assertEquals(i, second.read().value().orElseThrow());
            assertEquals(i, third.read().value().orElseThrow());
        }

        assertFalse(first.read().hasValue());
        assertFalse(second.read().hasValue());
        assertFalse(third.read().hasValue());
    }

    @Test
    void slowReaderReceivesSkipCountOnceAndThenContinuesFromOldestAvailableValue() {
        RingBuffer<Integer> buffer = new RingBuffer<>(3);
        Reader<Integer> slowReader = buffer.createReader();

        buffer.write(1);
        buffer.write(2);
        buffer.write(3);
        buffer.write(4);
        buffer.write(5);

        ReadResult<Integer> first = slowReader.read();
        ReadResult<Integer> second = slowReader.read();
        ReadResult<Integer> third = slowReader.read();
        ReadResult<Integer> fourth = slowReader.read();

        assertTrue(first.hasValue());
        assertEquals(3, first.value().orElseThrow());
        assertEquals(2, first.skippedCount());
        assertEquals(4, second.value().orElseThrow());
        assertEquals(0, second.skippedCount());
        assertEquals(5, third.value().orElseThrow());
        assertEquals(0, third.skippedCount());
        assertFalse(fourth.hasValue());
        assertEquals(0, fourth.skippedCount());
    }

    @Test
    void readerThatFallsBehindAgainResumesAtLatestAvailableValue() {
        RingBuffer<Integer> buffer = new RingBuffer<>(1);
        Reader<Integer> reader = buffer.createReader();

        buffer.write(1);
        buffer.write(2);
        assertEquals(2, reader.read().value().orElseThrow());

        buffer.write(3);
        buffer.write(4);

        ReadResult<Integer> result = reader.read();

        assertTrue(result.hasValue());
        assertEquals(4, result.value().orElseThrow());
        assertEquals(1, result.skippedCount());
    }

    @Test
    void readerCreatedAfterOverwriteStartsAtOldestCurrentlyAvailableValue() {
        RingBuffer<Integer> buffer = new RingBuffer<>(3);

        buffer.write(1);
        buffer.write(2);
        buffer.write(3);
        buffer.write(4);
        buffer.write(5);

        Reader<Integer> reader = buffer.createReader();

        assertEquals(3, reader.read().value().orElseThrow());
        assertEquals(4, reader.read().value().orElseThrow());
        assertEquals(5, reader.read().value().orElseThrow());
        assertFalse(reader.read().hasValue());
    }

    @Test
    void readerCreatedAfterWritesButBeforeCapacityIsExceededStartsAtFirstWrite() {
        RingBuffer<Integer> buffer = new RingBuffer<>(5);

        buffer.write(7);
        buffer.write(8);

        Reader<Integer> reader = buffer.createReader();

        assertEquals(7, reader.read().value().orElseThrow());
        assertEquals(8, reader.read().value().orElseThrow());
        assertFalse(reader.read().hasValue());
    }

    @Test
    void capacityOneKeepsOnlyLatestValueForLaggingReaders() {
        RingBuffer<String> buffer = new RingBuffer<>(1);
        Reader<String> reader = buffer.createReader();

        buffer.write("first");
        buffer.write("second");
        buffer.write("third");

        ReadResult<String> result = reader.read();

        assertEquals("third", result.value().orElseThrow());
        assertEquals(2, result.skippedCount());
        assertFalse(reader.read().hasValue());
    }

    @Test
    void preservesStoredObjectReference() {
        RingBuffer<StringBuilder> buffer = new RingBuffer<>(2);
        Reader<StringBuilder> reader = buffer.createReader();
        StringBuilder value = new StringBuilder("mutable");

        buffer.write(value);

        ReadResult<StringBuilder> result = reader.read();

        assertTrue(result.hasValue());
        assertSame(value, result.value().orElseThrow());
    }

    @Test
    void nullWriteIsStoredButReportedLikeNoValue() {
        RingBuffer<String> buffer = new RingBuffer<>(2);
        Reader<String> reader = buffer.createReader();

        buffer.write(null);

        ReadResult<String> result = reader.read();

        assertFalse(result.hasValue());
        assertTrue(result.value().isEmpty());
        assertEquals(0, result.skippedCount());
        assertEquals(1, buffer.totalWrites());
        assertEquals(1, buffer.size());
    }
}
