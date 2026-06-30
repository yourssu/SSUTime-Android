package com.yourssu.data

import kotlinx.serialization.Serializable

@Serializable
data class LocalGrade(
    val summaryItems: List<LocalGradeSummaryCell> = emptyList(),
    val details: Map<String, LocalGradeTable> = emptyMap(),
    val thisSemesterYear: String? = null,
    val thisSemesterType: String? = null
)

@Serializable
data class LocalGradeSummaryCell(
    val year: String = "",
    val semesterName: String = "",
    val semesterNameKor: String = "",
    val gpa: String = "",
    val earnedCredits: String = "",
    val semesterRank: String = "",
    val academicWarning: String = "",
    val attemptedCredits: String = "",
    val pfCredits: String = "",
    val gpaSum: String = "",
    val arithmeticMean: String = "",
    val totalRank: String = "",
    val consultationStatus: String = "",
    val failedYearStatus: String = ""
)

@Serializable
data class LocalGradeTable(
    val year: String = "",
    val semesterName: String = "",
    val semesterNameKor: String = "",
    val items: List<LocalGradeCell> = emptyList()
)

@Serializable
data class LocalGradeCell(
    val subjectName: String = "",
    val professor: String = "",
    val credits: String = "",
    val gradePoint: String = "",
    val grade: String = "",
    val subjectCode: String = "",
    val classification: String = ""
)
