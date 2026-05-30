package com.yourssu.ssutime.v2.screen.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssu.ssutime.v2.screen.main.LmsRefreshRepository
import com.yourssu.ssutime.v2.screen.main.RefreshSource
import com.yourssu.ssutime.v2.screen.main.TodoRefreshResult
import kotlinx.coroutines.launch

class OnBoardingViewModel(
    context: Context,
    private val onBoardingRepository: OnBoardingRepository,
    private val lmsRefreshRepository: LmsRefreshRepository,
) : ViewModel() {
    var isGranted = mutableStateOf(false)
    var isNotificationPermissionStepCompleted = mutableStateOf(false)
    var isTipConfirmed = mutableStateOf(false)
    var isOnBoardingDataLoaded = mutableStateOf(false)
    private var initialLmsRefreshStarted = false

    init {
        val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        if (permissionStatus == PackageManager.PERMISSION_DENIED) {
            isGranted.value = false
            isNotificationPermissionStepCompleted.value = false
            Log.e("PERMISSION", "알림 권한이 거부된 상태입니다.")
        } else {
            isGranted.value = true
            isNotificationPermissionStepCompleted.value = true
            Log.i("PERMISSION", "알림 권한이 허용된 상태입니다.")
        }

        viewModelScope.launch {
            val onBoardingData = onBoardingRepository.getOnBoardingData()
            isTipConfirmed.value = onBoardingData.isTipConfirmed
            isOnBoardingDataLoaded.value = true
        }
    }

    fun completeNotificationPermissionStep(granted: Boolean) {
        isGranted.value = granted
        isNotificationPermissionStepCompleted.value = true
    }

    suspend fun confirmTip() {
        val onBoardingData = OnBoardingData(
            isTipConfirmed = true
        )
        onBoardingRepository.updateOnBoardingData(onBoardingData)
        isTipConfirmed.value = true
    }

    fun startInitialLmsRefresh() {
        if (initialLmsRefreshStarted) {
            return
        }
        initialLmsRefreshStarted = true

        viewModelScope.launch {
            when (val result = lmsRefreshRepository.refreshTodos(source = RefreshSource.MANUAL)) {
                is TodoRefreshResult.Success -> {
                    Log.i(TAG, "온보딩 LMS 초기 새로고침이 완료되었습니다.")
                }

                is TodoRefreshResult.Failure -> {
                    Log.e(TAG, "온보딩 LMS 초기 새로고침에 실패했습니다: ${result.message}", result.throwable)
                }

                is TodoRefreshResult.Skipped -> {
                    Log.i(TAG, "온보딩 LMS 초기 새로고침을 건너뜁니다: ${result.reason}")
                }
            }
        }
    }
}

private const val TAG = "OnBoardingViewModel"
