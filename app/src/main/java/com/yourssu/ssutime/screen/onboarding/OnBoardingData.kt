package com.yourssu.ssutime.screen.onboarding

import kotlinx.serialization.Serializable

@Serializable
data class OnBoardingData(
    val isTipConfirmed: Boolean = false,
)
