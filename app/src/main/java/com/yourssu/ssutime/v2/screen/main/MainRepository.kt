package com.yourssu.ssutime.v2.screen.main

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import com.yourssu.data.AiSummaryCache
import com.yourssu.data.AlertData
import com.yourssu.data.TodoData
import com.yourssu.data.TodoInfo
import com.yourssu.data.network.LmsSessionRequest
import com.yourssu.data.network.UserTodoStatusResponse
import com.yourssu.data.network.toReportWithAnalysisRequestOrNull
import com.yourssu.data.todoUniqueKey
import com.yourssu.ssutime.v2.network.ApiRepository
import com.yourssu.ssutime.v2.notification.cancelDeadlineNotifications
import com.yourssu.ssutime.v2.notification.scheduleDeadlineNotifications
import com.yourssu.ssutime.v2.notification.sendDeadlineNotificationsIfNeeded
import com.yourssu.ssutime.v2.notification.withSentDeadlineReminderKeys
import com.yourssu.ssutime.v2.todo.sortedByDeadlineThenName
import com.yourssu.ssutime.v2.widget.updateAllTodoWidgets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MainRepository(
    private val todoDataStore: DataStore<TodoData>,
    private val alertDataStore: DataStore<AlertData>,
    private val apiRepository: ApiRepository,
    private val context: Context,
) {
    val todoData: Flow<TodoData> = todoDataStore.data
    val alertData: Flow<AlertData> = alertDataStore.data

    suspend fun updateTodoData(todoData: TodoData): TodoData {
        val updatedTodoData = todoDataStore.updateData { todoData }
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
        alertDataStore.updateData { updatedAlertData }
        if (updatedAlertData.allowSystemAlert) {
            updateDeadlineNotifications(getTodoData())
        } else {
            cancelDeadlineNotifications(context)
        }
        apiRepository.setNotificationSetting(
            updatedAlertData.allowCallAlert,
            updatedAlertData.callingAlertThresholdMinutes,
        )
    }

    suspend fun updateTodoData(transform: (TodoData) -> TodoData): TodoData {
        val updatedTodoData = todoDataStore.updateData(transform)
        updateAllTodoWidgets(context)
        updateDeadlineNotifications(updatedTodoData)
        return updatedTodoData
    }

    suspend fun clearTodoData() {
        updateTodoData(TodoData())
    }

    suspend fun markDiscussionAsRead(discussionId: Int) {
        updateTodoData { currentData ->
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
            currentData.copy(subjects = updatedSubjects)
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
    suspend fun getAlertData(): AlertData = alertDataStore.data.first().apply { Log.i("AlertData", "get AlertData : $this") }

    suspend fun getReportedTodos(): List<UserTodoStatusResponse> =
        apiRepository.getTodos()

    suspend fun getCachedAiSummary(key: String): AiSummaryCache? =
        getTodoData().aiSummaryCache[key]?.takeIf { it.summary.isNotBlank() }

    suspend fun cacheAiSummary(
        key: String,
        summary: String,
        estimatedDurationMinutes: Int?,
    ) {
        val cache = AiSummaryCache(
            summary = summary,
            estimatedDurationMinutes = estimatedDurationMinutes,
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
    ) {
        val request = todo.toReportWithAnalysisRequestOrNull(lmsSession) ?: return
        apiRepository.reportTodoWithAnalysis(request)
    }

    suspend fun dismissWidgetHelperBadge() {
        alertDataStore.updateData { currentData ->
            currentData.copy(showWidgetHelperBadge = false)
        }
    }

    private suspend fun updateDeadlineNotifications(todoData: TodoData) {
        if (getAlertData().allowSystemAlert) {
            scheduleDeadlineNotifications(context)
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
}
