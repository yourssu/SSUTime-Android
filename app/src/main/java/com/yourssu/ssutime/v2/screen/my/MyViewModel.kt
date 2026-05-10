package com.yourssu.ssutime.v2.screen.my

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import io.github.chlwhdtn03.LmsApi
import io.github.chlwhdtn03.data.Info
import kotlinx.coroutines.launch

class MyViewModel(
    private val loginRepository: com.yourssu.ssutime.v2.screen.login.LoginRepository
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
            FirebaseMessaging.getInstance().unsubscribeFromTopic(_root_ide_package_.com.yourssu.ssutime.v2.LMS_REFRESH_TOPIC)
            isLogout.value = true
        }
    }
}
