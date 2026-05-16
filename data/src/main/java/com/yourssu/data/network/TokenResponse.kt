package com.yourssu.data.network

import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    val accessToken: String = ""
)
