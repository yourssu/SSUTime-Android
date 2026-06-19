package com.yourssu.data

import kotlinx.serialization.Serializable

@Serializable
data class LocalGraduate(
    val items: List<LocalGraduateCell> = emptyList()
)

@Serializable
data class LocalGraduateCell(
    val classification: String = "",
    val requirement: String = "",
    val standardValue: String = "",
    val calculatedValue: String = "",
    val difference: String = "",
    val result: String = ""
)
