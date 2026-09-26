package com.yourssu.ssutime.v2

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class DeadlineTimeParsingTest {

    @Test
    fun getRemainingDays_keepsUtcOffsetForTargetTimeEndingWithZ() {
        val now = Instant.parse("2026-05-17T00:00:00Z")
        val targetTime = "2026-05-19T02:00:00Z"

        assertEquals(2, getRemainingDays(targetTime, now))
    }

    @Test
    fun getRemainingDays_treatsTargetTimeWithoutOffsetAsSeoulTime() {
        val now = Instant.parse("2026-05-17T00:00:00Z")
        val targetTime = "2026-05-19T02:00:00"

        assertEquals(1, getRemainingDays(targetTime, now))
    }

    @Test
    fun getRemainingTimeText_hoursFormatWhenOneHourOrMore() {
        val now = Instant.parse("2026-05-17T00:00:00Z")

        // 23 hours 45 mins
        val target23h = "2026-05-17T23:45:00Z"
        assertEquals("23h", getRemainingTimeText(target23h, now))

        // 2 hours exactly
        val target2h = "2026-05-17T02:00:00Z"
        assertEquals("2h", getRemainingTimeText(target2h, now))

        // 1 hour exactly (3600 seconds)
        val target1h = "2026-05-17T01:00:00Z"
        assertEquals("1h", getRemainingTimeText(target1h, now))
    }

    @Test
    fun getRemainingTimeText_mmSsFormatWhenLessThanOneHour() {
        val now = Instant.parse("2026-05-17T00:00:00Z")

        // 59 mins 59 secs (3599 seconds)
        val target59m59s = "2026-05-17T00:59:59Z"
        assertEquals("59:59", getRemainingTimeText(target59m59s, now))

        // 2 mins 30 secs
        val target2m30s = "2026-05-17T00:02:30Z"
        assertEquals("02:30", getRemainingTimeText(target2m30s, now))

        // 1 min exactly (60 seconds)
        val target1m = "2026-05-17T00:01:00Z"
        assertEquals("01:00", getRemainingTimeText(target1m, now))
    }

    @Test
    fun getRemainingTimeText_secondsFormatWhenLessThanOneMinute() {
        val now = Instant.parse("2026-05-17T00:00:00Z")

        // 59 seconds
        val target59s = "2026-05-17T00:00:59Z"
        val expected59 = if (java.util.Locale.getDefault().language == java.util.Locale.KOREAN.language) "59초" else "59s"
        assertEquals(expected59, getRemainingTimeText(target59s, now))

        // 5 seconds
        val target5s = "2026-05-17T00:00:05Z"
        val expected5 = if (java.util.Locale.getDefault().language == java.util.Locale.KOREAN.language) "5초" else "5s"
        assertEquals(expected5, getRemainingTimeText(target5s, now))

        // 0 seconds (passed)
        val target0s = "2026-05-17T00:00:00Z"
        val expected0 = if (java.util.Locale.getDefault().language == java.util.Locale.KOREAN.language) "0초" else "0s"
        assertEquals(expected0, getRemainingTimeText(target0s, now))
    }
}
