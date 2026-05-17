package com.yourssu.data.network

import kotlinx.serialization.Serializable

@Serializable
data class DeleteEnrollmentRequest(
    val id: Long
)
