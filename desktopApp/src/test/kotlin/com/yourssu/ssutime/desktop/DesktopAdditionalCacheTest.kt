package com.yourssu.ssutime.desktop

import com.yourssu.data.*
import com.yourssu.ssutime.desktop.core.cyber.*
import com.yourssu.ssutime.desktop.core.lms.toAppTodoData
import java.time.Instant
import kotlin.test.*

class DesktopAdditionalCacheTest {
    private val subject = SubjectInfo(-1001, "Cyber", "Teacher")
    private val pending = TodoInfo(todoId = -2001, title = "Lecture", due_date = "2027-01-01T00:00:00Z", type = TodoType.COMMONS, subject = subject)
    private val previous = TodoData(todos = listOf(pending), subjects = listOf(subject))
    private val now = Instant.parse("2026-10-01T00:00:00Z")
    @Test fun failedCyberFetchKeepsExistingSubjectsAndPendingItems() {
        val next = toAppTodoData(emptyList(), now.toString(), previous, isCyberConnected = true, now = now)
        assertEquals(listOf(subject), next.subjects)
        assertEquals(listOf(pending), next.todos)
    }
    @Test fun successfulEmptyCyberResponseRemovesDeletedItems() {
        val next = toAppTodoData(emptyList(), now.toString(), previous, CyberTodoResult(isComplete = true), true, now)
        assertTrue(next.todos.isEmpty())
        assertTrue(next.subjects.isEmpty())
    }
    @Test fun disconnectedCyberNeverRestoresOldCachedItems() {
        val next = toAppTodoData(emptyList(), now.toString(), previous, isCyberConnected = false, now = now)
        assertTrue(next.todos.isEmpty())
        assertTrue(next.subjects.isEmpty())
    }
    @Test fun hiddenCyberItemsStayHiddenAcrossFailedFetch() {
        val state = previous.copy(todos = emptyList(), hiddenTodos = listOf(pending), hiddenTodoKeys = listOf(pending.todoUniqueKey()))
        val next = toAppTodoData(emptyList(), now.toString(), state, isCyberConnected = true, now = now)
        assertTrue(next.todos.isEmpty())
        assertEquals(listOf(pending), next.hiddenTodos)
    }
    @Test fun newTodoAnnouncementEnforcesEveningWindowAndConsumesExpiredRecords() {
        val record = NewTodoNotificationRecord("new", "2026-10-01T08:00:00Z", "Course", TodoType.QUIZ)
        val expired = record.copy(todoKey = "expired", discoveredAt = "2026-09-28T00:00:00Z")
        val state = TodoData(pendingNewTodoNotifications = listOf(record, expired))
        assertNull(buildNewTodoAnnouncement(state, Instant.parse("2026-10-01T08:59:59Z")))
        val announcement = buildNewTodoAnnouncement(state, Instant.parse("2026-10-01T09:00:00Z"))!!
        assertEquals(listOf("new", "expired"), announcement.consumedKeys)
        assertNotNull(announcement.notification)
        assertNull(buildNewTodoAnnouncement(state, Instant.parse("2026-10-01T09:16:00Z")))
    }
}
