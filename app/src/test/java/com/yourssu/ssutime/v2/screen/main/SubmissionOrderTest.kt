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
import java.time.Instant

class SubmissionOrderTest {
    private val now = Instant.parse("2026-10-04T10:00:00Z")

    @Test
    fun mixesSubjectsAndSourcesBySubmissionTime() {
        val oldLms = todo(1, 1).copy(submittedAt = "2026-10-04T17:00:00+09:00")
        val cyber = todo(-10001, -1).copy(completionObservedAt = "2026-10-04T09:00:00Z")
        val recentLms = todo(2, 2).copy(submittedAt = "2026-10-04T19:00:00")
        val unknown = todo(-10002, -2)
        val malformed = todo(3, 3).copy(submittedAt = "invalid")

        assertEquals(
            listOf(recentLms, cyber, oldLms, unknown, malformed),
            listOf(oldLms, unknown, cyber, malformed, recentLms).sortedBySubmittedAtDescending(),
        )
    }

    @Test
    fun recordsVisibleAndHiddenCompletionTransitionsAndPreservesAcrossRefreshAndRestart() {
        val visible = todo(-10001, -1)
        val hidden = todo(-10002, -1)
        val previous = TodoData(
            todos = listOf(visible.copy(type = TodoType.COMMONS)),
            hiddenTodos = listOf(hidden.copy(type = TodoType.QUIZ)),
        )
        val fresh = TodoData(submitted = listOf(visible, hidden))
        val completed = fresh.withSubmissionOrder(previous, now)
        assertEquals(listOf(now.toString(), now.toString()), completed.submitted.map { it.completionObservedAt })

        val restored = Json.decodeFromString<TodoData>(Json.encodeToString(completed))
        val refreshed = fresh.withSubmissionOrder(restored, now.plusSeconds(3600))
        assertEquals(completed.submitted, refreshed.submitted)
    }

    @Test
    fun doesNotInventHistoricalSubmissionTimeOrMixSameIdsAcrossSubjects() {
        val historical = todo(-10001, -1)
        val previous = TodoData(todos = listOf(historical.copy(subject = subject(-2))))
        val completed = TodoData(submitted = listOf(historical)).withSubmissionOrder(previous, now)
        assertEquals("", completed.submitted.single().completionObservedAt)
    }

    @Test
    fun serverTimeTakesPrecedenceOverObservationTime() {
        val observed = todo(-10001, -1).copy(completionObservedAt = now.toString())
        val serverDated = todo(1, 1).copy(
            submittedAt = "2026-10-04T08:00:00Z",
            completionObservedAt = now.plusSeconds(3600).toString(),
        )
        assertEquals(listOf(observed, serverDated), listOf(serverDated, observed).sortedBySubmittedAtDescending())
    }

    @Test
    fun legacyCacheWithoutObservationFieldStillLoads() {
        val legacy = """{"todoId":1,"title":"과제","due_date":"","type":"SUBMITTED","subject":null}"""
        assertEquals("", Json.decodeFromString<TodoInfo>(legacy).completionObservedAt)
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
