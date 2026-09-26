package com.yourssu.data.network

import kotlinx.serialization.Serializable

@Serializable
data class AccountWithdrawalRequest(
    val reason: String = "string",
)
