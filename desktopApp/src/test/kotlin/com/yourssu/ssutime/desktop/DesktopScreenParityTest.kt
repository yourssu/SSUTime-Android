@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.yourssu.ssutime.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.asSkiaBitmap
import com.yourssu.ssutime.desktop.screen.notice.DesktopNoticeScreen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.yourssu.data.*
import com.yourssu.data.network.AttachmentLinkResponse
import com.yourssu.ssutime.desktop.analytics.DesktopAnalytics
import com.yourssu.ssutime.desktop.screen.main.*
import com.yourssu.ssutime.desktop.screen.todo.DesktopTodoDetailScreen
import com.yourssu.ssutime.desktop.screen.my.DesktopMyPageScreen
import com.yourssu.ssutime.desktop.ui.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.*
import java.time.Instant

class DesktopScreenParityTest {
    @get:Rule val compose = createComposeRule()
    @Before fun suppressTelemetry() { DesktopAnalytics.testEventSink = {} }
    @After fun restoreTelemetry() { DesktopAnalytics.testEventSink = null }
    private val task = TodoInfo(todoId = 10, title = "QA assignment", due_date = "2027-01-01T14:59:59Z", type = TodoType.ASSIGNMENT,
        subject = SubjectInfo(1, "QA course", "Teacher"), description = "<p>Description from LMS</p>")

    private fun saveScreenshot(name: String) {
        val bitmap = compose.onAllNodes(isRoot()).onLast().captureToImage().asSkiaBitmap()
        val directory = java.nio.file.Path.of("build", "qa")
        java.nio.file.Files.createDirectories(directory)
        org.jetbrains.skia.Image.makeFromBitmap(bitmap).use { image ->
            image.encodeToData()!!.use { data -> java.nio.file.Files.write(directory.resolve("$name.png"), data.bytes) }
        }
    }

    @Test fun noticeStartsAtUnreadCourseAndKeepsItSelectedAfterReadUpdate() {
        val read = SubjectInfo(1, "Read course", "Teacher", listOf(DiscussionInfo(1, "Old announcement", readState = "read")))
        val unread = SubjectInfo(2, "Unread course", "Teacher", listOf(DiscussionInfo(2, "New announcement", message = "QA notice", readState = "unread")))
        val subjects = mutableStateOf(listOf(read, unread))
        compose.setContent { MaterialTheme { DesktopNoticeScreen(subjects.value, {}, {}, {
            subjects.value = listOf(read, unread.copy(discussions = unread.discussions.map { it.copy(readState = "read") }))
        }) } }
        compose.onNodeWithText("New announcement", substring = true).assertExists().performClick()
        compose.onNodeWithText("QA notice").assertExists()
        compose.onNodeWithText("New announcement", substring = true).assertExists()
        saveScreenshot("notice-read")
    }

    @Test fun calendarNoticeShortcutLoadsNoticeScreenAndReturnsToCalendar() {
        val subject = SubjectInfo(1, "QA course", "Teacher", listOf(
            DiscussionInfo(1, "QA announcement", message = "Actual notice content", readState = "unread"),
        ))
        val tab = mutableStateOf(DesktopNavTab.CALENDAR)
        compose.setContent { MaterialTheme { DesktopMainScreen(
            todoData = TodoData(subjects = listOf(subject), loadedAt = Instant.now().toString()),
            aiSummaryStates = emptyMap(), isLoading = false, loadingProgress = 1f, errorMessage = null,
            onRefresh = {}, onExpandTodo = {}, currentTab = tab.value, onTabSelect = { tab.value = it },
        ) } }
        compose.onNodeWithContentDescription(runBlocking { getString(Res.string.calendar_notice_shortcut) }).performClick()
        compose.onNodeWithText("QA announcement", substring = true).assertExists().performClick()
        compose.onNodeWithText("Actual notice content").assertExists()
        compose.onNodeWithContentDescription(runBlocking { getString(Res.string.my_back_content_description) }).performClick()
        compose.onNodeWithText("QA announcement", substring = true).assertDoesNotExist()
        compose.onNodeWithContentDescription(runBlocking { getString(Res.string.calendar_notice_shortcut) }).assertExists()
    }

