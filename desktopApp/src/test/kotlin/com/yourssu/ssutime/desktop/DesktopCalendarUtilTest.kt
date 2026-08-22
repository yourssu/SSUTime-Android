package com.yourssu.ssutime.desktop

import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.data.todoUniqueKey
import com.yourssu.ssutime.desktop.screen.calendar.toDueTimeText
import com.yourssu.ssutime.desktop.screen.calendar.toLocalDate
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DesktopCalendarUtilTest {

    @Test
    fun `toLocalDate parses iso date correctly in Asia Seoul`() {
        val todo = TodoInfo(
            todoId = 100,
            title = "과제 1",
            due_date = "2026-08-22T14:59:00Z", // 23:59 KST
            type = TodoType.ASSIGNMENT,
            subject = SubjectInfo(id = 1, name = "컴퓨터구조", professor = "홍길동"),
        )

        val localDate = todo.toLocalDate()
        assertNotNull(localDate)
        assertEquals(LocalDate.of(2026, 8, 22), localDate)
        assertEquals("23시 59분까지", todo.toDueTimeText())
    }

    @Test
    fun `todoUniqueKey produces stable unique key`() {
        val todo = TodoInfo(
            todoId = 42,
            title = "퀴즈 1",
            due_date = "2026-08-22T12:00:00Z",
            type = TodoType.QUIZ,
            subject = SubjectInfo(id = 10, name = "알고리즘", professor = "이순신"),
        )

        assertEquals("10:42:QUIZ", todo.todoUniqueKey())
    }
}
