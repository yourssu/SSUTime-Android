package com.yourssu.ssutime.v2.screen.login

import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssu.data.LoginData
import com.yourssu.data.network.FcmRequest
import com.yourssu.ssutime.v2.accessToken
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter
import com.yourssu.ssutime.v2.lms.loginLms
import com.yourssu.ssutime.v2.network.ApiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel(
    private val loginRepository: LoginRepository,
    private val apiRepository: ApiRepository
) : ViewModel() {
    val idState = TextFieldState()
    val pwState = TextFieldState()
    val autoLoginState = mutableStateOf(true)
    var errorMessage = mutableStateOf("")
    var isLoading = mutableStateOf(false)

    init {
        viewModelScope.launch {
            val info = loginRepository.getLoginData()
            idState.edit {
                append(info.id)
            }
            pwState.edit {
                append(info.pw)
            }
            autoLoginState.value = true // 초기 자동로그인 체크상태
        }
    }

    suspend fun login(): Boolean {
        isLoading.value = true
        var loginErrorMessage = ""
        val id = idState.text.toString()
        val pw = pwState.text.toString()
        val isLoggined = withContext(Dispatchers.IO) {
            // 무거운 작업 + 네트워크 작업은 I/O쓰레드에서 따로 실행
            return@withContext try {
                Log.d(javaClass.name, "id : ${idState.text}")

                loginLms(id, pw).apply {
                    if(this)
                        accessToken = apiRepository.requestJwtToken(id, pw).accessToken
                }
            } catch (e: Exception) {
                SentryExceptionReporter.capture(e)
                loginErrorMessage = e.message ?: ""
                false
            }
        }
        isLoading.value = false
        errorMessage.value = loginErrorMessage
        if (isLoggined) {
            loginRepository.updateLoginData(
                LoginData(
                    id = if (autoLoginState.value) id else "",
                    pw = if (autoLoginState.value) pw else "",
                    isAutoLogin = autoLoginState.value,
                    accessToken = accessToken,
                )
            )
        }
        return isLoggined
    }

    suspend fun registerFCMToken(fcm: String) {
        apiRepository.registerFCMToken(FcmRequest(fcm))
    }
}
