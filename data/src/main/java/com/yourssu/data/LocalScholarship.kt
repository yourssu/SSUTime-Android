package com.yourssu.data

import kotlinx.serialization.Serializable

@Serializable
data class LocalScholarship(
    val items: List<LocalScholarshipCell> = emptyList()
)

@Serializable
data class LocalScholarshipCell(
    val year: String = "",
    val semester: String = "",
    val scholarshipName: String = "",
    val paymentMethod: String = "",
    val processStatus: String = "",
    val note: String = "",
    val dropReason: String = "",
    val processDate: String = "",
    val selectedAmount: String = "",
    val actualAmount: String = "",
    val redeemedAmount: String = "",
    val replacedAmount: String = "",
    val replacedScholarshipName: String = "",
    val workDepartment: String = ""
)
