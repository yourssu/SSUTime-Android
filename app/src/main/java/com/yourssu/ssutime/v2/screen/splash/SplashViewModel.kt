package com.yourssu.ssutime.v2.screen.splash

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssu.data.LoginData
import com.yourssu.ssutime.v2.accessToken
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter
import com.yourssu.ssutime.v2.network.ApiRepository
import com.yourssu.ssutime.v2.screen.login.LoginRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashViewModel(
    private val loginRepository: LoginRepository,
    private val apiRepository: ApiRepository,
) : ViewModel() {
    val destination = mutableStateOf<SplashDestination?>(null)

    init {
        viewModelScope.launch {
            val loginData = loginRepository.getLoginData()
            destination.value = if (loginData.hasAutoLoginCredentials) {
                refreshAccessToken(loginData)
                SplashDestination.Main
            } else {
                SplashDestination.Login
            }
        }
    }

    private suspend fun refreshAccessToken(loginData: LoginData) {
        withContext(Dispatchers.IO) {
            try {
                val refreshedToken = apiRepository.requestJwtToken(loginData.id, loginData.pw).accessToken
                if (refreshedToken.isBlank()) {
                    accessToken = loginData.accessToken
                    Log.w(TAG, "Backend access token refresh returned a blank token.")
                    return@withContext
                }

                accessToken = refreshedToken
                loginRepository.updateLoginData(loginData.copy(accessToken = refreshedToken))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                accessToken = loginData.accessToken
                SentryExceptionReporter.capture(e)
                Log.e(TAG, "Failed to refresh backend access token on app start.", e)
            }
        }
    }
}

enum class SplashDestination {
    Login,
    Main,
}

private const val TAG = "SplashViewModel"
