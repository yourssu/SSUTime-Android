package com.yourssu.ssutime.screen.main

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MainRepository(
    private val todoDataStore: DataStore<TodoData>
) {
    val todoData: Flow<TodoData> = todoDataStore.data

    suspend fun updateTodoData(todoData: TodoData) {
        todoDataStore.updateData { todoData }
    }

    suspend fun updateTodoData(transform: (TodoData) -> TodoData) {
        todoDataStore.updateData(transform)
    }

    suspend fun getTodoData(): TodoData = todoDataStore.data.first()
}
