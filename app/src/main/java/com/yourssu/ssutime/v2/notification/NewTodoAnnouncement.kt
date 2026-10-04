package com.yourssu.ssutime.v2.notification

import androidx.datastore.core.DataStore
import com.yourssu.data.NewTodoNotificationRecord
import com.yourssu.data.TodoData
import com.yourssu.data.TodoType
import com.yourssu.ssutime.v2.todo.TODO_DEADLINE_ZONE_ID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalTime

private val announcementTime: LocalTime = LocalTime.of(18, 0)
private val announcementMutex = Mutex()
private val announcementTypes = setOf(TodoType.ASSIGNMENT, TodoType.COMMONS, TodoType.QUIZ)

internal data class NewTodoAnnouncementBatch(
    val cutoff: Instant,
    val records: List<NewTodoNotificationRecord>,
)

internal fun TodoData.pendingNewTodoAnnouncement(now: Instant): NewTodoAnnouncementBatch? {
    val localNow = now.atZone(TODO_DEADLINE_ZONE_ID)
    val todayCutoff = localNow.toLocalDate().atTime(announcementTime).atZone(TODO_DEADLINE_ZONE_ID)
    val cutoff = (if (todayCutoff.toInstant().isAfter(now)) todayCutoff.minusDays(1) else todayCutoff)
        .toInstant()
    val lastAnnouncement = runCatching { Instant.parse(lastNewTodoAnnouncementAt) }.getOrNull()
    if (lastAnnouncement != null && !lastAnnouncement.isBefore(cutoff)) return null

    // Keep missed records until they are sent, even after a day without an alarm or refresh.
    // Records discovered at/after the cutoff belong to the following day's announcement.
    val records = pendingNewTodoNotifications.filter { record ->
        val discoveredAt = runCatching { Instant.parse(record.discoveredAt) }.getOrNull()
        record.type in announcementTypes && discoveredAt?.isBefore(cutoff) == true
    }
    return records.takeIf { it.isNotEmpty() }?.let { NewTodoAnnouncementBatch(cutoff, it) }
}

internal fun TodoData.withSentNewTodoAnnouncement(batch: NewTodoAnnouncementBatch): TodoData {
    val sentRecords = batch.records.toSet()
    return copy(
        lastNewTodoAnnouncementAt = batch.cutoff.toString(),
        pendingNewTodoNotifications = pendingNewTodoNotifications.filterNot { it in sentRecords },
    )
}

internal suspend fun dispatchNewTodoAnnouncement(
    store: DataStore<TodoData>,
    now: Instant,
    canSend: suspend () -> Boolean,
    send: (List<NewTodoNotificationRecord>) -> Unit,
) {
    // Alarm delivery and a refresh may run together. Read the latest state inside the lock.
    announcementMutex.withLock {
        val batch = store.data.first().pendingNewTodoAnnouncement(now) ?: return
        if (!canSend()) return
        send(batch.records)
        store.updateData { currentData -> currentData.withSentNewTodoAnnouncement(batch) }
    }
}
