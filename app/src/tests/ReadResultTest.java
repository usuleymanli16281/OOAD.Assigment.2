package ringbuffer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadResultTest {

    @Test
    void withValueExposesValueAndSkippedCount() {
        ReadResult<String> result = ReadResult.withValue("item", 3);

        assertTrue(result.hasValue());
        assertEquals("item", result.value().orElseThrow());
        assertEquals(3, result.skippedCount());
    }

    @Test
    void emptyExposesNoValueAndSkippedCount() {
        ReadResult<String> result = ReadResult.empty(2);

        assertFalse(result.hasValue());
        assertTrue(result.value().isEmpty());
        assertEquals(2, result.skippedCount());
    }

    @Test
    void withValueTreatsNullAsNoValue() {
        ReadResult<String> result = ReadResult.withValue(null, 0);

        assertFalse(result.hasValue());
        assertTrue(result.value().isEmpty());
        assertEquals(0, result.skippedCount());
    }
}
