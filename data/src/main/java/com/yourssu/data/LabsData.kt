package com.yourssu.data

import kotlinx.serialization.Serializable

@Serializable
data class LabsData(
    val isEnableSubmittedFile: Boolean = false
)
