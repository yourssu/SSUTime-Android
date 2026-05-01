package com.yourssu.ssutime.screen.main

import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import io.github.chlwhdtn03.LmsApi
import kotlin.time.ExperimentalTime

class MainViewModel(

) : ViewModel() {
    var todos = mutableStateListOf<TodoInfo>()
    var submitted = mutableStateListOf<TodoInfo>()
    var isLoading = mutableStateOf(false)
    var loadingProgress = mutableFloatStateOf(0f)

    @OptIn(ExperimentalTime::class)
    suspend fun loadTodos() {
        if(LmsApi.isLoggined) {
            isLoading.value = true
            loadingProgress.value = 0f
            val todoList = LmsApi.getTodoList(
                term = LmsApi.getTerms().first()
            ) {
                loadingProgress.value = it
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

            todos.apply {
                clear();
                addAll(newTodos)
//                add(newTodos.first().copy(due_date = "2026-05-01T14:59:59Z"))
//                add(newTodos.first().copy(due_date = "2026-05-02T14:59:59Z"))
//                add(newTodos.first().copy(due_date = "2026-05-03T14:59:59Z"))
//                add(newTodos.first().copy(due_date = "2026-05-04T14:59:59Z"))
            }

            submitted.apply {
                clear()
                addAll(newSubmitted)
//                add(newSubmitted.first().copy(todoId = 1))
//                add(newSubmitted.first().copy(todoId = 2))
//                add(newSubmitted.first().copy(todoId = 3))
//                add(newSubmitted.first().copy(todoId = 4))
//                add(newSubmitted.first().copy(todoId = 5))
//                add(newSubmitted.first().copy(todoId = 6))
//                add(newSubmitted.first().copy(todoId = 7))
//                add(newSubmitted.first().copy(todoId = 8))
//                add(newSubmitted.first().copy(todoId = 10))
//                add(newSubmitted.first().copy(todoId = 11))
//                add(newSubmitted.first().copy(todoId = 234))
            }

            isLoading.value = false

        } else {
            // TODO 재로그인 로직 필요
            Log.e(javaClass.name, "로그인이 되어있지 않아 정보를 불러올 수 없습니다")
            Log.e(javaClass.name, "앱 재실행을 요청하세요")
        }
    }

}