# Test Documentation

## Scope

These tests validate the existing code without modifying `app/src/main`. Existing repository-owner tests were intentionally ignored when deciding coverage. The added tests are in `app/src/tests/RingBufferBehaviorTest.java` and `app/src/tests/ReadResultTest.java`.

## Covered Behavior

- Constructor validation for zero and negative capacity.
- Large capacity construction.
- Capacity, size, total write count, and reader count reporting.
- Empty reads before any writes and after a reader catches up.
- Read ordering for a reader created before writes.
- Full-buffer draining in write order.
- Interleaved write/read behavior over repeated operations.
- Independent read positions for multiple readers.
- Multiple readers consuming the same stream when capacity is not exceeded.
- Overwrite behavior when a slow reader falls behind.
- Skip counts after overwritten entries, including when a reader catches up and later falls behind again.
- Reader creation after overwrites, starting at the oldest available value.
- Reader creation after writes but before capacity is exceeded.
- Capacity-one overwrite behavior.
- Preservation of stored object references.
- Current handling of `null` writes.
- Direct `ReadResult` value and empty-result behavior.

## Notable Edge Cases

`ReadResult` uses `null` internally to represent an empty read. Because `RingBuffer.write` accepts `null`, a stored `null` is read back as `hasValue() == false` and `Optional.empty()`. The write is still counted in `totalWrites()` and `size()`. If `null` should be a valid payload that readers can distinguish from no data, the production API would need to change. This was documented instead of changed.

The implementation documents a single-writer model. The public methods are synchronized, but the tests do not try to prove broad concurrent correctness because deterministic multi-threaded validation would require either a dedicated concurrency contract or source-level test hooks.

The added tests are placed in `app/src/tests` to keep them separate from the repository-owner tests. `app/build.gradle` includes this folder in the test source set so the normal Gradle test task runs both the repository-owner tests and these added tests.

## How To Run

The repository's default test command is:

```bash
./gradlew test
```

On Windows:

```powershell
.\gradlew.bat test
```

If Windows shows a `JAVA_HOME is set to an invalid directory` error, set `JAVA_HOME` to the installed JDK path and run the tests again:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21.0.10"
.\gradlew.bat test
```
