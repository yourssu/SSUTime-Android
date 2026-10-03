package com.yourssu.ssutime.v2

import androidx.datastore.core.DataStore
import com.yourssu.data.NewTodoNotificationRecord
import com.yourssu.data.TodoData
import com.yourssu.data.TodoType
import com.yourssu.ssutime.v2.notification.dispatchNewTodoAnnouncement
import com.yourssu.ssutime.v2.notification.pendingNewTodoAnnouncement
import com.yourssu.ssutime.v2.notification.withSentNewTodoAnnouncement
import com.yourssu.ssutime.v2.screen.main.TodoDataSerializer
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.OffsetDateTime

class NewTodoAnnouncementTest {
    @Test
    fun waitsUntil18ThenIncludesOnlyRecordsDiscoveredBefore18() {
        val earlier = record("earlier", "2026-10-03T17:00:00+09:00")
        val at18 = record("at18", "2026-10-03T18:00:00+09:00")
        val after18 = record("after18", "2026-10-03T18:01:00+09:00")
        val data = TodoData(pendingNewTodoNotifications = listOf(earlier, at18, after18))

        assertNull(data.pendingNewTodoAnnouncement(time("2026-10-03T17:59:59+09:00")))
        val batch = requireNotNull(data.pendingNewTodoAnnouncement(time("2026-10-03T18:00:00+09:00")))
        assertEquals(time("2026-10-03T18:00:00+09:00"), batch.cutoff)
        assertEquals(listOf(earlier), batch.records)
        assertEquals(listOf(at18, after18), data.withSentNewTodoAnnouncement(batch).pendingNewTodoNotifications)
    }

    @Test
    fun refreshAt1805RecoversDelayedAlarmAndDoesNotSendAgainThatDay() = runBlocking {
        val earlier = record("earlier", "2026-10-03T17:00:00+09:00")
        val later = record("later", "2026-10-03T18:05:00+09:00")
        val store = MemoryStore(TodoData(pendingNewTodoNotifications = listOf(earlier, later)))
        val sent = mutableListOf<List<NewTodoNotificationRecord>>()

        dispatchNewTodoAnnouncement(store, time("2026-10-03T18:05:00+09:00"), { true }, sent::add)
        // Simulate the delayed AlarmManager receiver after the refresh rescheduled its alarm.
        dispatchNewTodoAnnouncement(store, time("2026-10-03T18:30:00+09:00"), { true }, sent::add)
        assertEquals(listOf(listOf(earlier)), sent)
        assertEquals(listOf(later), store.data.value.pendingNewTodoNotifications)

        dispatchNewTodoAnnouncement(store, time("2026-10-04T18:00:00+09:00"), { true }, sent::add)
        assertEquals(listOf(listOf(earlier), listOf(later)), sent)
    }

    @Test
    fun missedDayDoesNotExpireUnsentRecords() {
        val earlier = record("earlier", "2026-10-01T17:00:00+09:00")
        val data = TodoData(pendingNewTodoNotifications = listOf(earlier))

        val batch = requireNotNull(data.pendingNewTodoAnnouncement(time("2026-10-03T09:00:00+09:00")))
        assertEquals(listOf(earlier), batch.records)
        assertEquals(time("2026-10-02T18:00:00+09:00"), batch.cutoff)
    }

    @Test
    fun concurrentAlarmAndRefreshSendOnlyOnce() = runBlocking {
        val store = MemoryStore(TodoData(pendingNewTodoNotifications = listOf(record())))
        var sends = 0
        val alarm = async {
            dispatchNewTodoAnnouncement(store, time("2026-10-03T18:05:00+09:00"), {
                yield()
                true
            }) { sends++ }
        }
        val refresh = async {
            dispatchNewTodoAnnouncement(store, time("2026-10-03T18:05:00+09:00"), { true }) { sends++ }
        }
        alarm.await()
        refresh.await()
        assertEquals(1, sends)
    }

    @Test
    fun blockedNotificationRetainsRecordsAndCanRetry() = runBlocking {
        val earlier = record()
        val initial = TodoData(pendingNewTodoNotifications = listOf(earlier))
        val store = MemoryStore(initial)
        var sends = 0

        dispatchNewTodoAnnouncement(store, time("2026-10-03T18:05:00+09:00"), { false }) { sends++ }
        assertEquals(initial, store.data.value)
        assertEquals(0, sends)
        dispatchNewTodoAnnouncement(store, time("2026-10-03T19:00:00+09:00"), { true }) { sends++ }
        assertEquals(1, sends)
    }

    @Test
    fun sendFailureRetainsRecordsForRetry() = runBlocking {
        val initial = TodoData(pendingNewTodoNotifications = listOf(record()))
        val store = MemoryStore(initial)
        val result = runCatching {
            dispatchNewTodoAnnouncement(store, time("2026-10-03T18:05:00+09:00"), { true }) {
                error("Notification failed")
            }
        }
        assertEquals(true, result.isFailure)
        assertEquals(initial, store.data.value)
    }

    @Test
    fun savingSentBatchPreservesRecordsAddedWhileSending() {
        val earlier = record()
        val data = TodoData(pendingNewTodoNotifications = listOf(earlier))
        val batch = requireNotNull(data.pendingNewTodoAnnouncement(time("2026-10-03T18:05:00+09:00")))
        val later = record("later", "2026-10-03T18:06:00+09:00")

        val updated = data.copy(pendingNewTodoNotifications = listOf(earlier, later))
            .withSentNewTodoAnnouncement(batch)
        assertEquals(listOf(later), updated.pendingNewTodoNotifications)
    }

    @Test
    fun persistedStatePreventsDuplicateAfterRestartAndReadsOldData() = runBlocking {
        val old = TodoDataSerializer.readFrom(ByteArrayInputStream("{}".toByteArray()))
        assertEquals("", old.lastNewTodoAnnouncementAt)
        val data = old.copy(pendingNewTodoNotifications = listOf(record()))
        val batch = requireNotNull(data.pendingNewTodoAnnouncement(time("2026-10-03T18:05:00+09:00")))
        val output = ByteArrayOutputStream()
        TodoDataSerializer.writeTo(data.withSentNewTodoAnnouncement(batch), output)
        val restored = TodoDataSerializer.readFrom(ByteArrayInputStream(output.toByteArray()))

        assertEquals(batch.cutoff.toString(), restored.lastNewTodoAnnouncementAt)
        assertNull(restored.pendingNewTodoAnnouncement(time("2026-10-03T19:00:00+09:00")))
        // A record committed late by another refresh must wait for the next day's batch.
        val lateCommit = restored.copy(pendingNewTodoNotifications = listOf(record()))
        assertNull(lateCommit.pendingNewTodoAnnouncement(time("2026-10-03T19:00:00+09:00")))
        assertEquals(listOf(record()), lateCommit.pendingNewTodoAnnouncement(time("2026-10-04T18:00:00+09:00"))?.records)
    }

    private fun record(key: String = "todo", discoveredAt: String = "2026-10-03T17:00:00+09:00") =
        NewTodoNotificationRecord(key, time(discoveredAt).toString(), "운영체제", TodoType.ASSIGNMENT)

    private fun time(value: String): Instant = OffsetDateTime.parse(value).toInstant()

    private class MemoryStore(initial: TodoData) : DataStore<TodoData> {
        override val data = MutableStateFlow(initial)

        override suspend fun updateData(transform: suspend (t: TodoData) -> TodoData): TodoData =
            transform(data.value).also { data.value = it }
    }
}
