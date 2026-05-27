package com.yourssu.ssutime.v2.network

import android.util.Log
import com.yourssu.data.network.AddEnrollmentRequest
import com.yourssu.data.network.AssignmentAnalysisResponse
import com.yourssu.data.network.DeleteEnrollmentRequest
import com.yourssu.data.network.EnrollmentResponse
import com.yourssu.data.network.FcmRequest
import com.yourssu.data.network.NotificationSetting
import com.yourssu.data.network.TodoReportRequest
import com.yourssu.data.network.TodoReportWithAnalysisRequest
import com.yourssu.data.network.TokenRequest
import com.yourssu.data.network.TokenResponse
import com.yourssu.data.network.UserTodoStatusResponse
import com.yourssu.ssutime.v2.accessToken
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ApiRepository {
    val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun requestJwtToken(id: String, pw: String): TokenResponse {
        val response = client.post(
            urlString = "https://ssutimev2-api-dev.yourssu.com/auth/tokens"
        ) {
            contentType(ContentType.Application.Json)
            setBody(TokenRequest(id, pw))
        }.apply {
            Log.i("ApiRepository", bodyAsText())
        }
        return response.body()
    }

    suspend fun registerFCMToken(fcmRequest: FcmRequest): HttpStatusCode {
        val response = client.post(
            urlString = "https://ssutimev2-api-dev.yourssu.com/auth/devices"
        ) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(fcmRequest)
        }
        Log.i("ApiRepository", "FCM token registered: ${response.status}")
        return response.status
    }

    suspend fun setNotificationSetting(enabled: Boolean, minutes: Long): NotificationSetting {
        val response = client.put(
            urlString = "https://ssutimev2-api-dev.yourssu.com/auth/notification-settings"
        ) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(NotificationSetting(enabled, minutes))
        }.apply {
            Log.i("ApiRepository", bodyAsText())
        }

        return response.body()
    }

    suspend fun addEnrollment(enrollment: AddEnrollmentRequest): HttpStatusCode {
        val response = client.post(
            urlString = "https://ssutimev2-api-dev.yourssu.com/enrollments"
        ) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(enrollment)
        }

        Log.i("ApiRepository", enrollment.name + " Add Enrollment Status Code : ${response.status.value}")
        return response.status
    }

    suspend fun deleteEnrollment(enrollment: DeleteEnrollmentRequest): HttpStatusCode {
        val response = client.delete(
            urlString = "https://ssutimev2-api-dev.yourssu.com/enrollments"
        ) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(enrollment)
        }
        Log.i("ApiRepository", enrollment.id.toString() + " Delete Enrollment Status Code : ${response.status.value}")
        return response.status
    }

    suspend fun getEnrollments(enrollment: AddEnrollmentRequest): List<EnrollmentResponse> {
        val response = client.get(
            urlString = "https://ssutimev2-api-dev.yourssu.com/enrollments"
        ) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(enrollment)
        }.apply {
            Log.i("ApiRepository", bodyAsText())
        }
        return response.body()
    }

    suspend fun reportTodo(todo: TodoReportRequest): HttpStatusCode {
        val response = client.post(
            urlString = "https://ssutimev2-api-dev.yourssu.com/todo/report"
        ) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(todo)
        }
        Log.i(
            "ApiRepository",
            todo.title + " Todo Report Request Status Code : ${response.status.value}",
        )
        return response.status
    }

    suspend fun getTodos(): List<UserTodoStatusResponse> {
        val response = client.get(
            urlString = "https://ssutimev2-api-dev.yourssu.com/todo/todos"
        ) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
        }
        Log.i("ApiRepository", "Get Todos Status Code : ${response.status.value}")
        return response.body()
    }

    suspend fun reportTodoWithAnalysis(todo: TodoReportWithAnalysisRequest): AssignmentAnalysisResponse {
        val response = client.post(
            urlString = "https://ssutimev2-api-dev.yourssu.com/todo/report-with-analysis"
        ) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(todo)
        }
        Log.i("ApiRepository", todo.title + " Analysis Reqeust Status Code : ${response.status.value}")
        Json.encodeToString(todo).chunked(3000).forEachIndexed { index, chunk ->
            Log.d("AI_REQUEST", "$chunk")
        }
        Log.i("ApiRepository RES", response.bodyAsText())
        return response.body()
    }
}
