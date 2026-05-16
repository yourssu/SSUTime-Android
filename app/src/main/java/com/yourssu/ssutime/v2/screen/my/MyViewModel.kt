package com.yourssu.ssutime.v2.screen.my

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.yourssu.ssutime.v2.LMS_REFRESH_TOPIC
import com.yourssu.ssutime.v2.screen.login.LoginRepository
import com.yourssu.ssutime.v2.screen.main.MainRepository
import io.github.chlwhdtn03.LmsApi
import io.github.chlwhdtn03.data.Lms.Info
import kotlinx.coroutines.launch

class MyViewModel(
    private val loginRepository: LoginRepository,
    private val mainRepository: MainRepository,
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
            mainRepository.clearTodoData()
            loginRepository.logout()
            FirebaseMessaging.getInstance().unsubscribeFromTopic(LMS_REFRESH_TOPIC)
            isLogout.value = true
        }
    }
}
