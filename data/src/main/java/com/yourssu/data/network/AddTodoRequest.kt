package com.yourssu.data.network

import com.yourssu.data.TodoInfo
import kotlinx.serialization.Serializable

@Serializable
data class AddTodoRequest(
    val subjectId: Long,
    val materialCode: Long,
    val type: String,
    val dueDate: String,
    val title: String,
)



fun TodoInfo.toAddTodoRequest(): AddTodoRequest
    = AddTodoRequest(
        subjectId = (this.subject?.id ?: this.subjectId).toLong(),
        materialCode = this.todoId.toLong(),
        type = this.type.name,
        dueDate = this.due_date,
        title = this.title
    )
