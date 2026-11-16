package com.yourssu.ssutime.v2.screen.main

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import com.yourssu.data.AiSummaryCache
import com.yourssu.data.AlertData
import com.yourssu.data.LabsData
import com.yourssu.data.LoginData
import com.yourssu.data.TodoData
import com.yourssu.data.TodoInfo
import com.yourssu.data.network.AssignmentAnalysisResponse
import com.yourssu.data.network.AttachmentLinkResponse
import com.yourssu.data.network.LmsSessionRequest
import com.yourssu.data.network.UserTodoStatusResponse
import com.yourssu.data.network.toReportWithAnalysisRequestOrNull
import com.yourssu.data.todoUniqueKey
import com.yourssu.ssutime.v2.accessToken
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter
import com.yourssu.ssutime.v2.network.ApiRepository
import com.yourssu.ssutime.v2.notification.cancelDeadlineNotifications
import com.yourssu.ssutime.v2.notification.scheduleDeadlineNotifications
import com.yourssu.ssutime.v2.notification.sendDeadlineNotificationsIfNeeded
import com.yourssu.ssutime.v2.notification.sendNewTodoAnnouncementsIfNeeded
import com.yourssu.ssutime.v2.notification.withSentDeadlineReminderKeys
import com.yourssu.ssutime.v2.screen.login.LoginRepository
import com.yourssu.ssutime.v2.screen.my.AlertLocalStore
import com.yourssu.ssutime.v2.todo.sortedByDeadlineThenName
import com.yourssu.ssutime.v2.todo.withSubmissionOrder
import com.yourssu.ssutime.v2.widget.updateAllTodoWidgets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

