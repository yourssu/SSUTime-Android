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
}
