package com.yourssu.data

import kotlinx.serialization.Serializable

@Serializable
data class TodoInfo(
    val todoId: Int,
    val title: String, // 제목
    val due_date: String, // 마감 기한
    val type: TodoType, // 제목
    val subject: SubjectInfo?, // 과목 정보
    val submittedAt: String = "", // 제출 시각
    val description: String = "", // 과제 설명
    val aiSummary: String = "", // AI 요약
) {
    var subjectId = 0
}

@Serializable
enum class TodoType(
    val kor: String,
) {
    COMMONS("강의"), ASSIGNMENT("과제"), QUIZ("퀴즈"), SUBMITTED("정상 제출"), SUBMITTED_LATE("지각 제출")
}
