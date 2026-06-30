package com.yourssu.data

import kotlinx.serialization.Serializable

@Serializable
data class LocalTimetable(
    val year: String = "",
    val semester: String = "",
    val items: List<LocalTimetableCell> = emptyList()
)

@Serializable
data class LocalTimetableCell(
    val dayOfWeek: String = "", // LmsDayOfWeek.name
    val period: String = "",
    val periodTime: String = "",
    val subject: String = "",
    val professor: String = "",
    val time: String = "",
    val classroom: String = ""
)
