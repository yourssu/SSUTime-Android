package com.yourssu.data

import kotlinx.serialization.Serializable

@Serializable
data class LoginData(
    val id: String,// 학번
    val pw: String, // 비번
    val isAutoLogin: Boolean = true, // 자동 로그인 설정 여부
    val accessToken: String,
) {
    val hasAutoLoginCredentials: Boolean
        get() = isAutoLogin && id.isNotBlank() && pw.isNotBlank()
}