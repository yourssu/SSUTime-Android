package com.yourssu.ssutime.v2.screen.my

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.yourssu.data.AlertData
import com.yourssu.data.UiState
import com.yourssu.ssutime.v2.accessToken
import com.yourssu.ssutime.v2.fcm.FcmDebugHistoryRepository
import com.yourssu.ssutime.v2.fcm.FcmDebugRecord
import com.yourssu.ssutime.v2.lms.getLmsLoginInfo
import com.yourssu.ssutime.v2.network.ApiRepository
import com.yourssu.ssutime.v2.screen.login.LoginRepository
import com.yourssu.ssutime.v2.screen.main.MainRepository
import io.github.chlwhdtn03.LmsApi
import io.github.chlwhdtn03.data.Lms.Info
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.yourssu.ssutime.v2.notification.scheduleDebugCallAlert as scheduleDebugCallAlertTest
import com.yourssu.ssutime.v2.notification.scheduleDebugNormalAlert as scheduleDebugNormalAlertTest

class MyViewModel(
    private val loginRepository: LoginRepository,
    private val mainRepository: MainRepository,
    private val apiRepository: ApiRepository,
    private val fcmDebugHistoryRepository: FcmDebugHistoryRepository,
) : ViewModel() {
    var loginInfo = mutableStateOf<Info?>(null)
    var isLogout = mutableStateOf(false)

    private val _uiState = MutableStateFlow<UiState<AlertData>>(UiState.Loading)
    val uiState: StateFlow<UiState<AlertData>> = _uiState.asStateFlow()
    private val _fcmToken = MutableStateFlow("FCM 토큰을 불러오는 중입니다.")
    val fcmToken: StateFlow<String> = _fcmToken.asStateFlow()
    val fcmDebugRecords: StateFlow<List<FcmDebugRecord>> = fcmDebugHistoryRepository.history
        .map { it.records }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    init {
        refreshFcmToken()

        viewModelScope.launch {
            _uiState.value = UiState.Success(
                mainRepository.getAlertData()
            )
            if(LmsApi.isLoggined)
                loginInfo.value = getLmsLoginInfo()
        }
    }

    fun refreshFcmToken() {
        _fcmToken.value = "FCM 토큰을 불러오는 중입니다."
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                _fcmToken.value = "FCM 토큰 발급 실패: ${task.exception?.localizedMessage ?: "알 수 없는 오류"}"
                return@addOnCompleteListener
            }

            _fcmToken.value = task.result.orEmpty().ifBlank { "FCM 토큰이 비어 있습니다." }
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

    fun scheduleDebugCallAlert(context: Context) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            scheduleDebugCallAlertTest(appContext) {
                mainRepository.getTodoData().todos
            }
        }
    }

    fun scheduleDebugNormalAlert(context: Context) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            scheduleDebugNormalAlertTest(appContext) {
                mainRepository.getTodoData().todos
            }
        }
    }

    fun clearFcmDebugHistory() {
        viewModelScope.launch {
            fcmDebugHistoryRepository.clear()
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
