package com.yourssu.data.network

import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import kotlinx.serialization.Serializable

@Serializable
data class TodoReportWithAnalysisRequest(
    val subjectId: Long,
    val materialCode: Long,
    val type: String,
    val dueDate: String,
    val title: String,
    val assignmentAnalysis: AssignmentAnalysisPayload,
)

@Serializable
data class AssignmentAnalysisPayload(
    val courseId: Long,
    val assignmentId: Long,
    val assignmentHtml: String,
    val lmsSession: LmsSessionRequest,
)

@Serializable
data class LmsSessionRequest(
    val cookies: List<LmsSessionCookieRequest> = emptyList(),
)

@Serializable
data class LmsSessionCookieRequest(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
)

@Serializable
data class AssignmentAnalysisResponse(
    val analysisId: Long,
    val status: String,
    val skippedFiles: List<String> = emptyList(),
)

fun TodoInfo.toReportWithAnalysisRequestOrNull(
    lmsSession: LmsSessionRequest,
): TodoReportWithAnalysisRequest? {
    val subjectId = (subject?.id ?: subjectId).toLong()
    val assignmentId = todoId.toLong()
    val assignmentHtml = description.takeIf { it.isNotBlank() } ?: return null
    if (type != TodoType.ASSIGNMENT || subjectId <= 0L || assignmentId <= 0L) {
        return null
    }

    return TodoReportWithAnalysisRequest(
        subjectId = subjectId,
        materialCode = assignmentId,
        type = type.name,
        dueDate = due_date,
        title = title,
        assignmentAnalysis = AssignmentAnalysisPayload(
            courseId = subjectId,
            assignmentId = assignmentId,
            assignmentHtml = assignmentHtml,
            lmsSession = lmsSession,
        ),
    )
}
