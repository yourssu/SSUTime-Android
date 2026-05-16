package com.yourssu.data

import kotlinx.serialization.Serializable

@Serializable
data class AlertData(
    var valid: Boolean = false,
    val allowSystemAlert: Boolean,
    val allowCallAlert: Boolean,
    val callingAlertThresholdMinutes: Long
)