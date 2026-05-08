package com.yourssu.ssutime.screen.my

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.yourssu.ssutime.LMS_REFRESH_TOPIC
import com.yourssu.ssutime.screen.login.LoginRepository
import io.github.chlwhdtn03.LmsApi
import io.github.chlwhdtn03.data.Info
import kotlinx.coroutines.launch

class MyViewModel(
    private val loginRepository: LoginRepository
) : ViewModel() {
    var loginInfo = mutableStateOf<Info?>(null)

    var isLogout = mutableStateOf(false)

    init {
        viewModelScope.launch {
            if(LmsApi.isLoggined)
                loginInfo.value = LmsApi.getLoginInfo()
        }
    }

    fun logout() {
        viewModelScope.launch {
            loginRepository.logout()
            FirebaseMessaging.getInstance().unsubscribeFromTopic(LMS_REFRESH_TOPIC)
            isLogout.value = true
        }
    }
}
