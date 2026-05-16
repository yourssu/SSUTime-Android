package com.yourssu.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenRequest(
    @SerialName("id")
    val id: String,
    @SerialName("password")
    val password: String,
)
