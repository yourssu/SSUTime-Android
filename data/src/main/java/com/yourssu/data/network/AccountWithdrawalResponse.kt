package com.yourssu.data.network

import kotlinx.serialization.Serializable

@Serializable
data class AccountWithdrawalResponse(
    val id: Long = 0L,
    val status: String = "",
    val requestedAt: String = "",
)
