package com.yourssu.ssutime.v2.screen.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class OnBoardingViewModel(
    context: Context,
    private val onBoardingRepository: OnBoardingRepository
) : ViewModel() {
    var isGranted = mutableStateOf(false)
    var isTipConfirmed = mutableStateOf(false)
    var isOnBoardingDataLoaded = mutableStateOf(false)

    init {
        val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        if (permissionStatus == PackageManager.PERMISSION_DENIED) {
            isGranted.value = false
            Log.e("PERMISSION", "알림 권한이 거부된 상태입니다.")
        } else {
            isGranted.value = true
            Log.i("PERMISSION", "알림 권한이 허용된 상태입니다.")
        }

        viewModelScope.launch {
            val onBoardingData = onBoardingRepository.getOnBoardingData()
            isTipConfirmed.value = onBoardingData.isTipConfirmed
            isOnBoardingDataLoaded.value = true
        }
    }

    suspend fun confirmTip() {
        val onBoardingData = OnBoardingData(
            isTipConfirmed = true
        )
        onBoardingRepository.updateOnBoardingData(onBoardingData)
        isTipConfirmed.value = true
    }
}
