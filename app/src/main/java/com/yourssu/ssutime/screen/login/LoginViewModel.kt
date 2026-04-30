package com.yourssu.ssutime.screen.login

import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssu.data.LoginData
import io.github.chlwhdtn03.loginLMS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel(
    private val loginRepository: LoginRepository
) : ViewModel() {
    val idState = TextFieldState()
    val pwState = TextFieldState()
    val autoLoginState = mutableStateOf(false)
    val isAutoLogined = mutableStateOf(false)
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
            autoLoginState.value = info.isAutoLogin

            if(autoLoginState.value && idState.text.isNotEmpty() && pwState.text.isNotEmpty()) {
                if(login()) {
                    isAutoLogined.value = true
                }
            }
        }
    }

    suspend fun login(): Boolean {
        isLoading.value = true
        var errorMessage = ""
        val isLogined = withContext(Dispatchers.IO) {
            // 무거운 작업 + 네트워크 작업은 I/O쓰레드에서 따로 실행
            return@withContext try {
                val id = idState.text.toString()
                val pw = pwState.text.toString()
                Log.d(javaClass.name, "id : ${idState.text} || pw : ${pwState.text}")

                loginLMS(id, pw)
            } catch (e: Exception) {
                errorMessage = e.message ?: ""
                false
            }
        }
        isLoading.value = false
        // 다시 메인쓰레드에서 나머지 작업 처리
        processLogin(isLogined, errorMessage)
        return isLogined
    }

    fun processLogin(logined: Boolean, errorMessage: String = "") {
        this.errorMessage.value = errorMessage
        if (logined) {
            viewModelScope.launch {
                loginRepository.updateLoginData(
                    LoginData(
                        id = if (autoLoginState.value) idState.text.toString() else "",
                        pw = if (autoLoginState.value) pwState.text.toString() else "",
                        isAutoLogin = autoLoginState.value
                    )
                )
            }
        }
    }
}