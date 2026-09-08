package com.yourssu.data

import kotlinx.serialization.Serializable

@Serializable
data class CyberLoginData(
    val id: String = "",
    val pw: String = "",
    val isConnected: Boolean = false,
) {
    val hasCredentials: Boolean
        get() = isConnected && id.isNotBlank() && pw.isNotBlank()
}
