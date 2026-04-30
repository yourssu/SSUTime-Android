package com.yourssu.ssutime.screen.main

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import io.github.chlwhdtn03.getTerms
import io.github.chlwhdtn03.getTodoList
import io.github.chlwhdtn03.isLoggined
import kotlin.time.ExperimentalTime

class MainViewModel(

) : ViewModel() {
    var todos = mutableStateListOf<TodoInfo>()
    var isLoading = mutableStateOf(false)

    @OptIn(ExperimentalTime::class)
    suspend fun loadTodos() {
        if(isLoggined) {
            isLoading.value = true
            val todoList = getTodoList(
                term = getTerms().first()
            )

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

            todos.apply {
                clear();
                addAll(newTodos)
            }
            isLoading.value = false

        } else {
            // TODO 재로그인 로직 필요
            Log.e(javaClass.name, "로그인이 되어있지 않아 정보를 불러올 수 없습니다")
            Log.e(javaClass.name, "앱 재실행을 요청하세요")
        }
    }

}