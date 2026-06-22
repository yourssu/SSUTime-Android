package com.yourssu.data

import kotlinx.serialization.Serializable

@Serializable
data class LocalChapel(
    val details: Map<String, LocalChapelTable> = emptyMap(),
    val thisSemesterYear: String? = null,
    val thisSemesterType: String? = null
)

@Serializable
data class LocalChapelTable(
    val year: String = "",
    val semesterName: String = "",
    val semesterNameKor: String = "",
    val seatStatus: List<LocalChapelSeatStatusCell> = emptyList(),
    val attendance: List<LocalChapelAttendanceCell> = emptyList(),
    val absence: List<LocalChapelAbsenceCell> = emptyList()
)

@Serializable
data class LocalChapelSeatStatusCell(
    val classGroup: String = "",
    val timetable: String = "",
    val classroom: String = "",
    val seatNo: String = "",
    val absenceCount: String = "",
    val gradeResult: String = "",
    val rawValues: Map<String, String> = emptyMap()
)

@Serializable
data class LocalChapelAttendanceCell(
    val classGroup: String = "",
    val date: String = "",
    val lectureType: String = "",
    val status: String = "",
    val rawValues: Map<String, String> = emptyMap()
)

@Serializable
data class LocalChapelAbsenceCell(
    val year: String = "",
    val semester: String = "",
    val detail: String = "",
    val rawValues: Map<String, String> = emptyMap()
)
