package com.yourssu.ssutime.v2.screen.main

import com.yourssu.data.AlertData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CallAlertRemindPolicyTest {

    @Test
    fun callAlertRemindPeriod_returnsFirstSemesterForMarchToJune() {
        assertEquals("2026-1", callAlertRemindPeriod(LocalDate.of(2026, 3, 1)))
        assertEquals("2026-1", callAlertRemindPeriod(LocalDate.of(2026, 4, 15)))
        assertEquals("2026-1", callAlertRemindPeriod(LocalDate.of(2026, 5, 20)))
        assertEquals("2026-1", callAlertRemindPeriod(LocalDate.of(2026, 6, 30)))
    }

    @Test
    fun callAlertRemindPeriod_returnsSecondSemesterForSeptemberToDecember() {
        assertEquals("2026-2", callAlertRemindPeriod(LocalDate.of(2026, 9, 1)))
        assertEquals("2026-2", callAlertRemindPeriod(LocalDate.of(2026, 10, 15)))
        assertEquals("2026-2", callAlertRemindPeriod(LocalDate.of(2026, 11, 20)))
        assertEquals("2026-2", callAlertRemindPeriod(LocalDate.of(2026, 12, 31)))
    }

    @Test
    fun callAlertRemindPeriod_returnsNullForVacationMonths() {
        assertNull(callAlertRemindPeriod(LocalDate.of(2026, 1, 15)))
        assertNull(callAlertRemindPeriod(LocalDate.of(2026, 2, 28)))
        assertNull(callAlertRemindPeriod(LocalDate.of(2026, 7, 1)))
        assertNull(callAlertRemindPeriod(LocalDate.of(2026, 8, 31)))
    }

    @Test
    fun shouldShowCallingAlertBottomSheet_returnsTrueForInitialUser() {
        val initialAlertData = AlertData(
            valid = false,
            allowSystemAlert = false,
            allowCallAlert = false,
            callingAlertThresholdMinutes = 60,
        )

        assertTrue(shouldShowCallingAlertBottomSheet(initialAlertData, currentPeriod = null))
        assertTrue(shouldShowCallingAlertBottomSheet(initialAlertData, currentPeriod = "2026-1"))
        assertTrue(shouldShowCallingAlertBottomSheet(initialAlertData, currentPeriod = "2026-2"))
    }

    @Test
    fun shouldShowCallingAlertBottomSheet_returnsFalseWhenCallingAlertIsEnabled() {
        val alertDataWithCalling = AlertData(
            valid = true,
            allowSystemAlert = true,
            allowCallAlert = true,
            callingAlertThresholdMinutes = 60,
            lastCallAlertRemindPeriod = null,
        )

        assertFalse(shouldShowCallingAlertBottomSheet(alertDataWithCalling, currentPeriod = "2026-1"))
        assertFalse(shouldShowCallingAlertBottomSheet(alertDataWithCalling, currentPeriod = "2026-2"))
    }

    @Test
    fun shouldShowCallingAlertBottomSheet_returnsFalseDuringVacationPeriod() {
        val alertDataWithoutCalling = AlertData(
            valid = true,
            allowSystemAlert = true,
            allowCallAlert = false,
            callingAlertThresholdMinutes = -1,
            lastCallAlertRemindPeriod = null,
        )

        assertFalse(shouldShowCallingAlertBottomSheet(alertDataWithoutCalling, currentPeriod = null))
    }

    @Test
    fun shouldShowCallingAlertBottomSheet_returnsTrueOncePerSemesterWhenDisabled() {
        val alertDataWithoutCalling = AlertData(
            valid = true,
            allowSystemAlert = true,
            allowCallAlert = false,
            callingAlertThresholdMinutes = -1,
            lastCallAlertRemindPeriod = null,
        )

        // 1학기 첫 진입 시 노출
        assertTrue(shouldShowCallingAlertBottomSheet(alertDataWithoutCalling, currentPeriod = "2026-1"))

        // 1학기에 노출 완료 후 다시 진입 시 미노출
        val updatedForSemester1 = alertDataWithoutCalling.copy(lastCallAlertRemindPeriod = "2026-1")
        assertFalse(shouldShowCallingAlertBottomSheet(updatedForSemester1, currentPeriod = "2026-1"))

        // 2학기가 되면 다시 1회 노출
        assertTrue(shouldShowCallingAlertBottomSheet(updatedForSemester1, currentPeriod = "2026-2"))

        // 2학기 노출 완료 후 다시 진입 시 미노출
        val updatedForSemester2 = updatedForSemester1.copy(lastCallAlertRemindPeriod = "2026-2")
        assertFalse(shouldShowCallingAlertBottomSheet(updatedForSemester2, currentPeriod = "2026-2"))

        // 다음 해 1학기가 되면 다시 1회 노출
        assertTrue(shouldShowCallingAlertBottomSheet(updatedForSemester2, currentPeriod = "2027-1"))
    }
}
