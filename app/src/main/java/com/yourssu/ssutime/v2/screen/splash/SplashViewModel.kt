package com.yourssu.ssutime.v2.screen.splash

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssu.data.LoginData
import com.yourssu.ssutime.v2.screen.login.LoginRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashViewModel(
    private val loginRepository: LoginRepository,
) : ViewModel() {
    val destination = mutableStateOf<SplashDestination?>(null)

    init {
        viewModelScope.launch {
            delay(SPLASH_DELAY_MILLIS)
            val loginData = loginRepository.getLoginData()
            destination.value = if (loginData.hasAutoLoginCredentials) {
                SplashDestination.Main
            } else {
                SplashDestination.Login
            }
        }
    }
}

enum class SplashDestination {
    Login,
    Main,
}

private val LoginData.hasAutoLoginCredentials: Boolean
    get() = isAutoLogin && id.isNotBlank() && pw.isNotBlank()

private const val SPLASH_DELAY_MILLIS = 100L