    @Test fun learningXHtmlLinkOpensItsOriginalUrl() {
        var opened = ""
        compose.setContent { MaterialTheme {
            DesktopTodoDetailScreen(
                task.copy(description = """<p><b>설명</b> <a href="https://example.com/course?a=1&amp;b=2">자료 링크</a></p>"""),
                null, {}, {}, {}, { opened = it },
            )
        } }
        val node = compose.onNodeWithText("설명 자료 링크")
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        node.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        node.performTouchInput { click(layouts.single().getBoundingBox(4).center) }
        compose.runOnIdle { Assert.assertEquals("https://example.com/course?a=1&b=2", opened) }
    }

    @Test fun noticeHtmlLinkOpensItsOriginalUrl() {
        var opened = ""
        val subject = SubjectInfo(1, "QA course", "Teacher", listOf(
            DiscussionInfo(1, "Linked announcement", message = """<a href="https://example.com/notice">공지 링크</a>"""),
        ))
        compose.setContent { MaterialTheme { DesktopNoticeScreen(listOf(subject), {}, { opened = it }, {}) } }
        compose.onNodeWithText("Linked announcement").performClick()
        val node = compose.onNodeWithText("공지 링크")
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        node.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        node.performTouchInput { click(layouts.single().getBoundingBox(1).center) }
        compose.runOnIdle { Assert.assertEquals("https://example.com/notice", opened) }
    }

    @Test fun summaryTabAppearsAfterSuccessAndOpensLinkedAttachment() {
        val state = mutableStateOf<DesktopAiSummaryUiState>(DesktopAiSummaryUiState.Loading)
        val label = runBlocking { getString(Res.string.todo_detail_ai_summary_tab) }
        var opened = ""
        compose.setContent { MaterialTheme { DesktopTodoDetailScreen(task, state.value, {}, {}, {}, { opened = it }) } }
        compose.onNodeWithText(label).assertDoesNotExist()
        compose.runOnIdle { state.value = DesktopAiSummaryUiState.Success("Actual summary", null, listOf(AttachmentLinkResponse("https://example.com/file.pdf", "QA file.pdf", "pdf"))) }
        compose.onNodeWithText(label).assertExists().performClick()
        compose.onNodeWithText("Actual summary").assertExists()
        compose.onNodeWithText(runBlocking { getString(Res.string.ai_estimated_duration) }).assertDoesNotExist()
        compose.runOnIdle { state.value = (state.value as DesktopAiSummaryUiState.Success).copy(estimatedDurationMinutes = 30) }
        compose.onNodeWithText(runBlocking { getString(Res.string.ai_estimated_duration_minutes, 30) }).assertExists()
        saveScreenshot("todo-summary")
        compose.onNodeWithText("QA file.pdf").performScrollTo().performClick()
        compose.runOnIdle { Assert.assertEquals("https://example.com/file.pdf", opened) }
    }

    @Test fun detailTabsAndHideDialogUseAndroidTextForEachTaskType() {
        val selected = mutableStateOf(task)
        val hide = runBlocking { getString(Res.string.todo_hide_from_list) }
        val cancel = runBlocking { getString(Res.string.common_cancel) }
        compose.setContent { MaterialTheme {
            DesktopTodoDetailScreen(selected.value, null, {}, {}, {}, {})
        } }
        compose.onNodeWithText("러닝엑스 내용").assertExists()
        for ((type, title) in listOf(
            TodoType.ASSIGNMENT to Res.string.assignment_hide_popup_title,
            TodoType.QUIZ to Res.string.quiz_hide_popup_title,
            TodoType.COMMONS to Res.string.common_hide_popup_title,
        )) {
            compose.runOnIdle { selected.value = task.copy(type = type) }
            compose.onNodeWithText(hide).performClick()
            compose.onNodeWithText(runBlocking { getString(title) }).assertExists()
            compose.onNodeWithText(cancel).performClick()
        }
    }

