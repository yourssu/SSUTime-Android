package com.yourssu.ssutime.v2.analytics

import android.content.Context
import android.util.Log
import com.yourssu.data.LoginData
import com.yourssu.ssutime.v2.loginDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val USER_IDENTITY_TAG = "UserIdentityAnalytics"

fun Context.identifyStoredAnalyticsUserIfNeeded() {
    val appContext = applicationContext

    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        runCatching {
            val loginData = appContext.loginDataStore.data.first()
            if (loginData.hasAutoLoginCredentials) {
                Analytics.identifyUser(loginData.id)
            }
        }.onFailure { exception ->
            Log.w(USER_IDENTITY_TAG, "Failed to identify stored analytics user.", exception)
        }
    }
}

private val LoginData.hasAutoLoginCredentials: Boolean
    get() = isAutoLogin && id.isNotBlank() && pw.isNotBlank()
