package com.yourssu.data.network

import com.yourssu.data.SubjectInfo
import kotlinx.serialization.Serializable

@Serializable
data class AddEnrollmentRequest(
    val courseId: Long,
    val name: String,
    val semester: String
)

fun SubjectInfo.toAddEnrollmentRequest(semester: String): AddEnrollmentRequest =
    AddEnrollmentRequest(
        courseId = id.toLong(),
        name = name,
        semester = semester,
    )
