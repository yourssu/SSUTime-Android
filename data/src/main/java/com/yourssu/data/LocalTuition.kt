package com.yourssu.data

import kotlinx.serialization.Serializable

@Serializable
data class LocalTuition(
    val items: List<LocalTuitionCell> = emptyList()
)

@Serializable
data class LocalTuitionCell(
    val year: String = "",
    val semester: String = "",
    val grade: String = "",
    val registrationType: String = "",
    val registrationDate: String = "",
    val amount: String = "",
    val reduction: String = "",
    val paymentAmount: String = ""
)
