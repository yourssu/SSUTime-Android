package com.yourssu.ssutime.screen.login

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {
    val idState = TextFieldState()
    val pwState = TextFieldState()
    val autoLoginState = mutableStateOf(false)

    fun login(): Boolean {
        return false
    }
}