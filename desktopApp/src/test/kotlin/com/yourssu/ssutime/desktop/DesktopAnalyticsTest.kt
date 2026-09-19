package com.yourssu.ssutime.desktop

import com.yourssu.data.AttachmentInfo
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.ssutime.desktop.analytics.DesktopAnalytics
import com.yourssu.ssutime.desktop.analytics.toDetailType
import com.yourssu.ssutime.desktop.core.model.AppTodo
import com.yourssu.ssutime.desktop.core.model.AppTodoType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopAnalyticsTest {

    @Test
    fun `TodoInfo toDetailType returns video for COMMONS type or positive duration`() {
        val videoLecture = TodoInfo(
            title = "1주차 강의",
            due_date = "2026-09-30 23:59:59",
            type = TodoType.COMMONS,
            subject = null,
            duration = 1800.0,
        )
        assertEquals("video", videoLecture.toDetailType())

        val videoWithDurationOnly = TodoInfo(
            title = "온라인 강의",
            due_date = "2026-09-30 23:59:59",
            type = TodoType.ASSIGNMENT,
            subject = null,
            duration = 120.0,
        )
        assertEquals("video", videoWithDurationOnly.toDetailType())
    }

    @Test
    fun `TodoInfo toDetailType returns attachment for todo with attachments`() {
        val todoWithAttachment = TodoInfo(
            title = "과제 1",
            due_date = "2026-09-30 23:59:59",
            type = TodoType.ASSIGNMENT,
            subject = null,
            duration = -1.0,
            attachments = listOf(
                AttachmentInfo(
                    id = 1,
                    uuid = "uuid-1",
                    folder_id = "folder-1",
                    display_name = "report.docx",
                    url = "https://example.com/report.docx",
                    size = 1024L,
                    created_at = "2026-09-01",
                    updated_at = "2026-09-01",
                    modified_at = "2026-09-01",
                    mime_class = "document",
                )
            ),
        )
        assertEquals("attachment", todoWithAttachment.toDetailType())
    }

    @Test
    fun `TodoInfo toDetailType returns default for regular assignment without attachments`() {
        val regularTodo = TodoInfo(
            title = "퀴즈 1",
            due_date = "2026-09-30 23:59:59",
            type = TodoType.QUIZ,
            subject = null,
            duration = -1.0,
            attachments = emptyList(),
        )
        assertEquals("default", regularTodo.toDetailType())
    }

    @Test
    fun `AppTodo toDetailType returns video for COMMONS type or positive duration`() {
        val videoLecture = AppTodo(
            title = "1주차 강의",
            due_date = "2026-09-30 23:59:59",
            type = AppTodoType.COMMONS,
            subject = null,
            duration = 1800.0,
        )
        assertEquals("video", videoLecture.toDetailType())

        val videoWithDurationOnly = AppTodo(
            title = "온라인 강의",
            due_date = "2026-09-30 23:59:59",
            type = AppTodoType.ASSIGNMENT,
            subject = null,
            duration = 120.0,
        )
        assertEquals("video", videoWithDurationOnly.toDetailType())
    }

    @Test
    fun `AppTodo toDetailType returns attachment for todo with attachments`() {
        val todoWithAttachment = AppTodo(
            title = "과제 1",
            due_date = "2026-09-30 23:59:59",
            type = AppTodoType.ASSIGNMENT,
            subject = null,
            duration = -1.0,
            attachments = listOf(
                AttachmentInfo(
                    id = 1,
                    uuid = "uuid-1",
                    folder_id = "folder-1",
                    display_name = "report.docx",
                    url = "https://example.com/report.docx",
                    size = 1024L,
                    created_at = "2026-09-01",
                    updated_at = "2026-09-01",
                    modified_at = "2026-09-01",
                    mime_class = "document",
                )
            ),
        )
        assertEquals("attachment", todoWithAttachment.toDetailType())
    }

    @Test
    fun `AppTodo toDetailType returns default for regular assignment without attachments`() {
        val regularTodo = AppTodo(
            title = "퀴즈 1",
            due_date = "2026-09-30 23:59:59",
            type = AppTodoType.QUIZ,
            subject = null,
            duration = -1.0,
            attachments = emptyList(),
        )
        assertEquals("default", regularTodo.toDetailType())
    }

    @Test
    fun `selectedTimeFromMinutes maps 1h 2h 3h 6h 12h correctly`() {
        assertEquals("1h", DesktopAnalytics.selectedTimeFromMinutes(60L))
        assertEquals("2h", DesktopAnalytics.selectedTimeFromMinutes(120L))
        assertEquals("3h", DesktopAnalytics.selectedTimeFromMinutes(180L))
        assertEquals("6h", DesktopAnalytics.selectedTimeFromMinutes(360L))
        assertEquals("12h", DesktopAnalytics.selectedTimeFromMinutes(720L))
        assertNull(DesktopAnalytics.selectedTimeFromMinutes(999L))
    }

    @Test
    fun `postHogDistinctId returns hashed id for valid login id`() {
        val hashed = DesktopAnalytics.postHogDistinctId("20210001")
        assertNotNull(hashed)
        assertTrue(hashed!!.startsWith("lms_sha256:"))
        assertNull(DesktopAnalytics.postHogDistinctId("   "))
    }

    @Test
    fun `initializeAnonymousId reuses existing or generates new UUID`() {
        val savedId = "existing-uuid-1234"
        val initialized = DesktopAnalytics.initializeAnonymousId(savedId)
        assertEquals(savedId, initialized)
        assertEquals(savedId, DesktopAnalytics.currentAnonymousId())

        val generated = DesktopAnalytics.initializeAnonymousId("")
        assertTrue(generated.isNotBlank())
        assertNotEquals("", generated)
        assertEquals(generated, DesktopAnalytics.currentAnonymousId())
    }

    @Test
    fun `resetUser generates a new anonymous id and clears identified id`() {
        runBlocking {
            DesktopAnalytics.identifyUser("20210001")
            assertEquals(DesktopAnalytics.postHogDistinctId("20210001"), DesktopAnalytics.currentPostHogDistinctId())

            val newAnonId = DesktopAnalytics.resetUser()
            assertTrue(newAnonId.isNotBlank())
            assertNull(DesktopAnalytics.currentPostHogDistinctId())
            assertEquals(newAnonId, DesktopAnalytics.currentAnonymousId())
        }
    }

    @Test
    fun `applicationScope executes background task successfully`() {
        var completed = false
        val job = DesktopAnalytics.launch {
            completed = true
        }
        runBlocking {
            job.join()
        }
        assertTrue(completed)
    }

    @Test
    fun `capture methods execute cleanly without throwing`() {
        DesktopAnalytics.viewLogin(isOnboarding = true)
        DesktopAnalytics.viewLogin(isOnboarding = false)
        DesktopAnalytics.viewLogin()
        DesktopAnalytics.loginAttempt(autoLogin = true)
        DesktopAnalytics.loginSuccess()
        DesktopAnalytics.loginFailIfKnown("network timeout")
        DesktopAnalytics.loginFailIfKnown("wrong credentials")
        DesktopAnalytics.viewHome(taskCount = 5, urgentCount = 2, entrySource = "home")
        DesktopAnalytics.viewCalendar()
        DesktopAnalytics.calendarDateClick()
        DesktopAnalytics.viewNotice()
        DesktopAnalytics.noticeExpand(isUnread = true)
        DesktopAnalytics.viewHiddenTasks()
        DesktopAnalytics.restoreClick()
        DesktopAnalytics.cyberConnectClick(entryPoint = "mypage")
        DesktopAnalytics.cyberLoginSuccess()
        DesktopAnalytics.cyberLoginFail()
        DesktopAnalytics.cyberFindIdClick()
        DesktopAnalytics.cyberDisconnectClick()
        DesktopAnalytics.refreshClick()
        DesktopAnalytics.pullToRefresh()
        DesktopAnalytics.taskDetailTabClick("ai_summary")
        DesktopAnalytics.lmsLinkClick()
        DesktopAnalytics.hideConfirm()
        DesktopAnalytics.submitCompleteClick()
        DesktopAnalytics.viewMyPage()
        DesktopAnalytics.kakaoClick()
        DesktopAnalytics.settingSystemAlarm(true)
        DesktopAnalytics.logoutClick()
        DesktopAnalytics.logoutCancel()
        DesktopAnalytics.logoutConfirm()
        DesktopAnalytics.flush()
    }
}
