package com.yourssu.ssutime.desktop

import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.ssutime.desktop.core.model.AppTodoData
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopDeadlineNotifierTest {
    private val subject = SubjectInfo(
        id = 1,
        name = "운영체제",
        professor = "교수",
    )

    @Test
    fun `windows toast uses foreground activation without a custom protocol`() {
        assertTrue(windowsToastXmlTemplate.contains("launch=\"--notification-activated\""))
        assertFalse(windowsToastXmlTemplate.contains("activationType=\"protocol\""))
        assertFalse(windowsToastXmlTemplate.contains("ssutime://"))
    }

    @Test
    fun `automatic notifications enforce the schedule and sent reminder keys`() {
        val data = AppTodoData(
            todos = listOf(
                todo(1, "내일 과제", "2026-08-09T14:59:00Z"),
                todo(2, "나흘 뒤 과제", "2026-08-12T14:59:00Z"),
            ),
        )
        val beforeSchedule = buildAutomaticDeadlineNotifications(
            todoData = data,
            sentKeys = emptyList(),
            now = Instant.parse("2026-08-08T08:59:59Z"),
        )
        assertTrue(beforeSchedule.isEmpty())

        val scheduled = buildAutomaticDeadlineNotifications(
            todoData = data,
            sentKeys = emptyList(),
            now = Instant.parse("2026-08-08T09:05:00Z"),
        )
        assertEquals(1, scheduled.size)
        assertEquals("운영체제 과제 마감 D-1이에요!", scheduled.single().body)

        val alreadySent = buildAutomaticDeadlineNotifications(
            todoData = data,
            sentKeys = scheduled.single().reminderKeys,
            now = Instant.parse("2026-08-08T09:05:00Z"),
        )
        assertTrue(alreadySent.isEmpty())
    }

    private fun todo(
        id: Int,
        title: String,
        dueDate: String,
        type: TodoType = TodoType.ASSIGNMENT,
    ): TodoInfo = TodoInfo(
        todoId = id,
        title = title,
        due_date = dueDate,
        type = type,
        subject = subject,
    )
}
