package com.yourssu.ssutime.v2.network

import android.util.Log
import com.yourssu.data.network.FcmRequest
import com.yourssu.data.network.NotificationSetting
import com.yourssu.data.network.TokenRequest
import com.yourssu.data.network.TokenResponse
import com.yourssu.ssutime.v2.accessToken
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
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

    suspend fun registerFCMToken(fcmRequest: FcmRequest) {
        val response = client.post(
            urlString = "https://ssutimev2-api-dev.yourssu.com/auth/devices"
        ) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(fcmRequest)
        }.apply {
            Log.i("ApiRepository", accessToken)
        }
        Log.i("ApiRepository", response.headers.toString())
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


}