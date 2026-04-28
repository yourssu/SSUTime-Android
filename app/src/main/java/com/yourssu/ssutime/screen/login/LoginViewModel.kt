package com.yourssu.ssutime.screen.login

import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.github.chlwhdtn03.loginLMS

class LoginViewModel : ViewModel() {
    val idState = TextFieldState()
    val pwState = TextFieldState()
    val autoLoginState = mutableStateOf(false)
    var errorMessage = mutableStateOf("")
    suspend fun login(): Boolean {
        val id = idState.text.toString()
        val pw = pwState.text.toString()
        Log.d(javaClass.name, "id : ${idState.text} || pw : ${pwState.text}")

        return loginLMS(id, pw)
    }

    fun processLogin(logined: Boolean, errorMessage: String = "") {
        if(!logined) {
            this.errorMessage.value = errorMessage
            return
        }
        this.errorMessage.value = "로그인 성공!"
    }
}