package com.yourssu.ssutime.v2.screen.main

import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoData
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.data.todoUniqueKey
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenTodoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun todoUniqueKey_generatesExpectedKeys() {
        val subject = SubjectInfo(101, "컴퓨터개론", "교수님")
        val todoWithId = TodoInfo(
            todoId = 12345,
            title = "과제1",
            due_date = "2026-05-29T18:00:00Z",
            type = TodoType.ASSIGNMENT,
            subject = subject,
        )
        val todoWithoutId = TodoInfo(
            todoId = -1,
            title = "강의영상",
            due_date = "2026-05-30T18:00:00Z",
            type = TodoType.COMMONS,
            subject = subject,
        )

        assertEquals("101:12345:ASSIGNMENT", todoWithId.todoUniqueKey())
        assertEquals("101:-1:COMMONS:강의영상:2026-05-30T18:00:00Z", todoWithoutId.todoUniqueKey())
    }

    @Test
    fun todoData_serialization_roundTripWithHiddenFields() {
        val subject = SubjectInfo(101, "컴퓨터개론", "교수님")
        val visibleTodo = TodoInfo(
            todoId = 1,
            title = "과제1",
            due_date = "2026-05-29T18:00:00Z",
            type = TodoType.ASSIGNMENT,
            subject = subject,
        )
        val hiddenTodo = TodoInfo(
            todoId = 2,
            title = "과제2",
            due_date = "2026-05-30T18:00:00Z",
            type = TodoType.QUIZ,
            subject = subject,
        )

        val originalData = TodoData(
            todos = listOf(visibleTodo),
            hiddenTodos = listOf(hiddenTodo),
            hiddenTodoKeys = listOf(hiddenTodo.todoUniqueKey()),
        )

        val encoded = json.encodeToString(TodoData.serializer(), originalData)
        val decoded = json.decodeFromString(TodoData.serializer(), encoded)

        assertEquals(1, decoded.todos.size)
        assertEquals("과제1", decoded.todos[0].title)
        assertEquals(1, decoded.hiddenTodos.size)
        assertEquals("과제2", decoded.hiddenTodos[0].title)
        assertEquals(listOf("101:2:QUIZ"), decoded.hiddenTodoKeys)
    }

    @Test
    fun todoData_backwardCompatibility_deserializationWithoutHiddenFields() {
        val legacyJson = """
            {
                "todos": [],
                "submitted": [],
                "subjects": [],
                "loadedAt": "2026-08-22T00:00:00Z"
            }
        """.trimIndent()

        val decoded = json.decodeFromString(TodoData.serializer(), legacyJson)

        assertTrue(decoded.hiddenTodos.isEmpty())
        assertTrue(decoded.hiddenTodoKeys.isEmpty())
    }

    @Test
    fun hiddenTodos_filtering_partitionLogic() {
        val subject = SubjectInfo(101, "컴퓨터개론", "교수님")
        val todo1 = TodoInfo(1, "과제1", "2026-05-29T18:00:00Z", TodoType.ASSIGNMENT, subject)
        val todo2 = TodoInfo(2, "퀴즈1", "2026-05-30T18:00:00Z", TodoType.QUIZ, subject)
        val allTodos = listOf(todo1, todo2)

        val hiddenKeys = setOf(todo2.todoUniqueKey())
        val (hidden, visible) = allTodos.partition { it.todoUniqueKey() in hiddenKeys }

        assertEquals(listOf(todo1), visible)
        assertEquals(listOf(todo2), hidden)
    }
}
