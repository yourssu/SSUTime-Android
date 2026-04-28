package com.yourssu.ssutime.screen.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel

class OnBoardingViewModel(context: Context) : ViewModel() {
    var isGranted = mutableStateOf(false)

    init {
        val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        if (permissionStatus == PackageManager.PERMISSION_DENIED) {
            isGranted.value = false
            Log.e("PERMISSION", "알림 권한이 거부된 상태입니다.")
        } else {
            isGranted.value = true
            Log.e("PERMISSION", "알림 권한이 허용된 상태입니다.")
        }
    }
}