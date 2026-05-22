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
}
