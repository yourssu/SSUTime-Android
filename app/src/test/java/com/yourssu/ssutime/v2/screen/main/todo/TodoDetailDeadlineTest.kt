package com.yourssu.ssutime.v2.screen.main.todo

import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TodoDetailDeadlineTest {
    private val due = Instant.parse("2026-10-04T09:00:00Z")
    private val late = Instant.parse("2026-10-05T09:00:00Z")
    private val todo = TodoInfo(
        todoId = 1,
        title = "과제",
        due_date = due.toString(),
        lateAt = late.toString(),
        type = TodoType.ASSIGNMENT,
        subject = null,
    )

    @Test
    fun switchesFromNormalToLateDeadlineDuringLateSubmissionWindow() {
        assertEquals(due.toString(), todo.detailDeadline(due.minusSeconds(1)))
        assertFalse(todo.isLateSubmissionAvailable(due.minusSeconds(1)))
        assertEquals(late.toString(), todo.detailDeadline(due))
        assertTrue(todo.isLateSubmissionAvailable(due))
        assertEquals(late.toString(), todo.detailDeadline(late.minusSeconds(1)))
        assertFalse(todo.isLateSubmissionAvailable(late))
        assertEquals(due.toString(), todo.detailDeadline(late))
    }

    @Test
    fun doesNotOfferLateSubmissionForMissingInvalidOrNonExtendedDeadline() {
        for (lateAt in listOf("", "invalid", due.toString(), due.minusSeconds(1).toString())) {
            val item = todo.copy(lateAt = lateAt)
            assertFalse(item.isLateSubmissionAvailable(due))
            assertEquals(due.toString(), item.detailDeadline(due))
        }
    }

    @Test
    fun completedItemsKeepNormalDeadline() {
        for (type in listOf(TodoType.SUBMITTED, TodoType.SUBMITTED_LATE)) {
            assertFalse(todo.copy(type = type).isLateSubmissionAvailable(due))
            assertEquals(due.toString(), todo.copy(type = type).detailDeadline(due))
        }
        assertFalse(todo.copy(submittedAt = due.toString()).isLateSubmissionAvailable(due))
    }

    @Test
    fun videoAndQuizAlsoUseLateDeadline() {
        for (type in listOf(TodoType.COMMONS, TodoType.QUIZ)) {
            assertEquals(late.toString(), todo.copy(type = type).detailDeadline(due))
        }
    }
}
