package com.yourssu.ssutime.v2.analytics

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

private const val TAG = "InstallReferrer"
private const val INSTALL_REFERRER_FETCH_TIMEOUT_MILLIS = 2_000L

private val Context.installReferrerAnalyticsDataStore by preferencesDataStore(
    name = "install_referrer_analytics",
)

private val APP_STORE_INSTALLED_SENT_KEY = booleanPreferencesKey(
    name = "app_store_installed_sent",
)

fun Context.captureInstallReferrerIfNeeded() {
    val appContext = applicationContext

    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            val alreadySent = appContext.installReferrerAnalyticsDataStore.data
                .first()[APP_STORE_INSTALLED_SENT_KEY] == true
            if (alreadySent) return@launch

            val referrer = runCatching {
                withTimeoutOrNull(INSTALL_REFERRER_FETCH_TIMEOUT_MILLIS) {
                    appContext.fetchInstallReferrer()
                }
            }.onFailure { exception ->
                Log.w(TAG, "Failed to fetch install referrer.", exception)
            }.getOrNull()
            Analytics.appStoreInstalled(referrer?.extractUtmProperties().orEmpty())
            appContext.installReferrerAnalyticsDataStore.edit { preferences ->
                preferences[APP_STORE_INSTALLED_SENT_KEY] = true
            }
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to capture install referrer analytics.", exception)
        } finally {
            Analytics.markInstallAttributionHandled()
        }
    }
}

private suspend fun Context.fetchInstallReferrer(): String? = suspendCancellableCoroutine { continuation ->
    val referrerClient = InstallReferrerClient.newBuilder(this).build()
    val completed = AtomicBoolean(false)

    fun finish(referrer: String?) {
        if (!completed.compareAndSet(false, true)) return

        if (continuation.isActive) {
            continuation.resume(referrer)
        }

        runCatching {
            referrerClient.endConnection()
        }.onFailure { exception ->
            Log.w(TAG, "Failed to end install referrer connection.", exception)
        }
    }

    continuation.invokeOnCancellation {
        finish(null)
    }

    runCatching {
        referrerClient.startConnection(
            object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    when (responseCode) {
                        InstallReferrerResponse.OK -> {
                            val referrer = runCatching {
                                referrerClient.installReferrer.installReferrer
                            }.onFailure { exception ->
                                Log.w(TAG, "Failed to read install referrer.", exception)
                            }.getOrNull()
                            finish(referrer)
                        }

                        InstallReferrerResponse.FEATURE_NOT_SUPPORTED,
                        InstallReferrerResponse.SERVICE_UNAVAILABLE,
                        InstallReferrerResponse.DEVELOPER_ERROR,
                        -> finish(null)
                    }
                }

                override fun onInstallReferrerServiceDisconnected() {
                    Log.i(TAG, "Install referrer service disconnected.")
                    finish(null)
                }
            },
        )
    }.onFailure { exception ->
        Log.w(TAG, "Failed to start install referrer connection.", exception)
        finish(null)
    }
}

private fun String.extractUtmProperties(): Map<String, String> {
    return parseUtmProperties() ?: decodeReferrer().parseUtmProperties().orEmpty()
}

private fun String.parseUtmProperties(): Map<String, String>? = runCatching {
    val uri = if (contains("://")) {
        Uri.parse(this)
    } else {
        Uri.parse("https://ssutime.local/?$this")
    }

    val utmProperties = uri.queryParameterNames
        .filter { key -> key.startsWith("utm_") }
        .associateWith { key -> uri.getQueryParameter(key).orEmpty() }

    utmProperties.ifEmpty { null }
}.getOrNull()

private fun String.decodeReferrer(): String =
    runCatching {
        URLDecoder.decode(this, StandardCharsets.UTF_8.name())
    }.getOrDefault(this)
