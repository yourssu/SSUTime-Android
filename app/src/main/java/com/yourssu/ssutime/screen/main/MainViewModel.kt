package com.yourssu.ssutime.screen.main

import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import io.github.chlwhdtn03.LmsApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlin.time.ExperimentalTime

class MainViewModel(
    private val mainRepository: MainRepository
) : ViewModel() {
    var todos = mutableStateListOf<TodoInfo>()
    var submitted = mutableStateListOf<TodoInfo>()
    var isLoading = mutableStateOf(false)
    var showLoading = mutableStateOf(false)
    var loadingProgress = mutableFloatStateOf(0f)
    var loadedAt = mutableStateOf("")

    @OptIn(ExperimentalTime::class)
    suspend fun loadTodos() {
        if(isLoading.value) {
            return
        }

        isLoading.value = true
        loadingProgress.value = 0f

        try {
            val cachedTodoData = mainRepository.getTodoData()
            val hasCachedTodoData = cachedTodoData.loadedAt.isNotEmpty()
            showLoading.value = !hasCachedTodoData
            if(hasCachedTodoData) {
                updateTodoState(cachedTodoData)
            }

            if(LmsApi.isLoggined) {
                val todoData = withContext(Dispatchers.IO) {
                    val todoList = LmsApi.getTodoList(
                        term = LmsApi.getTerms().first()
                    ) {
                        viewModelScope.launch {
                            loadingProgress.value = it
                        }
                    }

                    // 불러온 정보에서 과목 정보 먼저 보관
                    val subjects = todoList.map { SubjectInfo(it.id, it.name, it.professor) }.toSet()
                    val newTodos = todoList.flatMap { subject ->
                        subject.todoList.map { todo ->
                            Log.d("Todos", todo.due_date)
                            TodoInfo(
                                todo.assignment_id ?: -1,
                                todo.title,
                                todo.due_date,
                                TodoType.valueOf(todo.component_type.uppercase()),
                                subjects.find { it.id == subject.id }
                            )
                        }
                    }.sortedBy { it.due_date }

                    val newSubmitted = todoList.flatMap { subject ->
                        subject.submissions
                            .filter { it.submitted_at.isNotEmpty() }
                            .map { todo ->
                                TodoInfo(
                                    todo.assignment_id,
                                    todo.name,
                                    todo.cached_due_date,
                                    if(todo.late) TodoType.SUBMITTED_LATE else TodoType.SUBMITTED,
                                    subjects.find { it.id == subject.id }
                                )
                            }
                    }.sortedByDescending { it.due_date }

                    TodoData(
                        todos = newTodos,
                        submitted = newSubmitted,
                        loadedAt = Instant.now().toString()
                    )
                }

                updateTodoState(todoData)
                mainRepository.updateTodoData(todoData)
            } else {
                // TODO 재로그인 로직 필요
                Log.e(javaClass.name, "로그인이 되어있지 않아 정보를 불러올 수 없습니다")
                Log.e(javaClass.name, "앱 재실행을 요청하세요")
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
            clear();
            addAll(todoData.todos)
        }

        submitted.apply {
            clear()
            addAll(todoData.submitted)
        }

        loadedAt.value = todoData.loadedAt
    }

}
