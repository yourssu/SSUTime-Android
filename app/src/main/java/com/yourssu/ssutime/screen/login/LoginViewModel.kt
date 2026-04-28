package com.yourssu.ssutime.screen.login

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.yourssu.data.LoginData
import org.koin.core.annotation.KoinViewModel

class LoginViewModel : ViewModel() {
    val idState = TextFieldState()
    val pwState = TextFieldState()
    val autoLoginState = mutableStateOf(false)

    fun login(credential: LoginData): Boolean {

        return false
    }
}