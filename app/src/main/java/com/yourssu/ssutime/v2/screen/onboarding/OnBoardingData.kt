package com.yourssu.ssutime.v2.screen.onboarding

import kotlinx.serialization.Serializable

@Serializable
data class OnBoardingData(
    val isTipConfirmed: Boolean = false,
)
