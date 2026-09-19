package com.yourssu.ssutime.v2.analytics

import com.yourssu.data.AttachmentInfo
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalyticsTest {

    @Test
    fun `toDetailType returns video for COMMONS type or positive duration`() {
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
    fun `toDetailType returns attachment for todo with attachments`() {
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
    fun `toDetailType returns default for regular assignment without attachments`() {
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
    fun `selectedTimeFromMinutes maps 3h 6h 12h correctly`() {
        assertEquals("3h", Analytics.selectedTimeFromMinutes(180L))
        assertEquals("6h", Analytics.selectedTimeFromMinutes(360L))
        assertEquals("12h", Analytics.selectedTimeFromMinutes(720L))
        assertEquals("1h", Analytics.selectedTimeFromMinutes(60L))
        assertEquals("2h", Analytics.selectedTimeFromMinutes(120L))
        assertNull(Analytics.selectedTimeFromMinutes(999L))
    }

    @Test
    fun `postHogDistinctId returns hashed id for valid login id`() {
        val hashed = Analytics.postHogDistinctId("20210001")
        assertEquals(true, hashed?.startsWith("lms_sha256:"))
        assertNull(Analytics.postHogDistinctId("   "))
    }

    @Test
    fun `applicationScope executes background task successfully`() {
        var completed = false
        val job = Analytics.launch {
            completed = true
        }
        kotlinx.coroutines.runBlocking {
            job.join()
        }
        assertEquals(true, completed)
    }

    @Test
    fun `viewLogin executes cleanly with onboarding property options`() {
        Analytics.viewLogin(isOnboarding = true)
        Analytics.viewLogin(isOnboarding = false)
        Analytics.viewLogin()
    }
}
