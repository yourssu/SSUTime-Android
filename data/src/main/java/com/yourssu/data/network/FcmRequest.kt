package com.yourssu.data.network

import kotlinx.serialization.Serializable

@Serializable
data class FcmRequest(
    val fcmToken: String? = ""
)
