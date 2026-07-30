package com.yourssu.ssutime.desktop.core.network

import com.yourssu.data.network.TokenRequest
import com.yourssu.data.network.TokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

private const val TOKEN_URL = "https://ssutimev2-api-dev.yourssu.com/auth/tokens"

fun interface AuthTokenProvider {
    suspend fun requestAccessToken(id: String, password: String): String
}

class SsuTimeAuthApi(
    private val client: HttpClient,
) : AuthTokenProvider {
    override suspend fun requestAccessToken(id: String, password: String): String {
        val token = client.post(TOKEN_URL) {
            contentType(ContentType.Application.Json)
            setBody(TokenRequest(id = id, password = password))
        }.body<TokenResponse>().accessToken

        check(token.isNotBlank()) {
            "SSUTime 인증 토큰을 발급받지 못했어요."
        }
        return token
    }
}
