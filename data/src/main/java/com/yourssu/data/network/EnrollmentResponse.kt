package com.yourssu.data.network

data class EnrollmentResponse(
    val enrollmentId: Long,
    val courseId: Long,
    val name: String,
    val semester: String
)
