package com.yourssu.ssutime.screen.main

import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssu.data.TodoInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainViewModel(
    private val mainRepository: MainRepository,
    private val lmsRefreshRepository: LmsRefreshRepository,
) : ViewModel() {
    var todos = mutableStateListOf<TodoInfo>()
    var submitted = mutableStateListOf<TodoInfo>()
    var isLoading = mutableStateOf(false)
    var showLoading = mutableStateOf(false)
    var loadingProgress = mutableFloatStateOf(0f)
    var loadedAt = mutableStateOf("")

    suspend fun loadTodos() {
        if(isLoading.value) {
            return
        }

        isLoading.value = true
        showLoading.value = true
        loadingProgress.value = 0f

        try {
            val cachedTodoData = mainRepository.getTodoData()
            val hasCachedTodoData = cachedTodoData.loadedAt.isNotEmpty()
            if(hasCachedTodoData) {
                updateTodoState(cachedTodoData)
            }

            when (val refreshResult = lmsRefreshRepository.refreshTodos(
                source = RefreshSource.MANUAL,
                loadingState = {
                    viewModelScope.launch {
                        loadingProgress.value = it
                    }
                }
            )) {
                is TodoRefreshResult.Success -> {
                    loadingProgress.value = 1f
                    updateTodoState(refreshResult.todoData)
                }

                is TodoRefreshResult.Skipped -> {
                    Log.i(javaClass.name, refreshResult.reason)
                }

                is TodoRefreshResult.Failure -> {
                    Log.e(javaClass.name, refreshResult.message, refreshResult.throwable)
                }
            }
        } catch(e: Exception) {
            if(e is CancellationException) throw e
            Log.e(javaClass.name, "과제 정보를 갱신하지 못했습니다.", e)
        } finally {
            isLoading.value = false
            showLoading.value = false
        }
    }

    private fun updateTodoState(todoData: TodoData) {
        todos.apply {
            clear()
            addAll(todoData.todos)
        }

        submitted.apply {
            clear()
            addAll(todoData.submitted)
        }

        loadedAt.value = todoData.loadedAt
    }

}
