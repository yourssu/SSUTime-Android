package com.yourssu.data.network

import kotlinx.serialization.Serializable

@Serializable
data class NotificationSetting(
    val notificationEnabled: Boolean = false,
    val notificationThresholdMinutes: Long = -1
)
