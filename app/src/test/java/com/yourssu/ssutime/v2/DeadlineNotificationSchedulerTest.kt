package com.yourssu.ssutime.v2

import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.ssutime.v2.notification.deadlineReminderKey
import com.yourssu.ssutime.v2.notification.nextDeadlineReminderTriggerTime
import com.yourssu.ssutime.v2.notification.toStableNotificationId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class DeadlineNotificationSchedulerTest {

    private val seoulZone: ZoneId = ZoneId.of("Asia/Seoul")

    @Test
    fun nextDeadlineReminderTriggerTime_returnsTodayWhenTriggerTimeIsLater() {
        val now = ZonedDateTime.of(2026, 5, 26, 8, 30, 0, 0, seoulZone)

        val trigger = nextDeadlineReminderTriggerTime(now, LocalTime.of(9, 0))

        assertEquals(ZonedDateTime.of(2026, 5, 26, 9, 0, 0, 0, seoulZone), trigger)
    }

    @Test
    fun nextDeadlineReminderTriggerTime_returnsTomorrowWhenTriggerTimeAlreadyPassed() {
        val now = ZonedDateTime.of(2026, 5, 26, 18, 0, 0, 0, seoulZone)

        val trigger = nextDeadlineReminderTriggerTime(now, LocalTime.of(18, 0))

        assertEquals(ZonedDateTime.of(2026, 5, 27, 18, 0, 0, 0, seoulZone), trigger)
    }

    @Test
    fun deadlineReminderKey_distinguishesTodosWithSameIdAndDueDate() {
        val firstTodo = todoInfo(title = "중간 리포트")
        val secondTodo = todoInfo(title = "기말 리포트")

        assertNotEquals(
            firstTodo.deadlineReminderKey(daysBefore = 0),
            secondTodo.deadlineReminderKey(daysBefore = 0),
        )
    }

    @Test
    fun deadlineReminderKey_normalizesWhitespaceInTextFields() {
        val firstTodo = todoInfo(title = "중간   리포트")
        val secondTodo = todoInfo(title = " 중간 리포트 ")

        assertEquals(
            firstTodo.deadlineReminderKey(daysBefore = 0),
            secondTodo.deadlineReminderKey(daysBefore = 0),
        )
    }

    @Test
    fun stableNotificationId_avoidsKnownStringHashCollision() {
        assertEquals("FB".hashCode(), "Ea".hashCode())

        assertNotEquals(
            "FB".toStableNotificationId(),
            "Ea".toStableNotificationId(),
        )
    }

    private fun todoInfo(title: String): TodoInfo =
        TodoInfo(
            todoId = 1,
            title = title,
            due_date = "2026-05-31T09:00:00+09:00",
            type = TodoType.ASSIGNMENT,
            subject = SubjectInfo(
                id = 10,
                name = "운영체제",
                professor = "김교수",
            ),
        )
}
