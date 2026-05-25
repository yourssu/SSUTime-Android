package com.yourssu.ssutime.v2.screen.my

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssu.data.AlertData
import com.yourssu.data.UiState
import com.yourssu.ssutime.v2.accessToken
import com.yourssu.ssutime.v2.lms.getLmsLoginInfo
import com.yourssu.ssutime.v2.screen.login.LoginRepository
import com.yourssu.ssutime.v2.screen.main.MainRepository
import io.github.chlwhdtn03.LmsApi
import io.github.chlwhdtn03.data.Lms.Info
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyViewModel(
    private val loginRepository: LoginRepository,
    private val mainRepository: MainRepository,
) : ViewModel() {
    var loginInfo = mutableStateOf<Info?>(null)
    var isLogout = mutableStateOf(false)

    private val _uiState = MutableStateFlow<UiState<AlertData>>(UiState.Loading)
    val uiState: StateFlow<UiState<AlertData>> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = UiState.Success(
                mainRepository.getAlertData()
            )
            if(LmsApi.isLoggined)
                loginInfo.value = getLmsLoginInfo()
        }
    }

    fun updateAlertData(alertData: AlertData) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            mainRepository.updateAlertData(
                alertData
            )
            _uiState.value = UiState.Success(mainRepository.getAlertData())
        }
    }

    fun logout() {
        viewModelScope.launch {
            mainRepository.clearTodoData()
            loginRepository.logout()
            accessToken = ""
            isLogout.value = true
        }
    }
}
