package com.yourssu.ssutime.desktop

import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.ssutime.desktop.core.model.effectiveSubmittedInstant
import com.yourssu.ssutime.desktop.core.model.submittedTodoComparator
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DesktopSubmittedSortTest {

    private val subject = SubjectInfo(
        id = 1,
        name = "운영체제",
        professor = "교수님",
    )

    @Test
    fun testEffectiveSubmittedInstantPrioritizesSubmittedAtOverDueDate() {
        val todoWithBoth = TodoInfo(
            todoId = 1,
            title = "과제",
            due_date = "2026-04-20T23:59:59Z",
            type = TodoType.SUBMITTED,
            subject = subject,
            submittedAt = "2026-04-10T14:30:00Z",
        )
        val todoWithDueOnly = TodoInfo(
            todoId = 2,
            title = "퀴즈",
            due_date = "2026-04-15T12:00:00Z",
            type = TodoType.SUBMITTED,
            subject = subject,
            submittedAt = "",
        )
        val todoWithNone = TodoInfo(
            todoId = 3,
            title = "기타",
            due_date = "",
            type = TodoType.SUBMITTED,
            subject = subject,
            submittedAt = "",
        )

        assertEquals(
            Instant.parse("2026-04-10T14:30:00Z"),
            todoWithBoth.effectiveSubmittedInstant(),
        )
        assertEquals(
            Instant.parse("2026-04-15T12:00:00Z"),
            todoWithDueOnly.effectiveSubmittedInstant(),
        )
        assertNull(todoWithNone.effectiveSubmittedInstant())
    }

    @Test
    fun testSubmittedTodoComparatorSortsDescendingBySubmissionTime() {
        val olderSubmission = TodoInfo(
            todoId = 1,
            title = "1주차 과제",
            due_date = "2026-04-05T23:59:59Z",
            type = TodoType.SUBMITTED,
            subject = subject,
            submittedAt = "2026-04-03T10:00:00Z",
        )
        val newerSubmission = TodoInfo(
            todoId = 2,
            title = "2주차 과제",
            due_date = "2026-04-12T23:59:59Z",
            type = TodoType.SUBMITTED,
            subject = subject,
            submittedAt = "2026-04-11T15:00:00Z",
        )
        val latestCyberSubmission = TodoInfo(
            todoId = 3,
            title = "3주차 강의",
            due_date = "2026-04-19T23:59:59Z",
            type = TodoType.SUBMITTED,
            subject = subject,
            submittedAt = "", // falls back to due_date: 2026-04-19
        )
        val undatedSubmission = TodoInfo(
            todoId = 4,
            title = "미정 과제",
            due_date = "",
            type = TodoType.SUBMITTED,
            subject = subject,
            submittedAt = "",
        )

        val list = listOf(olderSubmission, undatedSubmission, latestCyberSubmission, newerSubmission)
        val sorted = list.sortedWith(submittedTodoComparator())

        // Latest date first: 2026-04-19 (latestCyberSubmission), then 2026-04-11 (newerSubmission),
        // then 2026-04-03 (olderSubmission), then undated
        assertEquals(listOf(3, 2, 1, 4), sorted.map { it.todoId })
    }

    @Test
    fun testSubmittedTodoComparatorTieBreaksDeterministic() {
        val itemA = TodoInfo(
            todoId = 10,
            title = "A 과제",
            due_date = "2026-04-10T00:00:00Z",
            type = TodoType.SUBMITTED,
            subject = subject,
            submittedAt = "2026-04-05T12:00:00Z",
        )
        val itemB = TodoInfo(
            todoId = 20,
            title = "B 과제",
            due_date = "2026-04-10T00:00:00Z",
            type = TodoType.SUBMITTED,
            subject = subject,
            submittedAt = "2026-04-05T12:00:00Z",
        )

        val sorted = listOf(itemB, itemA).sortedWith(submittedTodoComparator())
        assertEquals(listOf(10, 20), sorted.map { it.todoId })
    }
}
