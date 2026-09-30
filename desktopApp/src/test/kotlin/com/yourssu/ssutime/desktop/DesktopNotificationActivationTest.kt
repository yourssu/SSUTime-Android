package com.yourssu.ssutime.desktop

import org.junit.Test
import org.junit.Rule
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import java.time.Instant
import java.time.Duration
import com.yourssu.data.TodoInfo
import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoType

class DesktopNotificationActivationTest {
    @get:Rule val temporary = TemporaryFolder()
    private val now = Instant.parse("2026-10-01T09:00:00Z")
    private val notification = DeadlineNotification("Title", "Body", listOf("key"), dDay = 2,
        notificationTaskCount = 3, notificationType = "deadline_soon",
        representativeTodo = TodoInfo(todoId = 1, title = "Task", due_date = "2026-10-03T14:59:00Z", type = TodoType.ASSIGNMENT,
            subject = SubjectInfo(1, "QA course", "Teacher")))

    @Test fun activationRoundTripsAcrossProcessesAndIsConsumedOnce() {
        val directory = temporary.newFolder().toPath()
        val producer = DesktopNotificationActivation(directory)
        val uri = producer.register(notification, now)
        assertFalse(uri.contains("QA"))
        assertTrue(DesktopNotificationActivation(directory).request(uri))
        val consumer = DesktopNotificationActivation(directory)
        val result = consumer.consume(now).single()
        assertEquals("QA course", result.subjectName)
        assertEquals(2, result.dDay)
        assertEquals(3, result.taskCount)
        assertEquals("deadline_soon", result.properties()["notification_type"])
        assertTrue(consumer.consume(now).isEmpty())
    }

    @Test fun invalidOrUnknownProtocolsCannotQueueNotificationData() {
        val store = DesktopNotificationActivation(temporary.newFolder().toPath())
        assertFalse(store.request("https://example.com"))
        assertFalse(store.request("ssutime://notification/../../other"))
        assertFalse(store.request("ssutime://notification/00000000-0000-0000-0000-000000000000"))
        val valid = store.register(notification, now)
        assertFalse(store.request("$valid?extra=true"))
        assertTrue(store.consume(now).isEmpty())
    }

    @Test fun expiredActivationIsNotReportedAsACurrentTap() {
        val store = DesktopNotificationActivation(temporary.newFolder().toPath())
        val uri = store.register(notification, now)
        assertTrue(store.request(uri))
        assertTrue(store.consume(now.plus(Duration.ofDays(8))).isEmpty())
    }
}