class MainRepository(
    private val todoDataStore: DataStore<TodoData>,
    private val alertDataStore: DataStore<AlertData>,
    private val labsDataStore: DataStore<LabsData>,
    private val apiRepository: ApiRepository,
    private val loginRepository: LoginRepository,
    private val context: Context,
) {
    val todoData: Flow<TodoData> = todoDataStore.data
    val alertData: Flow<AlertData> = alertDataStore.data
    val labsData: Flow<LabsData> = labsDataStore.data

    suspend fun getLabsData(): LabsData = labsDataStore.data.first()

    suspend fun updateLabsData(labsData: LabsData) {
        labsDataStore.updateData { labsData }
    }

    suspend fun clearLabsData() {
        labsDataStore.updateData { LabsData() }
    }

    suspend fun updateTodoData(todoData: TodoData): TodoData {
        val updatedTodoData = todoDataStore.updateData { todoData.withSubmissionOrder() }
        updateAllTodoWidgets(context)
        updateDeadlineNotifications(updatedTodoData)
        return updatedTodoData
    }

    suspend fun updateAlertData(alertData: AlertData) {
        Log.i("AlertData", "Update AlertData : $alertData")
        val currentAlertData = getAlertData()
        val updatedAlertData = alertData.copy(
            valid = true,
            showWidgetHelperBadge = currentAlertData.showWidgetHelperBadge,
            lastCallAlertRemindPeriod = alertData.lastCallAlertRemindPeriod
                ?: currentAlertData.lastCallAlertRemindPeriod,
        )
        AlertLocalStore.saveAlertData(context, updatedAlertData)
        alertDataStore.updateData { updatedAlertData }
        if (updatedAlertData.allowSystemAlert) {
            updateDeadlineNotifications(getTodoData())
        } else {
            cancelDeadlineNotifications(context)
        }
        runCatching {
            withValidAccessToken {
                apiRepository.setNotificationSetting(
                    updatedAlertData.allowCallAlert,
                    updatedAlertData.callingAlertThresholdMinutes,
                )
            }
        }.onFailure { exception ->
            if (exception is CancellationException) throw exception
            SentryExceptionReporter.capture(exception)
            Log.w("MainRepository", "알림 설정 서버 동기화에 실패했습니다.", exception)
        }
    }

    suspend fun updateTodoData(transform: (TodoData) -> TodoData): TodoData {
        val updatedTodoData = todoDataStore.updateData { previous -> transform(previous).withSubmissionOrder() }
        updateAllTodoWidgets(context)
        updateDeadlineNotifications(updatedTodoData)
        return updatedTodoData
    }

    suspend fun clearTodoData() {
        updateTodoData(TodoData())
        cancelDeadlineNotifications(context)
    }

    suspend fun markDiscussionAsRead(discussionId: Int) {
        updateTodoData { currentData ->
            val updatedKeys = (currentData.readDiscussionIds + discussionId).distinct()
            val updatedSubjects = currentData.subjects.map { subject ->
                val updatedDiscussions = subject.discussions.map { discussion ->
                    if (discussion.id == discussionId) {
                        discussion.copy(readState = "read")
                    } else {
                        discussion
                    }
                }
                subject.copy(discussions = updatedDiscussions)
            }
            currentData.copy(
                subjects = updatedSubjects,
                readDiscussionIds = updatedKeys,
            )
        }
    }

    suspend fun hideTodo(todo: TodoInfo) {
        val key = todo.todoUniqueKey()
        updateTodoData { currentData ->
            val updatedKeys = (currentData.hiddenTodoKeys + key).distinct()
            val updatedTodos = currentData.todos.filterNot { it.todoUniqueKey() == key }
            val updatedHiddenTodos = (currentData.hiddenTodos.filterNot { it.todoUniqueKey() == key } + todo)
                .sortedByDeadlineThenName()
            currentData.copy(
                todos = updatedTodos,
                hiddenTodos = updatedHiddenTodos,
                hiddenTodoKeys = updatedKeys,
            )
        }
    }

    suspend fun restoreTodo(todo: TodoInfo) {
        val key = todo.todoUniqueKey()
        updateTodoData { currentData ->
            val updatedKeys = currentData.hiddenTodoKeys.filterNot { it == key }
            val updatedHiddenTodos = currentData.hiddenTodos.filterNot { it.todoUniqueKey() == key }
            val updatedTodos = (currentData.todos.filterNot { it.todoUniqueKey() == key } + todo)
                .sortedByDeadlineThenName()
            currentData.copy(
                todos = updatedTodos,
                hiddenTodos = updatedHiddenTodos,
                hiddenTodoKeys = updatedKeys,
            )
        }
    }

    suspend fun clearAlertData() {
        Log.i("AlertData", "Clear AlertData")
        updateAlertData(AlertData(valid = false, false, false, 60L))
    }

    suspend fun getTodoData(): TodoData = todoDataStore.data.first()
    suspend fun getAlertData(): AlertData = alertDataStore.data.first().apply {
        Log.i("AlertData", "get AlertData : $this")
        AlertLocalStore.saveAlertData(context, this)
    }

    suspend fun getReportedTodos(): List<UserTodoStatusResponse> =
        withValidAccessToken {
            apiRepository.getTodos()
        }

    suspend fun getCachedAiSummary(key: String): AiSummaryCache? =
        getTodoData().aiSummaryCache[key]?.takeIf { it.summary.isNotBlank() }

    suspend fun cacheAiSummary(
        key: String,
        summary: String,
        estimatedDurationMinutes: Int?,
        attachmentLinks: List<AttachmentLinkResponse> = emptyList(),
    ) {
        val cache = AiSummaryCache(
            summary = summary,
            estimatedDurationMinutes = estimatedDurationMinutes,
            attachmentLinks = attachmentLinks,
        )
        todoDataStore.updateData { currentData ->
            currentData.copy(
                aiSummaryCache = currentData.aiSummaryCache + (key to cache),
            )
        }
    }

    suspend fun reportTodoWithAnalysis(
        todo: TodoInfo,
        lmsSession: LmsSessionRequest,
    ): AssignmentAnalysisResponse? {
        val request = todo.toReportWithAnalysisRequestOrNull(lmsSession) ?: run {
            Log.w("MainRepository", "AI summary request skipped: unsupported todo payload")
            return null
        }
        return withValidAccessToken {
            apiRepository.reportTodoWithAnalysis(request)
        }
    }

    suspend fun dismissWidgetHelperBadge() {
        alertDataStore.updateData { currentData ->
            currentData.copy(showWidgetHelperBadge = false)
        }
    }

    private suspend fun updateDeadlineNotifications(todoData: TodoData) {
        if (getAlertData().allowSystemAlert) {
            // Recover today's announcement before replacing a delayed 18:00 alarm with tomorrow's.
            try {
                sendNewTodoAnnouncementsIfNeeded(context)
            } finally {
                scheduleDeadlineNotifications(context)
            }
            val result = sendDeadlineNotificationsIfNeeded(context, todoData)
            if (result.sentKeys.isNotEmpty()) {
                todoDataStore.updateData { currentData ->
                    currentData.withSentDeadlineReminderKeys(result.sentKeys)
                }
            }
        } else {
            cancelDeadlineNotifications(context)
        }
    }

    private suspend fun <T> withValidAccessToken(block: suspend () -> T): T {
        ensureBackendToken()
        return try {
            block()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            if (!exception.isUnauthorizedError() || !refreshBackendToken()) {
                throw exception
            }
            block()
        }
    }

    private suspend fun ensureBackendToken(): Boolean {
        if (accessToken.isBlank()) {
            val loginData = loginRepository.getLoginData()
            accessToken = loginData.accessToken
            if (accessToken.isBlank()) {
                return refreshBackendToken(loginData)
            }
        }
        return true
    }

    private suspend fun refreshBackendToken(cachedLoginData: LoginData? = null): Boolean {
        val loginData = cachedLoginData ?: loginRepository.getLoginData()
        if (!loginData.hasAutoLoginCredentials) {
            return false
        }
        return try {
            val newToken = apiRepository.requestJwtToken(loginData.id, loginData.pw).accessToken
            accessToken = newToken
            loginRepository.updateLoginData(loginData.copy(accessToken = newToken))
            true
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            SentryExceptionReporter.capture(exception)
            Log.e("MainRepository", "백엔드 토큰 갱신에 실패했습니다.", exception)
            false
        }
    }

    private fun Throwable.isUnauthorizedError(): Boolean =
        message?.startsWith("HTTP 401") == true
}
