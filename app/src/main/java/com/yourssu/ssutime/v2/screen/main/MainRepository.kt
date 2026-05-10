package com.yourssu.ssutime.v2.screen.main

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MainRepository(
    private val todoDataStore: DataStore<TodoData>,
    private val context: Context,
) {
    val todoData: Flow<TodoData> = todoDataStore.data

    suspend fun updateTodoData(todoData: TodoData) {
        todoDataStore.updateData { todoData }
        _root_ide_package_.com.yourssu.ssutime.v2.widget.DDayWidget().updateAll(context)
    }

    suspend fun updateTodoData(transform: (TodoData) -> TodoData) {
        todoDataStore.updateData(transform)
        _root_ide_package_.com.yourssu.ssutime.v2.widget.DDayWidget().updateAll(context)
    }

    suspend fun getTodoData(): TodoData = todoDataStore.data.first()
}
