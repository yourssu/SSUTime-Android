package com.yourssu.ssutime.v2.notification

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallAlertSessionTest {
    @After
    fun tearDown() {
        CallAlertSession.activeNotificationId()
            ?.toInt()
            ?.let(CallAlertSession::finish)
    }

    @Test
    fun tryStart_blocksNewCallAlertWhileOneIsActive() {
        assertTrue(CallAlertSession.tryStart(notificationId = 1))

        assertFalse(CallAlertSession.tryStart(notificationId = 2))
        assertEquals(1L, CallAlertSession.activeNotificationId())
    }

    @Test
    fun finish_ignoresDifferentNotificationId() {
        assertTrue(CallAlertSession.tryStart(notificationId = 1))

        CallAlertSession.finish(notificationId = 999)

        assertEquals(1L, CallAlertSession.activeNotificationId())
    }

    @Test
    fun finish_allowsNextCallAlertWhenActiveNotificationFinishes() {
        assertTrue(CallAlertSession.tryStart(notificationId = 1))

        CallAlertSession.finish(notificationId = 1)

        assertNull(CallAlertSession.activeNotificationId())
        assertTrue(CallAlertSession.tryStart(notificationId = 2))
    }
}
