package com.yourssu.ssutime.v2.screen.main

import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoData
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.ssutime.v2.todo.sortedBySubmittedAtDescending
import com.yourssu.ssutime.v2.todo.withSubmissionOrder
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmissionOrderTest {
    @Test
    fun mixesSubjectsAndSourcesBySubmissionTimeOrOriginalDeadline() {
        val oldLms = todo(1, 1).copy(submittedAt = "2026-10-04T17:00:00+09:00")
        val cyber = todo(-10001, -1).copy(due_date = "2026-10-04T09:00:00Z")
        val recentLms = todo(2, 2).copy(submittedAt = "2026-10-04T19:00:00")
        val unknown = todo(-10002, -2)
        val malformed = todo(3, 3).copy(submittedAt = "invalid")

        assertEquals(
            listOf(recentLms, cyber, oldLms, unknown, malformed),
            listOf(oldLms, unknown, cyber, malformed, recentLms).sortedBySubmittedAtDescending(),
        )
    }

    @Test
    fun submissionTimeTakesPrecedenceOverOriginalAndLateDeadlines() {
        val dated = todo(1, 1).copy(
            submittedAt = "2026-10-04T08:00:00Z",
            due_date = "2026-10-06T10:00:00Z",
            lateAt = "2026-10-07T10:00:00Z",
        )
        val fallback = todo(-10001, -1).copy(
            due_date = "2026-10-04T09:00:00Z",
            lateAt = "2026-10-08T10:00:00Z",
            completionObservedAt = "2026-10-09T10:00:00Z",
        )
        val laterSubmission = todo(2, 2).copy(submittedAt = "2026-10-04T10:00:00Z")
        assertEquals(
            listOf(laterSubmission, fallback, dated),
            listOf(dated, fallback, laterSubmission).sortedBySubmittedAtDescending(),
        )
    }

    @Test
    fun invalidSubmissionTimeFallsBackToDeadlineAndUnknownDatesStayAtBottom() {
        val invalidSubmission = todo(1, 1).copy(
            submittedAt = "invalid",
            due_date = "2026-10-04T12:00:00+09:00",
        )
        val noSubmission = todo(2, 2).copy(due_date = "2026-10-04T02:00:00Z")
        val unknown = todo(3, 3).copy(due_date = "invalid")
        assertEquals(
            listOf(invalidSubmission, noSubmission, unknown),
            listOf(unknown, noSubmission, invalidSubmission).sortedBySubmittedAtDescending(),
        )
    }

    @Test
    fun refreshAndRestartKeepTheSameOrderWithoutChangingOriginalDates() {
        val earlier = todo(1, 1).copy(due_date = "2026-10-03T09:00:00Z")
        val later = todo(-10001, -1).copy(due_date = "2026-10-04T09:00:00Z")
        val data = TodoData(submitted = listOf(earlier, later)).withSubmissionOrder()
        val restored = Json.decodeFromString<TodoData>(Json.encodeToString(data))
        assertEquals(listOf(later, earlier), restored.withSubmissionOrder().submitted)
        assertEquals(data, restored.withSubmissionOrder())
    }

    private fun subject(id: Int) = SubjectInfo(id = id, name = "과목 $id", professor = "")

    private fun todo(id: Int, subjectId: Int) = TodoInfo(
        todoId = id,
        title = "제출 $id",
        due_date = "",
        type = TodoType.SUBMITTED,
        subject = subject(subjectId),
    )
}
