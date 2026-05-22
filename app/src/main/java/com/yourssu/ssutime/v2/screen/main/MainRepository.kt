package com.yourssu.ssutime.v2.screen.main

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.glance.appwidget.updateAll
import com.yourssu.data.AlertData
import com.yourssu.data.TodoData
import com.yourssu.ssutime.v2.network.ApiRepository
import com.yourssu.ssutime.v2.notification.cancelDeadlineNotifications
import com.yourssu.ssutime.v2.notification.sendDeadlineNotificationsIfNeeded
import com.yourssu.ssutime.v2.widget.DDayWidget
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

    suspend fun updateTodoData(todoData: TodoData) {
        todoDataStore.updateData { todoData }
        DDayWidget().updateAll(context)
        updateDeadlineNotifications(todoData)
    }

    suspend fun updateAlertData(alertData: AlertData) {
        Log.i("AlertData", "Update AlertData : $alertData")
        alertData.valid = true
        alertDataStore.updateData { alertData }
        if (alertData.allowSystemAlert) {
            updateDeadlineNotifications(getTodoData())
        } else {
            cancelDeadlineNotifications(context)
        }
        apiRepository.setNotificationSetting(alertData.allowCallAlert, alertData.callingAlertThresholdMinutes)
    }

    suspend fun updateTodoData(transform: (TodoData) -> TodoData) {
        val updatedTodoData = todoDataStore.updateData(transform)
        DDayWidget().updateAll(context)
        updateDeadlineNotifications(updatedTodoData)
    }

    suspend fun clearTodoData() {
        updateTodoData(TodoData())
    }

    suspend fun clearAlertData() {
        Log.i("AlertData", "Clear AlertData")
        updateAlertData(AlertData(valid = false, false, false, 60L))
    }

    suspend fun getTodoData(): TodoData = todoDataStore.data.first()
    suspend fun getAlertData(): AlertData = alertDataStore.data.first().apply { Log.i("AlertData", "get AlertData : $this") }

    private suspend fun updateDeadlineNotifications(todoData: TodoData) {
        if (getAlertData().allowSystemAlert) {
            val result = sendDeadlineNotificationsIfNeeded(context, todoData)
            if (result.sentKeys.isNotEmpty()) {
                todoDataStore.updateData { currentData ->
                    currentData.copy(
                        sentDeadlineReminderKeys = (currentData.sentDeadlineReminderKeys + result.sentKeys)
                            .distinct()
                            .takeLast(MAX_SENT_DEADLINE_REMINDER_KEYS),
                    )
                }
            }
        } else {
            cancelDeadlineNotifications(context)
        }
    }

    private companion object {
        const val MAX_SENT_DEADLINE_REMINDER_KEYS = 500
    }
}
