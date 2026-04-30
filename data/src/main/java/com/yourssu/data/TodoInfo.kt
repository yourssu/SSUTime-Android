package com.yourssu.data

data class TodoInfo(
    val todoId: Int,
    val title: String, // 제목
    val due_date: String, // 마감 기한
    val type: TodoType, // 제목
    val subject: SubjectInfo?, // 과목 정보
) {
    var subjectId = 0
}

enum class TodoType(
    val kor: String,
) {
    COMMONS("강의"), ASSIGNMENT("과제")
}