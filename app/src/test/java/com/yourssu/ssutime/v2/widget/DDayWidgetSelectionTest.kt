package com.yourssu.ssutime.v2.widget

import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class DDayWidgetSelectionTest {
    private val due = Instant.parse("2026-10-08T09:00:00Z")
    private val late = due.plusSeconds(3600)
    private val todo = TodoInfo(
        todoId = 1,
        title = "과제",
        due_date = due.toString(),
        lateAt = late.toString(),
        type = TodoType.ASSIGNMENT,
        subject = null,
    )

    @Test
    fun keepsLateSubmissionUntilExtendedDeadline() {
        val todos = listOf(todo)
        assertEquals(todo, todos.selectMostUrgentTodo(due.minusSeconds(1)))
        assertEquals(todo, todos.selectMostUrgentTodo(due))
        assertEquals(todo, todos.selectMostUrgentTodo(late.minusSeconds(1)))
        assertNull(todos.selectMostUrgentTodo(late))
    }

    @Test
    fun selectsLateSubmissionAheadOfLaterNormalDeadline() {
        val upcoming = todo.copy(todoId = 2, due_date = late.plusSeconds(3600).toString())
        assertEquals(listOf(todo, upcoming), listOf(upcoming, todo).availableWidgetTodos(due))
        assertEquals(listOf(upcoming), listOf(upcoming, todo).availableWidgetTodos(late))
        assertEquals(todo, listOf(upcoming, todo).selectMostUrgentTodo(due))
        assertEquals(upcoming, listOf(upcoming, todo).selectMostUrgentTodo(late))
    }

    @Test
    fun excludesExpiredInvalidAndCompletedLateSubmissions() {
        for (item in listOf(
            todo.copy(lateAt = ""),
            todo.copy(lateAt = "invalid"),
            todo.copy(lateAt = due.toString()),
            todo.copy(type = TodoType.SUBMITTED),
            todo.copy(type = TodoType.SUBMITTED_LATE),
            todo.copy(submittedAt = due.toString()),
        )) {
            assertNull(listOf(item).selectMostUrgentTodo(due))
        }
    }

    @Test
    fun usesSeoulCalendarDateDifferenceForSummaryCountdown() {
        val now = Instant.parse("2026-11-19T13:00:00Z") // 2026-11-19 22:00:00 KST

        val todayDue = Instant.parse("2026-11-19T14:59:59Z") // 2026-11-19 23:59:59 KST
        val todayCountdown = todo.copy(due_date = todayDue.toString(), lateAt = "")
            .toTodoSummaryCountdownOrNull(now)!!
        assertEquals("D-0", todayCountdown.dDayText)
        assertEquals("1:59:59", todayCountdown.countdownText)
        assertEquals(todayDue.toEpochMilli(), todayCountdown.countdownTargetEpochMillis)

        // Less than 24h remaining, but due on the next Seoul calendar day -> D-1 (static, not live countdown)
        val tomorrowDue = Instant.parse("2026-11-20T09:00:00Z") // 2026-11-20 18:00:00 KST (20h away)
        val tomorrowCountdown = todo.copy(due_date = tomorrowDue.toString(), lateAt = "")
            .toTodoSummaryCountdownOrNull(now)!!
        assertEquals("D-1", tomorrowCountdown.dDayText)
        assertEquals("D-1", tomorrowCountdown.countdownText)
        assertNull(tomorrowCountdown.countdownTargetEpochMillis)

        // Four calendar days later at an earlier time of day -> D-4 (not truncated to D-3)
        val fourDaysLaterDue = Instant.parse("2026-11-23T03:00:00Z") // 2026-11-23 12:00:00 KST
        val fourDaysLaterCountdown = todo.copy(due_date = fourDaysLaterDue.toString(), lateAt = "")
            .toTodoSummaryCountdownOrNull(now)!!
        assertEquals("D-4", fourDaysLaterCountdown.dDayText)
        assertEquals("D-4", fourDaysLaterCountdown.countdownText)
        assertNull(fourDaysLaterCountdown.countdownTargetEpochMillis)
    }
}
