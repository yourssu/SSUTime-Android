package com.yourssu.ssutime.v2.screen.my

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssu.data.AlertData
import com.yourssu.data.TodoInfo
import com.yourssu.data.UiState
import com.yourssu.ssutime.v2.accessToken
import com.yourssu.ssutime.v2.getRemainingDays
import com.yourssu.ssutime.v2.lms.getLmsLoginInfo
import com.yourssu.ssutime.v2.notification.showCallAlert
import com.yourssu.ssutime.v2.screen.login.LoginRepository
import com.yourssu.ssutime.v2.screen.main.MainRepository
import io.github.chlwhdtn03.LmsApi
import io.github.chlwhdtn03.data.Lms.Info
import kotlinx.coroutines.delay
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

    fun sendDebugCallAlertAfterDelay(context: Context) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            delay(10_000L)
            val todo = mainRepository.getTodoData().todos.selectDebugCallAlertTodo()
            if (todo == null) {
                Log.i(TAG, "디버그 전화 알림을 표시할 Todo가 없습니다.")
                return@launch
            }

            showCallAlert(appContext, todo)
        }
    }

    private fun List<TodoInfo>.selectDebugCallAlertTodo(): TodoInfo? =
        minWithOrNull(
            compareBy<TodoInfo> { todo ->
                runCatching { getRemainingDays(todo.due_date) }.getOrDefault(Long.MAX_VALUE)
            }.thenBy { todo -> todo.due_date },
        )

    private companion object {
        const val TAG = "MyViewModel"
    }
}