    @Test fun deadlineLabelSwitchesOnlyDuringLateSubmissionWindow() {
        val now = Instant.now()
        val selected = mutableStateOf(task.copy(
            due_date = now.minusSeconds(3600).toString(),
            lateAt = now.plusSeconds(3600).toString(),
        ))
        val normalLabel = runBlocking { getString(Res.string.main_deadline_label) }
        val lateLabel = runBlocking { getString(Res.string.todo_detail_late_submission_deadline) }
        val lateNotice = runBlocking { getString(Res.string.todo_detail_late_notice) }
        compose.setContent { MaterialTheme {
            DesktopTodoDetailScreen(selected.value, null, {}, {}, {}, {})
        } }
        compose.onNodeWithText(lateLabel).assertExists()
        compose.onNodeWithText(normalLabel).assertDoesNotExist()
        compose.onNodeWithText(lateNotice).assertExists()
        compose.runOnIdle { selected.value = selected.value.copy(type = TodoType.SUBMITTED) }
        compose.onNodeWithText(normalLabel).assertExists()
        compose.onNodeWithText(lateLabel).assertDoesNotExist()
        compose.onNodeWithText(lateNotice).assertDoesNotExist()
        compose.runOnIdle { selected.value = selected.value.copy(type = TodoType.ASSIGNMENT, lateAt = "") }
        compose.onNodeWithText(normalLabel).assertExists()
        compose.onNodeWithText(lateLabel).assertDoesNotExist()
    }

    @Test fun clickingHomeTabResetsTheSelectedDetail() {
        val tab = mutableStateOf(DesktopNavTab.TODO)
        compose.setContent { MaterialTheme { DesktopMainScreen(
            todoData = TodoData(todos = listOf(task), loadedAt = Instant.now().toString()), aiSummaryStates = emptyMap(),
            isLoading = false, loadingProgress = 1f, errorMessage = null, onRefresh = {}, onExpandTodo = {},
            currentTab = tab.value, onTabSelect = { tab.value = it }, isCyberConnected = true,
        ) } }
        compose.onNodeWithText(task.title).performClick()
        compose.onNodeWithText("Description from LMS").assertExists()
        compose.onNodeWithContentDescription(runBlocking { getString(Res.string.desktop_nav_todo) }).performClick()
        compose.onNodeWithText("Description from LMS").assertDoesNotExist()
        compose.onNodeWithText(task.title).assertExists()
    }

    @Test fun withdrawalRequiresConfirmationAndBusyStatePreventsRepeatedSubmission() {
        val busy = mutableStateOf(false)
        val error = mutableStateOf<String?>(null)
        var attempts = 0
        val withdraw = runBlocking { getString(Res.string.my_withdraw) }
        val confirm = runBlocking { getString(Res.string.common_confirm) }
        compose.setContent { MaterialTheme { DesktopMyPageScreen(
            profile = null, isLoading = false, errorMessage = null, onOpenUrl = {}, onLogout = {},
            onWithdrawAccount = { attempts++; busy.value = true }, isWithdrawing = busy.value, withdrawalError = error.value,
        ) } }
        compose.onNodeWithText(withdraw).performScrollTo().performClick()
        compose.onNodeWithText(confirm).performClick()
        compose.onNodeWithText(confirm).performClick()
        compose.runOnIdle { Assert.assertEquals(1, attempts); busy.value = false; error.value = "QA request failed" }
        compose.onNodeWithText("QA request failed").assertExists()
        saveScreenshot("withdraw-retry")
        compose.onNodeWithText(confirm).performClick()
        compose.runOnIdle { Assert.assertEquals(2, attempts) }
    }
}
