package com.yourssu.ssutime.v2.screen.main

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.glance.appwidget.updateAll
import com.yourssu.data.AlertData
import com.yourssu.data.TodoData
import com.yourssu.ssutime.v2.widget.DDayWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MainRepository(
    private val todoDataStore: DataStore<TodoData>,
    private val alertDataStore: DataStore<AlertData>,
    private val context: Context,
) {
    val todoData: Flow<TodoData> = todoDataStore.data
    val alertData: Flow<AlertData> = alertDataStore.data

    suspend fun updateTodoData(todoData: TodoData) {
        todoDataStore.updateData { todoData }
        DDayWidget().updateAll(context)
    }

    suspend fun updateAlertData(alertData: AlertData) {
        Log.i("AlertData", "Update AlertData : $alertData")
        alertData.valid = true
        alertDataStore.updateData { alertData }
        if(alertData.callingAlertThresholdMinutes > 0L) {
            // TODO POST Backend
        }
    }

    suspend fun updateTodoData(transform: (TodoData) -> TodoData) {
        todoDataStore.updateData(transform)
        DDayWidget().updateAll(context)
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
}
