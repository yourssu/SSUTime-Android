package com.yourssu.ssutime.desktop.core.network

import com.yourssu.data.network.AddEnrollmentRequest
import com.yourssu.data.network.LmsSessionCookieRequest
import com.yourssu.data.network.LmsSessionRequest
import com.yourssu.data.network.ReportedTodoResponse
import com.yourssu.data.network.UserTodoStatusResponse
import com.yourssu.data.network.matches
import com.yourssu.data.network.toReportWithAnalysisRequestOrNull
import com.yourssu.data.network.toTodoReportRequest
import com.yourssu.ssutime.desktop.core.model.AiSummary
import com.yourssu.ssutime.desktop.core.model.AppSubject
import com.yourssu.ssutime.desktop.core.model.AppTodo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

private const val API_BASE_URL = "https://ssutimev2-api-dev.yourssu.com"

typealias LmsCookie = LmsSessionCookieRequest

class SsuTimeApi(
    private val client: HttpClient,
) {
    suspend fun addEnrollment(
        accessToken: String,
        subject: AppSubject,
        semester: String,
    ): HttpStatusCode = client.post("$API_BASE_URL/enrollments") {
        bearerAuth(accessToken)
        contentType(ContentType.Application.Json)
        setBody(
            AddEnrollmentRequest(
                courseId = subject.id.toLong(),
                name = subject.name,
                semester = semester,
            ),
        )
    }.status

    suspend fun reportTodo(
        accessToken: String,
        todo: AppTodo,
    ): HttpStatusCode = client.post("$API_BASE_URL/todo/report") {
        bearerAuth(accessToken)
        contentType(ContentType.Application.Json)
        setBody(todo.toTodoReportRequest())
    }.status

    suspend fun reportTodoWithAnalysis(
        accessToken: String,
        todo: AppTodo,
        cookies: List<LmsCookie>,
    ) {
        val request = requireNotNull(
            todo.toReportWithAnalysisRequestOrNull(
                lmsSession = LmsSessionRequest(cookies),
            ),
        ) {
            "AI 요약을 요청할 수 없는 할 일이에요."
        }
        client.post("$API_BASE_URL/todo/report-with-analysis") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun findAiSummary(
        accessToken: String,
        todo: AppTodo,
    ): ReportedTodoResponse? = client.get("$API_BASE_URL/todo/todos") {
        bearerAuth(accessToken)
        contentType(ContentType.Application.Json)
    }.body<List<UserTodoStatusResponse>>()
        .firstOrNull { response -> response.todo.matches(todo) }
        ?.todo
}

fun ReportedTodoResponse.toConfirmedAiSummaryOrNull(): AiSummary? {
    val text = aiSummary.orEmpty().trim()
    if (!status.equals("CONFIRMED", ignoreCase = true) || text.isEmpty()) {
        return null
    }
    return AiSummary(
        summary = text,
        estimatedDurationMinutes = estimatedDurationMinutes,
    )
}
