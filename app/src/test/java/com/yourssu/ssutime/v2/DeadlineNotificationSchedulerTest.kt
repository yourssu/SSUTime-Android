package com.yourssu.ssutime.v2

import com.yourssu.ssutime.v2.notification.nextDeadlineReminderTriggerTime
import org.junit.Assert.assertEquals
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
}
